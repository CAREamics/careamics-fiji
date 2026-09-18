package org.careamics.fiji;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;

import net.imagej.ImageJ;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.NDArrays;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.Environment;
import org.apposed.appose.NDArray;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import org.careamics.fiji.gui.MainGUI;
import org.careamics.fiji.Config;


@Plugin(type = Command.class, menuPath = "Plugins>CAREamics>Noise2Void")
public class N2V extends DynamicCommand implements Initializable {

    @Parameter
    private LogService logger;

    protected MainGUI mainGUI;
    protected Task task;

    @Override
    public void initialize() {

    }

    public static void main(final String[] args)
	{
		// ImageJ.main(args);
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
		IJ.openImage("/Users/mehdi.seifi/Projects/CAREamics/tmp_data_src/data/SEM/val/val_small.tif").show();
        
        try (Context context = new Context()) {
            final N2V plugin = new N2V();
            context.inject(plugin);
            plugin.run();
        }
	}

    @Override
    public void run() {
        // get the current image
        ImagePlus imgp = WindowManager.getCurrentImage();
        if (imgp == null) {
            IJ.error("No image is available.");
            return;
        }

        // width, height, nChannels, nSlices, nFrames
        int[] dims = imgp.getDimensions();
        logger.info("Image dimensions: " + Arrays.toString(dims));
        int num_channels = dims[2];
        int num_slices = dims[3];
        int num_frames = dims[4];
        // get the iamge name and drop the file extension from the image name
        String img_name = imgp.getTitle();
        img_name = img_name.contains(".") ? img_name.substring(0, img_name.lastIndexOf('.')) : img_name;

        // show the main GUI
        this.mainGUI = new MainGUI(img_name, num_channels, num_slices, num_frames);
        GUI.center(this.mainGUI);
        
        mainGUI.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName() == MainGUI.CONFIGREADY) {
                Config config = (Config) evt.getNewValue();
                logger.info("Configuration is ready: " + config.toString());
                
                // process the image by running the python script
                this.task = processImage(imgp, config);

                if ( this.task.status != TaskStatus.COMPLETE )
                    throw new RuntimeException("Python script failed with error: " + task.error);
                
                final NDArray prediction = (NDArray) task.outputs.get("prediction");
                // // final Img<?> output = arrayToImage(prediction);
                // // ShmImg<FloatType> img = new ShmImg<>(prediction);
                ArrayImg<FloatType, ?> view = NDArrays.asArrayImg(prediction);
                // // NDArray copied = NDArrays.asNDArray(img);
                ImageJFunctions.show(view);
                // ImageJFunctions.wrap(prediction, "Prediction");
        
            }
        });

    }

    private Task processImage(final ImagePlus imgp, final Config config) {
        logger.info("creating the python uv environment...");
        final Environment env = createEnvironment();
        
        final String n2vScript = getN2VScript();
        
        final Map<String, Object> inputs = getInputs(imgp, config);
        
        try (Service python = env.python()) {
            final Task task = python.task(n2vScript, inputs);
            
            // listen for task updates
            task.listen(event -> {
                switch (event.responseType) {
                    case UPDATE:
                        String msg = event.message;
                        if (event.maximum > 0) {
                            msg += " (" + event.current + "/" + event.maximum + ")";
                            this.mainGUI.updateProgress(msg, event.current, event.maximum);
                        }
                        logger.info("[Python backend] " + msg);
                        break;
                
                    case COMPLETION:
                        logger.info("Task completed successfully.");
                        break;
                
                    case CANCELATION:
                        logger.warn("Task was cancelled.");
                        break;
                
                    case FAILURE:
                        logger.error("Task failed:\n" + event.task.error);
                        break;
                
                    default:
                        break;
                }
            });
            
            task.start();
            task.waitFor();
            return task;
            
        } catch (Exception e) {
            IJ.error(e.toString());
            return null;
        }
    }

    private Map<String, Object> getInputs(final ImagePlus imgp, final Config config) {
        final Map<String, Object> inputs = new HashMap<>();
        inputs.put("input_image", imageToAppose(imgp));
        inputs.put("patch_size", config.patchSize);
        inputs.put("batch_size", config.batchSize);
        inputs.put("num_epochs", config.numEpochs);
        inputs.put("num_steps", config.numSteps);

        return inputs;
    }

    private <T extends RealType<T> & NativeType<T>> NDArray imageToAppose(final ImagePlus imgp) {
        /*
         * Copy the image into a shared memory image and wrap it into an
         * NDArray
         */
        @SuppressWarnings("unchecked")
        final ImgPlus<T> img = rawWraps(imgp);
        return NDArrays.asNDArray(img);
    }

    @SuppressWarnings("rawtypes")
    public static final ImgPlus rawWraps(final ImagePlus imgp) {
        /*
        * A utility to wrap an ImagePlus into an ImgPlus, without too many
        * warnings. Hacky.
        */
        final ImgPlus<DoubleType> img = ImagePlusAdapter.wrapImgPlus(imgp);
        final ImgPlus raw = img;
        return raw;
    }

    private Environment createEnvironment() {
        try {
            final Environment env = Appose.uv()
                .python("3.11")
                .include("appose>=0.12.0", "careamics==0.3.3")
                .include("git+https://github.com/CAREamics/careamics-appose.git")
                .name("careamics_env")
                .logDebug()
                // .subscribeProgress((msg, curr, max) -> {IJ.log(msg);})
				// .subscribeOutput((msg) -> {IJ.log(msg);})
				// .subscribeError(IJ::error)
                .build();

            return env;
        } catch (BuildException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getN2VScript() {
        String n2vScript = null;
        try (InputStream instream = this.getClass().getClassLoader().getResourceAsStream("n2v.py")) {
            n2vScript = new String(instream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return n2vScript;
    }

}
