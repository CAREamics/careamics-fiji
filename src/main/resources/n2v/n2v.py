from pathlib import Path
from typing import Literal

import appose
import numpy as np
from appose import NDArray
from appose.python_worker import Task
from careamics.careamist import CAREamist
from careamics.config import create_advanced_n2v_config
from careamics.config.configuration import Configuration

# from numpy.typing import NDArray

SEED = 777


def log(msg: str, end="\n"):
    task: Task | None = globals().get("task")
    if task is not None:
        task.update(msg)
    else:
        print(f"{msg}", end=end)


def to_shared_memory(img_arr: np.ndarray) -> NDArray:
    shared = NDArray(str(img_arr.dtype), list(img_arr.shape))
    shared.ndarray()[:] = img_arr
    return shared


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


# ==================== main script ====================

# override the print function
# to redirect print statements to task updates
print = log

# appose mode
appose_mode = "task" in globals()
task: Task | None = globals().get("task")

log("starting task")
log(f"appose mode: {appose_mode}")

# data
if appose_mode:
    # get input parameters from java appose
    input_image: NDArray | None = globals().get("input_image")
    if input_image is not None:
        train_data = input_image.ndarray()
        log(f"input_image: {input_image.shape}")

    num_epochs: int = globals().get("num_epochs", 1)

    log(f"epochs: {num_epochs}")

else:
    train_data = np.random.rand(512, 512)

# config
config = create_config(
    data_type="array",
    axes="YX",
    patch_size=[64, 64],
    batch_size=8,
    num_epochs=num_epochs,
    num_steps=100,
    augmentations=["x_flip", "y_flip", "rotate_90"],
    n_val_patches=15,
    in_memory=True,
    normalization="mean_std",
    seed=SEED,
)

# careamist
work_dir = Path("..")
careamist = CAREamist(config, work_dir=work_dir)

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
    task.outputs["prediction"] = to_shared_memory(preds[0])
