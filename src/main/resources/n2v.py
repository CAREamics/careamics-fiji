from pathlib import Path
from typing import Any, Literal

import appose
import numpy as np
from appose import NDArray
from appose.python_worker import Task
from careamics.careamist import CAREamist
from careamics.config import create_advanced_n2v_config
from careamics.config.configuration import Configuration
from careamics_appose.callbacks import ApposeProgressBarCallback, update_callbacks
from careamics_appose.utils import numpy_to_shared_memory

SEED = 777


def log(msg: str, end="\n"):
    task: Task | None = globals().get("task")
    if task is not None:
        task.update(msg)
    else:
        print(f"{msg}", end=end)


def create_config(
    data_type: Literal["array", "tiff", "zarr", "czi", "custom"] = "array",
    axes: str = "YX",
    patch_size: list[int] = [64, 64],
    batch_size: int = 8,
    num_epochs: int = 1,
    num_steps: int = 100,
    augmentations: list = ["x_flip", "y_flip", "rotate_90"],
    n_val_patches: int = 15,
    in_memory: bool = True,
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
        augmentations=augmentations,
        n_val_patches=n_val_patches,
        in_memory=in_memory,
        normalization=normalization,
        num_workers=3,
        seed=seed,
    )
    return config


# ========================= main script =========================

# appose mode
appose_mode = "task" in globals()
task: Task | None = globals().get("task")

log("starting task")
log(f"appose mode: {appose_mode}")

# data
if appose_mode:
    # get input parameters from appose
    input_image: NDArray | None = globals().get("input_image")
    if input_image is not None:
        train_data = input_image.ndarray()
        log(f"input_image: {input_image.shape}")

    patch_size = globals().get("patch_size", [64, 64])
    batch_size = globals().get("batch_size", 8)
    num_epochs = globals().get("num_epochs", 1)
    num_steps = globals().get("num_steps", 100)
    log(
        f"patch_size: {patch_size}, batch_size: {batch_size}, num_epochs: {num_epochs}, num_steps: {num_steps}"
    )

else:
    train_data = np.random.rand(512, 512)

# config
config = create_config(
    data_type="array",
    axes="YX",
    patch_size=patch_size,
    batch_size=batch_size,
    num_epochs=num_epochs,
    num_steps=num_steps,
    augmentations=["x_flip", "y_flip", "rotate_90"],
    n_val_patches=15,
    in_memory=True,
    normalization="mean_std",
    seed=SEED,
)

# careamist
log("Initializing CAREamist...")
work_dir = Path("..")
careamist = CAREamist(config, work_dir=work_dir)
# update task callbacks if in appose mode
# so that the task can receive updates from the training process
if task is not None:
    update_callbacks(careamist, task)

# train
log("starting training...")
careamist.train(train_data=train_data)

# prediction
log("starting prediction...")
preds, _ = careamist.predict(
    pred_data=train_data,
    tile_size=(128, 128),
)

if task is not None:
    task.outputs["prediction"] = numpy_to_shared_memory(preds[0])
