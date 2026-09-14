from pathlib import Path
from typing import Any, Literal

import appose
import numpy as np
from appose import NDArray
from appose.python_worker import Task
from careamics.careamist import CAREamist
from careamics.config import create_advanced_n2v_config
from careamics.config.configuration import Configuration
from careamics.lightning.callbacks import ProgressBarCallback
from lightning.pytorch import LightningModule, Trainer
from lightning.pytorch.callbacks import ProgressBar

# from numpy.typing import NDArray


SEED = 777


class ApposeProgressBarCallback(ProgressBar):
    def __init__(self, task: Task):
        super().__init__()
        self.task = task
        self.num_epochs = 0
        self.curr_epoch = 0

    def on_fit_start(self, trainer: Trainer, pl_module: LightningModule):
        super().on_fit_start(trainer, pl_module)
        self.task.update("on_fit_start")
        self.num_epochs = trainer.max_epochs
        self.curr_epoch = trainer.current_epoch

    def on_train_batch_start(
        self, trainer: Trainer, pl_module: LightningModule, batch, batch_idx
    ):
        super().on_train_batch_start(trainer, pl_module, batch, batch_idx)
        self.task.update("on_train_batch_start")
        self.task.update(
            f"Training Epoch {self.curr_epoch + 1}/{self.num_epochs}",
            current=batch_idx,
            maximum=int(self.total_train_batches),
        )

    def on_validation_batch_start(
        self,
        trainer: Trainer,
        pl_module: LightningModule,
        batch: Any,
        batch_idx: int,
        dataloader_idx: int = 0,
    ):
        super().on_validation_batch_start(
            trainer, pl_module, batch, batch_idx, dataloader_idx
        )
        self.task.update("on_validation_batch_start")
        self.task.update(
            "Validation:", current=batch_idx, maximum=int(self.total_val_batches)
        )

    def on_fit_end(self, trainer: Trainer, pl_module: LightningModule):
        super().on_fit_end(trainer, pl_module)
        self.task.update(
            "Training finished", current=self.num_epochs, maximum=self.num_epochs
        )


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


def update_callbacks(careamist: CAREamist, task: Task):
    progress_callback = ApposeProgressBarCallback(task)
    callbacks = [
        cb for cb in careamist.callbacks if not isinstance(cb, ProgressBarCallback)
    ]
    callbacks.append(progress_callback)
    careamist.callbacks = callbacks
    careamist.trainer.callbacks = [careamist.prediction_writer, *callbacks]


# ========================= main script =========================

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
log("Initializing CAREamist...")
work_dir = Path("..")
careamist = CAREamist(config, work_dir=work_dir)
if task is not None:
    # careamist.callbacks = get_callbacks(careamist, task)
    update_callbacks(careamist, task)

# log(f"CAREamist callbacks: {len(careamist.callbacks)}")
log(f"Callbacks initialized\n{[type(cb).__name__ for cb in careamist.callbacks]}")

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
