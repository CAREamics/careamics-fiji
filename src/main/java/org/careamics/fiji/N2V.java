package org.careamics.fiji;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.beans.PropertyChangeListener;
import java.awt.Color;
import java.beans.PropertyChangeEvent;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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
import org.careamics.fiji.gui.LossPlot;
import org.careamics.fiji.Config;


@Plugin(type = Command.class, menuPath = "Plugins>CAREamics>Noise2Void")
public class N2V extends DynamicCommand implements Initializable {

    @Parameter
    private LogService logger;

    protected MainGUI mainGUI;
    protected Environment apposeEnv;
    protected Task apposeTask;
    protected SwingWorker<Task, Task> taskWorker;

    @Override
    public void initialize() {

    }

    public static void main(final String[] args)
	{
		// ImageJ.main(args);
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
		IJ.openImage("/Users/mehdi.seifi/Projects/CAREamics/tmp_data_src/data/SEM/val/val.tif").show();
        // IJ.openImage("/Users/mehdi.seifi/Projects/CAREamics/tmp_data_src/data/rgb_stack.tif").show();
        
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
        img_name = img_name.contains(".") ? img_name.substring(0, img_name.lastIndexOf(".")) : img_name;
        
        // show the main GUI
        this.mainGUI = new MainGUI(img_name, num_channels, num_slices, num_frames);
        GUI.center(this.mainGUI);
        
        mainGUI.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName() == MainGUI.CONFIGREADY) {
                Config config = (Config) evt.getNewValue();
                logger.info("Configuration: " + config.toString());
            
                // run the image processing task in the background
                this.taskWorker = createTaskWorker(imgp, config);
                this.taskWorker.execute();
            
            } else if (evt.getPropertyName() == MainGUI.CANCELREQUESTED) {
                logger.info("Task cancellation requested.");
                if (apposeTask != null) {
                    logger.info(apposeTask);
                    apposeTask.cancel();
                }
            }
        });
        
    }

    private SwingWorker<Task, Task> createTaskWorker(final ImagePlus imgp, final Config config) {
        return new SwingWorker<>() {
            @Override
            protected Task doInBackground() throws Exception {
                apposeTask = processImage(imgp, config);

                return apposeTask;
            }
        
            @Override
            protected void done() {
                try {
                    apposeTask = get();
                    if (apposeTask == null) {
                        logger.error("Python script failed: apposeTask is null");
                        return;
                    }
                    if (apposeTask.status == TaskStatus.FAILED) {
                        throw new RuntimeException("Python script failed with error: " + apposeTask.error);
                    }
                    
                    if (apposeTask.status == TaskStatus.COMPLETE && apposeTask.outputs.containsKey("prediction")) {
                        final Map<String, ArrayList<Float>> losses = (Map<String, ArrayList<Float>>) apposeTask.outputs.get("losses");
                        // logger.info("Losses: " + losses.toString());
                        LossPlot lossPlot = new LossPlot(losses);
                        
                        final NDArray prediction = (NDArray) apposeTask.outputs.get("prediction");
                        ArrayImg<FloatType, ?> view = NDArrays.asArrayImg(prediction);
                        // ImageJFunctions.show(view);
                        ImagePlus wrapped = ImageJFunctions.wrap(view, "prediction");
                        ImageJFunctions.show(ImageJFunctions.convertFloat(wrapped));
                    }
                
                } catch (Exception e) {
                    logger.error("Error executing task", e);
                } finally {
                    mainGUI.reset();
                }
            }
        };
    }

    private Task processImage(final ImagePlus imgp, final Config config) {
        logger.info("creating the python uv environment...");
        this.apposeEnv = createEnvironment();
        final Environment env = this.apposeEnv;
        
        final String n2vScript = getN2VScript();
        
        final Map<String, Object> inputs = config.toDictionary();
        inputs.put("input_image", imageToAppose(imgp));
        
        try (Service python = env.python()) {
            apposeTask = python.task(n2vScript, inputs);
            
            // listen for task updates
            apposeTask.listen(event -> {
                switch (event.responseType) {
                    case UPDATE:
                        String msg = event.message;
                        if (event.maximum > 0) {
                            msg += " (" + event.current + "/" + event.maximum + ")";
                            SwingUtilities.invokeLater(() -> {
                                this.mainGUI.updateProgress(event.message, event.current, event.maximum);
                            });
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
            
            apposeTask.start();
            apposeTask.waitFor();
            return apposeTask;
            
        } catch (Exception e) {
            logger.error(e.toString());
            return null;
        }
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
            String home = System.getProperty("user.home");
            String env_dir = Paths.get(home, "appose_careamics_env").toString();
            
            final Environment env = Appose.uv()
                .python("3.11")
                .include("appose>=0.12.0", "careamics==0.3.3")
                .include("git+https://github.com/CAREamics/careamics-appose.git")
                .name("appose_careamics_env")
                .base(env_dir)
                .logDebug()
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
