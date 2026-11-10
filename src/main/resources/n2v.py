from functools import partial
from pathlib import Path
from typing import Literal

import appose
import numpy as np
import torch
from appose import NDArray
from appose.python_worker import Task
from careamics.careamist import CAREamist
from careamics.config import create_advanced_n2v_config
from careamics.config.configuration import Configuration
from careamics.dataset import ImageRegionData
from careamics.dataset.dataset import CareamicsDataset
from careamics.lightning.callbacks import (
    PredictionStoppedException,
    StopPredictionCallback,
)
from careamics.model_io.bioimage.cover_factory import create_cover
from careamics.utils.reshape_array import reshape_array
from careamics_appose import (
    # TaskTQDM,
    numpy_to_shared_memory,
    update_careamist_callbacks,
)
from lightning.pytorch.callbacks import ModelCheckpoint

SEED = 777
WORK_DIR = Path.home() / "careamics_logs"


def log(msg: str, is_error: bool = False, end="\n"):
    if is_error:
        msg = f"ERROR: {msg}"

    task: Task | None = globals().get("task")
    if task is not None:
        task.update(msg)
    else:
        print(f"{msg}", end=end)


def cancel_requested() -> bool:
    task: Task | None = globals().get("task")
    return task is not None and task.cancel_requested


def transpose_axes(axes: str) -> str:
    _axes = "YX"
    if "C" in axes:
        _axes = "C" + _axes

    if "Z" in axes:
        _axes = "Z" + _axes

    if "T" in axes:
        _axes = "T" + _axes

    return _axes


def get_data_type(data_source: str) -> Literal["tiff", "zarr", "czi", "custom"]:
    suffix = Path(data_source).suffix.lower()
    if suffix in [".tif", ".tiff"]:
        return "tiff"
    if suffix == ".zarr":
        return "zarr"
    if suffix == ".czi":
        return "czi"
    return "custom"


def get_prediction_dir(pred_data: str) -> Path:
    out_dir = Path(pred_data).parent / "predictions"
    count = 0
    while out_dir.exists():
        count += 1
        out_dir = Path(pred_data).parent / f"predictions_{count}"

    return out_dir


def create_config(
    data_type: Literal["array", "tiff", "zarr", "czi", "custom"] = "array",
    axes: str = "YX",
    patch_size: list[int] = [64, 64],
    batch_size: int = 8,
    num_epochs: int = 1,
    num_steps: int = 100,
    num_channels: int | None = None,
    augmentations: list = ["x_flip", "y_flip", "rotate_90"],
    n_val_patches: int = 15,
    in_memory: bool | None = None,
    normalization: Literal["mean_std", "min_max", "quantile", "none"] = "mean_std",
    seed: int = SEED,
) -> Configuration:
    # for creating n2v config from given config parameters
    config = create_advanced_n2v_config(
        experiment_name="n2v_appose",
        data_type=data_type,
        axes=axes,
        patch_size=patch_size,
        batch_size=batch_size,
        num_epochs=num_epochs,
        num_steps=num_steps,
        n_channels=num_channels,
        augmentations=augmentations,
        n_val_patches=n_val_patches,
        in_memory=in_memory,
        normalization=normalization,
        num_workers=3,
        seed=seed,
    )
    return config


def get_losses(careamist: "CAREamist") -> dict:
    report = careamist.get_losses()
    return {
        "epoch": report.train_loss.epoch,
        "train": report.train_loss.value,
        "val": report.val_loss.value,
    }


def get_sample_input_output(careamist: CAREamist) -> tuple:
    log("Getting sample input and output...")
    dataset: CareamicsDataset = careamist.trainer.datamodule.val_dataset
    img_region: ImageRegionData = dataset[0][0]
    sample_input = dataset.normalization.denormalize(
        torch.from_numpy(img_region.data)
    ).numpy()
    # make sure the input array has the correct number of dims according to the axes
    dim_diff = sample_input.ndim - len(careamist.config.data_config.axes)
    if dim_diff > 0:
        sample_input = np.squeeze(sample_input, axis=tuple(range(dim_diff)))

    # get the prediction as sample output
    sample_output, _ = careamist.predict(
        sample_input,
        data_type="array",
        in_memory=True,
        batch_size=1,
    )

    output_array = reshape_array(
        np.concatenate(sample_output, axis=0), careamist.config.data_config.axes
    )
    input_array = reshape_array(sample_input, careamist.config.data_config.axes)

    return input_array, output_array


def create_cover_image(input_array: np.ndarray, output_array: np.ndarray) -> str:
    create_cover(WORK_DIR, input_array, output_array)
    cover_image_path = WORK_DIR / "cover.png"

    return str(cover_image_path)


def get_model_specs(careamist: CAREamist) -> dict | None:
    name = careamist.config.get_algorithm_friendly_name()
    description = careamist.config.get_algorithm_description()
    citation = careamist.config.get_algorithm_citations()
    references = careamist.config.get_algorithm_references()

    return {
        "model_name": name,
        "general_description": description,
        "citation": citation,
        "references": references,
    }


def train(task: Task | None = None):
    # get input parameters from appose
    axes = transpose_axes(globals().get("axes", "YX"))
    patch_size = globals().get("patch_size", [64, 64])
    batch_size = globals().get("batch_size", 8)
    num_epochs = globals().get("num_epochs", 1)
    num_steps = globals().get("num_steps", 100)

    input_image: NDArray | None = globals().get("input_image")
    if input_image is not None:
        train_data = input_image.ndarray()
        data_type = "array"
        num_channels = globals().get("num_channels", 1)
        log(f"input_image: {input_image.shape}")
    else:
        train_data = globals().get("train_source")
        data_type = get_data_type(train_data)
        num_channels = None
        log(f"train_source: {train_data}")

    if "C" not in axes:
        num_channels = None
    elif num_channels is None:
        # drop C from axes
        axes = axes.replace("C", "")
        # raise ValueError("Number of channels must be specified if 'C' is in axes.")

    log(
        f"axes: {axes}, num_channels: {num_channels}, patch_size: {patch_size}, "
        f"batch_size: {batch_size}, num_epochs: {num_epochs}, num_steps: {num_steps}"
    )

    # config
    config = create_config(
        data_type=data_type,
        axes=axes,
        patch_size=patch_size,
        batch_size=batch_size,
        num_epochs=num_epochs,
        num_steps=num_steps,
        num_channels=num_channels,
        augmentations=["x_flip", "y_flip", "rotate_90"],
        n_val_patches=15,
        normalization="mean_std",
        seed=SEED,
    )

    if not cancel_requested():
        log("Initializing CAREamist...")
        careamist = CAREamist(
            config,
            work_dir=WORK_DIR,
        )

        # update task callbacks if in appose mode
        # so that the task can receive updates from the training process
        if task is not None:
            update_careamist_callbacks(careamist, task)

    # training
    if not cancel_requested():
        log("Starting Training...")
        careamist.train(train_data=train_data)

    if not cancel_requested():
        losses = get_losses(careamist)
        task.outputs["losses"] = losses

        # get model checkpoint path
        for cb in careamist.callbacks:
            if isinstance(cb, ModelCheckpoint):
                checkpoint_path = cb.best_model_path
                if checkpoint_path is None or len(checkpoint_path) == 0:
                    checkpoint_path = cb.last_model_path
                task.outputs["checkpoint_path"] = checkpoint_path
                log(f"model checkpoint: {checkpoint_path}")

        # sample input / output and cover image for bmz export
        input_array, output_array = get_sample_input_output(careamist)
        np.save(WORK_DIR / "sample_input.npy", input_array)
        np.save(WORK_DIR / "sample_output.npy", output_array)
        task.outputs["sample_input"] = str(WORK_DIR / "sample_input.npy")
        task.outputs["sample_output"] = str(WORK_DIR / "sample_output.npy")
        task.outputs["cover_image"] = create_cover_image(input_array, output_array)

        # get model specs for bmz export
        task.outputs["model_specs"] = get_model_specs(careamist)
    else:
        task.cancel()


def predict(task: Task | None = None):
    # checkpoint path
    checkpoint_path: str | None = globals().get("checkpoint_path")
    if checkpoint_path is None:
        log("No model checkpoint was provided.", is_error=True)
        return

    # pred data
    from_disk = globals().get("from_disk")
    pred_data: NDArray | str | None = globals().get("pred_source")
    if pred_data is None:
        log("No prediction data was provided.", is_error=True)
        return
    if from_disk:
        data_type = get_data_type(pred_data)
        in_memory = False
        log(f"pred_source: {pred_data}")
    else:
        pred_data = pred_data.ndarray()
        data_type = "array"
        in_memory = True
        log(f"pred_source: {pred_data.shape}")

    batch_size = globals().get("batch_size")
    xy_tile_size = globals().get("xy_tile_size")
    z_tile_size = globals().get("z_tile_size")

    tile_size = (xy_tile_size, xy_tile_size)
    tile_overlap = (16, 16)
    if z_tile_size is not None:
        tile_size = (z_tile_size, xy_tile_size, xy_tile_size)
        tile_overlap = (2, 16, 16)

    # init CAREamist from the checkpoint
    if not cancel_requested():
        try:
            careamist = CAREamist(checkpoint_path=checkpoint_path, work_dir=WORK_DIR)
        except Exception as e:
            log(f"Failed to initialize CAREamist from checkpoint: {e}", is_error=True)
            return
    else:
        task.cancel()
        return
    # update task callbacks if in appose mode
    # so that the task can receive updates from the prediction process
    if task is not None:
        update_careamist_callbacks(careamist, task)
        pred_stop_callback = StopPredictionCallback(cancel_requested)
        careamist.callbacks.append(pred_stop_callback)
        careamist.trainer.callbacks.append(pred_stop_callback)

    # predict to disk if the source is from disk
    pred_dir = None
    if from_disk:
        pred_dir = get_prediction_dir(pred_data)
        pred_func = partial(
            careamist.predict_to_disk,
            prediction_dir=pred_dir,
            write_type="zarr" if data_type == "zarr" else "tiff",
        )
    else:
        pred_func = careamist.predict

    # prediction
    if not cancel_requested():
        log("Starting Prediction...")
        try:
            output = pred_func(
                pred_data=pred_data,
                tile_size=tile_size,
                tile_overlap=tile_overlap,
                batch_size=batch_size,
                data_type=data_type,
                in_memory=in_memory,
            )

            if task is not None:
                if from_disk:
                    task.outputs["prediction"] = str(pred_dir)
                else:
                    preds = output[0][0]
                    task.outputs["prediction"] = numpy_to_shared_memory(preds)

        except PredictionStoppedException:
            task.cancel()
            log("Prediction was stopped.")
        except Exception as e:
            log(f"An error occurred during prediction: {e}", is_error=True)
    else:
        task.cancel()


# ========================= main script =========================

# appose mode
appose_mode = "task" in globals()
task: Task | None = globals().get("task")

if task is not None:
    if globals().get("training", False):
        train(task=task)
    else:
        predict(task=task)
