package org.careamics.fiji;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;


import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.appose.NDArrays;
import net.imglib2.img.ImagePlusAdapter;
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
import org.apposed.appose.TaskEvent;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.console.LoggingPanel;
import org.careamics.fiji.gui.MainGUI;
import org.careamics.fiji.gui.LossPlot;


@Plugin(type = Command.class, menuPath = "Plugins>CAREamics>Noise2Void")
public class N2V extends DynamicCommand implements Initializable {

    @Parameter
    private LogService logger;

    protected MainGUI mainGUI;
    protected Environment apposeEnv;
    protected Task apposeTask;
    protected SwingWorker<Task, Task> taskWorker;
    protected String trainedModelPath;
    protected String sampleInputPath;
    protected String sampleOutputPath;
    protected String coverImagePath;
    protected HashMap<String, Object> modelSpecs;

    @Override
    public void initialize() {
    
    }

    public static void main(final String[] args)
	{
		// ImageJ.main(args);
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
		IJ.openImage("/Users/mehdi.seifi/tmp_data/val.tif").show();
        IJ.openImage("/Users/mehdi.seifi/tmp_data/rgb_stack.tif").show();
        
        try (Context context = new Context()) {
            final N2V plugin = new N2V();
            context.inject(plugin);
            plugin.run();
        }
	}
    
    @Override
    public void run() {
        // logging panel
        final org.scijava.Context currentContext = getContext();
        SwingUtilities.invokeLater(() -> {
            // instantiate the native SciJava logging panel
            LoggingPanel loggingPanel = new LoggingPanel(currentContext);
            logger.addLogListener(loggingPanel);
            
            // wrap it inside an JFrame
            JFrame frame = new JFrame("CAREamics Log Monitor");
            frame.setSize(750, 450);
            frame.setLayout(new BorderLayout());
            frame.add(loggingPanel, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setVisible(true);
        });
        
        // init the main GUI
        this.mainGUI = new MainGUI(logger);
        GUI.center(this.mainGUI);
        
        mainGUI.addPropertyChangeListener(evt -> {
            switch (evt.getPropertyName()) {
                case MainGUI.START_TRAINING:
                    Config config = (Config) evt.getNewValue();
                    logger.info("Configuration: " + config.toString());
                    // run the image processing task in the background
                    this.taskWorker = createTrainWorker(config);
                    this.taskWorker.execute();
                    break;
                
                case MainGUI.CANCEL_REQUESTED:
                    logger.info("Task cancellation requested.");
                    if (apposeTask != null) {
                        apposeTask.cancel();
                    }
                    break;
                
                case MainGUI.START_PREDICTION:
                    HashMap<String, Object> predConfig = (HashMap<String, Object>) evt.getNewValue();
                    logger.info("Prediction Configuration: " + predConfig.toString());
                    // run the prediction task in the background
                    this.taskWorker = createPredictionWorker(predConfig);
                    this.taskWorker.execute();
                    break;
                
                case MainGUI.EXPORT_MODEL:
                    HashMap<String, Object> exportData = (HashMap<String, Object>) evt.getNewValue();
                    logger.info("Export model requested: " + exportData);
                    exportModel(exportData);
                    break;
                
                default:
                    break;
            }
        
        });
        
    }

    private SwingWorker<Task, Task> createTrainWorker(final Config config) {
        return new SwingWorker<>() {
            @Override
            protected Task doInBackground() throws Exception {
                apposeTask = trainProcess(config);
            
                return apposeTask;
            }
            
            @Override
            protected void done() {
                try {
                    apposeTask = get();
                    if (apposeTask == null) {
                        // logger.error("Task is null");
                        return;
                    }
                    if (apposeTask.status == TaskStatus.FAILED) {
                        throw new RuntimeException("Python script failed with error: " + apposeTask.error);
                    }
                    
                    if (apposeTask.status == TaskStatus.COMPLETE) {
                        // handle graceful cancellation
                        if (apposeTask.outputs == null || apposeTask.outputs.get("checkpoint_path") == null) {
                            logger.warn("Training was cancelled!");
                            return;
                        }
                        // get the checkpoint path
                        trainedModelPath = (String) apposeTask.outputs.get("checkpoint_path");
                        // get sample input / output
                        sampleInputPath = (String) apposeTask.outputs.get("sample_input");
                        sampleOutputPath = (String) apposeTask.outputs.get("sample_output");
                        // get cover image path
                        coverImagePath = (String) apposeTask.outputs.get("cover_image");
                        // get model specs
                        modelSpecs = (HashMap<String, Object>) apposeTask.outputs.get("model_specs");
                        // pass model specs to main GUI
                        mainGUI.setModelBMZSpecs(modelSpecs, coverImagePath);
                        // plot losses
                        final Map<String, ArrayList<Float>> losses = (Map<String, ArrayList<Float>>) apposeTask.outputs.get("losses");
                        // show the loss plot
                        LossPlot lossPlot = new LossPlot(losses);
                        lossPlot.setVisible(true);

                        // enable the export in the mainGUI
                        mainGUI.enableExport();
                    }
                
                } catch (Exception e) {
                    logger.error("Error executing task: ", e);
                } finally {
                    mainGUI.reset();
                }
            }
        };
    }

    private Task trainProcess(final Config config) {
        final Map<String, Object> inputs = config.toDictionary();
        
        // make sure there is an image for training
        if (config.trainSource == null) {
            logger.error("No image or training file provided.");
            return null;
        }
        if (config.fromDisk == false) {
            // get the image from the window manager
            final ImagePlus imgp = WindowManager.getImage(config.trainSource);
            if (imgp == null) {
                logger.error("Image was not found: " + config.trainSource);
                return null;
            } else {
                inputs.put("input_image", imageToAppose(imgp));
            }
        }
        
        // python environment
        if (this.apposeEnv == null) {
            this.apposeEnv = createEnvironment();
        }
        // get the python script
        final String n2vScript = getN2VScript();
        
        // train mode
        inputs.put("training", true);
        
        try (Service python = this.apposeEnv.python()) {
            apposeTask = python.task(n2vScript, inputs);
            
            // listen for task updates
            apposeTask.listen(event -> {
                taskEventListener(event, "training");
            });
            
            apposeTask.start();
            apposeTask.waitFor();
            return apposeTask;
            
        } catch (Exception e) {
            logger.error(e.toString());
            return null;
        }
    }

    private SwingWorker<Task, Task> createPredictionWorker(final HashMap<String, Object> predConfig) {
        return new SwingWorker<>() {
            @Override
            protected Task doInBackground() throws Exception {
                // run the prediction task
                apposeTask = predictionProcess(predConfig);
                return apposeTask;
            }

            @Override
            protected void done() {
                try {
                    apposeTask = get();
                    if (apposeTask == null) {
                        logger.error("Task is null");
                        return;
                    }
                    if (apposeTask.status == TaskStatus.FAILED) {
                        throw new RuntimeException("Python script failed with error: " + apposeTask.error);
                    }
                    
                    if (apposeTask.status == TaskStatus.COMPLETE) {
                        // handle graceful cancellation
                        if (apposeTask.outputs == null || apposeTask.outputs.get("prediction") == null) {
                            logger.info("Prediction was cancelled!");
                            return;
                        }
                        Object pred_out = apposeTask.outputs.get("prediction");
                        if (pred_out instanceof String) {
                            String predictionPath = (String) pred_out;
                            logger.info("Prediction was successfully saved at: " + predictionPath);
                        } else {
                            final NDArray prediction = (NDArray) pred_out;
                            ArrayImg<FloatType, ?> view = NDArrays.asArrayImg(prediction);
                            // ImageJFunctions.show(view);
                            ImagePlus wrapped = ImageJFunctions.wrap(view, "prediction");
                            ImageJFunctions.show(ImageJFunctions.convertFloat(wrapped));
                        }
                    }
                
                } catch (Exception e) {
                    logger.error("Error executing task: ", e);
                } finally {
                    mainGUI.reset();
                }
            }
        };
    }

    private Task predictionProcess(final HashMap<String, Object> config) {
        // model checkpoint path
        if (config.get("checkpoint_path") == null) {
            // set it to the trained model checkpoint path if available
            if (trainedModelPath != null) {
                config.put("checkpoint_path", trainedModelPath);
            } else {
                logger.error("No model checkpoint provided.");
                return null;
            }
        }

        // prediction source image
        String predSource = (String) config.get("pred_source");
        if (predSource == null) {
            logger.error("No image or training file provided.");
            return null;
        }
        if (config.get("from_disk") == Boolean.FALSE) {
            // get the image from the window manager
            final ImagePlus imgp = WindowManager.getImage(predSource);
            if (imgp == null) {
                logger.error("Image was not found: " + predSource);
                return null;
            } else {
                config.put("pred_source", imageToAppose(imgp));
            }
        }
        
        // python environment
        if (this.apposeEnv == null) {
            this.apposeEnv = createEnvironment();
        }
        // get the python script
        final String n2vScript = getN2VScript();
        
        // prediction mode
        config.put("training", false);
        
        try (Service python = this.apposeEnv.python()) {
            apposeTask = python.task(n2vScript, config);
            
            // listen for task updates
            apposeTask.listen(event -> {
                taskEventListener(event, "prediction");
            });
            
            apposeTask.start();
            apposeTask.waitFor();
            return apposeTask;
            
        } catch (Exception e) {
            logger.error(e.toString());
            return null;
        }
    }

    private void exportModel(HashMap<String, Object> exportData) {
        if (this.trainedModelPath != null) {
            exportData.put("checkpoint_path", this.trainedModelPath);
        } else {
            logger.error("No trained model was provided.");
            return;
        }
        
        if (exportData.get("export_type") == MainGUI.EXPORT_CHECKPOINT) {
            String exportPath = (String) exportData.get("export_path");
            if (exportPath != null) {
                // copy the file at trainedModelPath to the export path
                try {
                    Files.copy(
                        Paths.get(this.trainedModelPath),
                        Paths.get(exportPath),
                        StandardCopyOption.REPLACE_EXISTING
                );
                } catch (IOException e) {
                    logger.error("Failed to copy trained model checkpoint to export path: " + e.toString());
                    return;
                }
            } else {
                logger.error("No export path was provided for checkpoint export.");
                return;
            }
        
        } else {
            exportToBMZ(exportData);
        }

    }

    private void exportToBMZ(HashMap<String, Object> exportData) {
        // add model specs to export data
        exportData.putAll(this.modelSpecs);
        // add sample input and output paths
        exportData.put("sample_input", this.sampleInputPath);
        exportData.put("sample_output", this.sampleOutputPath);
        // add cover image path
        if (this.coverImagePath != null) {
            exportData.put("cover_image", this.coverImagePath);
        } else {
            logger.warn("No cover image was provided.");
        }
        
        // python environment
        if (this.apposeEnv == null) {
            this.apposeEnv = createEnvironment();
        }
        
        // get the export script
        final String exportScript = getExportScript();
        
        // export the model
        try (Service python = this.apposeEnv.python()) {
            apposeTask = python.task(exportScript, exportData);
            apposeTask.listen(event -> taskEventListener(event, "export"));
            apposeTask.start();
            apposeTask.waitFor();
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    private Task taskEventListener(TaskEvent event, String stage) {
        switch (event.responseType) {
            case UPDATE:
                String msg = event.message;
                if (event.maximum > 0) {
                    msg += " (" + event.current + "/" + event.maximum + ")";
                    if (stage == "training") {
                        SwingUtilities.invokeLater(() -> {
                            this.mainGUI.updateTrainingProgress(event.message, event.current, event.maximum);
                        });
                    } else if (stage == "prediction") {
                        SwingUtilities.invokeLater(() -> {
                            this.mainGUI.updatePredictionProgress(event.message, event.current, event.maximum);
                        });
                    }
                }
                if (msg.startsWith("ERROR")) {
                    logger.error("[Python] " + msg);
                } else {
                    logger.info("[Python] " + msg);
                }
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
        return event.task;
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
        logger.info("creating the python environment...");
        try {
            String home = System.getProperty("user.home");
            String env_dir = Paths.get(home, "appose_careamics_env").toString();
            
            final Environment env = Appose.uv()
                .python("3.11")
                .include("appose>=0.12.0", "careamics==0.3.5")
                // .include("pydantic==2.13.3")
                .include("git+https://github.com/CAREamics/careamics-appose.git")
                .name("appose_careamics_env")
                .base(env_dir)
                .subscribeProgress((title, current, maximum) -> {
                    logger.info(title + ": " + current + "/" + maximum);
                    SwingUtilities.invokeLater(() -> {
                        this.mainGUI.updateTrainingProgress(title, current, maximum);
                    });
                })
                // .subscribeOutput(line -> logger.info(line))
                .subscribeError(line -> logger.info(line))
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

    private String getExportScript() {
        String exportScript = null;
        try (InputStream instream = this.getClass().getClassLoader().getResourceAsStream("model_export.py")) {
            exportScript = new String(instream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exportScript;
    }

}
