# CAREamics Fiji Plugin
------------------------

### How to install
You need to copy the plugin **`jar`** into your Fiji Plugin directory.
- Windows / Linux: Open your main Fiji.app folder, and you will see a folder named plugins right inside.
- macOS: Right-click (or Control-click) on the Fiji application icon, choose Show Package Contents, and navigate to Contents/Plugins/ or look inside the extracted Fiji.app folder depending on how you installed it.

Also, check [here](https://imagej.net/imagej-wiki-static/Installing_3rd_party_plugins) for alternative ways.

>[!TIP]
> You can find the saved checkpoint of the trained model in `<your home dir>/careamics_logs`.


### Future Development
- **Train from Disk**: Allowing user to choose an image file (tif, zarr, etc.) from disk and train the model using that image.
- **Prediction plugin**: Can be used for predicting over images using a previously-trained model.
