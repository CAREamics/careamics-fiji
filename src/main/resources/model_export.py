from pathlib import Path

import numpy as np
from appose.python_worker import Task
from careamics.careamist import CAREamist
from careamics.model_io import export_to_bmz

CHECKPOINT_PATH = "checkpoint_path"
EXPORT_PATH = "export_path"
MODEL_NAME = "model_name"
GENERAL_DESCRIPTION = "general_description"
DATA_DESCRIPTION = "data_description"
AUTHORS = "authors"
COVER_IMAGE = "cover_image"
CITATION = "citation"
REFERENCES = "references"
SAMPLE_INPUT = "sample_input"
SAMPLE_OUTPUT = "sample_output"


def log(msg: str, is_error: bool = False, end="\n"):
    if is_error:
        msg = f"ERROR: {msg}"

    task: Task | None = globals().get("task")
    if task is not None:
        task.update(msg)
    else:
        print(f"{msg}", end=end)


def load_model() -> CAREamist | None:
    # checkpoint path
    checkpoint_path: str | None = globals().get("checkpoint_path")
    if checkpoint_path is None:
        log("No model was provided.")
        return None

    # init CAREamist from the checkpoint
    try:
        careamist = CAREamist(
            checkpoint_path=checkpoint_path,
            work_dir=Path.home() / "careamics_logs",
        )
        return careamist

    except Exception as e:
        log(f"Failed to initialize CAREamist from checkpoint: {e}", is_error=True)
        return


def export_model(export_type: str) -> bool:
    # checkpoint path
    checkpoint_path: str | Path | None = globals().get("checkpoint_path")
    if checkpoint_path is None:
        log("No model was provided.")
        return False
    checkpoint_path = Path(checkpoint_path)

    # export path
    export_path: str | Path | None = globals().get("export_path")
    if export_path is None:
        log("No export path was provided.")
        return False
    export_path = Path(export_path)

    # init CAREamist from the checkpoint
    try:
        careamist = CAREamist(
            checkpoint_path=checkpoint_path,
            work_dir=Path.home() / "careamics_logs",
        )
    except Exception as e:
        log(f"Failed to initialize CAREamist from checkpoint: {e}", is_error=True)
        return False

    if export_type == "checkpoint":
        # export as PyTorch Lightning checkpoint
        log("Exporting as PyTorch Lightning checkpoint...")
        careamist.trainer.save_checkpoint(export_path)
        log(f"Model checkpoint saved to {export_path}.")
        return True

    elif export_type == "bmz":
        # export as Bioimage.io model
        log("Exporting as Bioimage.io model...")
        try:
            # load sample input / output
            input_array = np.load(globals().get(SAMPLE_INPUT))
            output_array = np.load(globals().get(SAMPLE_OUTPUT))
            authors = globals().get(AUTHORS)
            log(f"authors: {type(authors)}, {type(authors[0])}")
            log(authors)
            # export
            export_to_bmz(
                model=careamist.model,
                config=careamist.config,
                path_to_archive=export_path,
                model_name=globals().get(MODEL_NAME),
                general_description=globals().get(GENERAL_DESCRIPTION),
                data_description=globals().get(DATA_DESCRIPTION),
                authors=globals().get(AUTHORS),
                covers=[globals().get(COVER_IMAGE)],
                input_array=input_array,
                output_array=output_array,
            )
            log(f"Model exported to Bioimage.io format at {export_path}.")
        except Exception as e:
            log(f"Failed to export model to Bioimage.io format: {e}", is_error=True)
            return False

        return True


# ========================= main script =========================

# appose mode
appose_mode = "task" in globals()
task: Task | None = globals().get("task")

if task is not None:
    export_type = globals().get("export_type")
    if export_type is not None:
        result = export_model(export_type)
        task.outputs["exported"] = result
