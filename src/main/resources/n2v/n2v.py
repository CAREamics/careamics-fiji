from pathlib import Path

import numpy as np
from careamics.careamist import CAREamist
from careamics.config import create_advanced_n2v_config
from careamics.config.configuration import Configuration
from numpy.typing import NDArray

SEED = 777


def create_config() -> Configuration:
    # for creating n2v config from given params
    pass


def main() -> None:
    # data
    train_data = Path(
        "/Users/mehdi.seifi/Projects/CAREamics/tmp_data_src/data/SEM/train/train.tif"
    )
    val_data = Path(
        "/Users/mehdi.seifi/Projects/CAREamics/tmp_data_src/data/SEM/val/val.tif"
    )

    # config
    config = create_advanced_n2v_config(
        experiment_name="n2v_appose_testing",
        data_type="tiff",
        axes="YX",
        patch_size=[64, 64],
        batch_size=8,
        num_epochs=1,
        num_steps=100,
        augmentations=["x_flip", "y_flip", "rotate_90"],
        n_val_patches=15,
        in_memory=True,
        normalization="mean_std",
        num_workers=3,
        seed=SEED,
    )

    # careamist
    work_dir = Path("..")
    careamist = CAREamist(config, work_dir=work_dir)

    # train
    careamist.train(train_data=train_data, val_data=val_data)


if __name__ == "__main__":
    main()
