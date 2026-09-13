package org.careamics.fiji;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import net.imagej.ImageJ;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.ops.bufferfactories.ImgImgSameTypeFactory;
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


@Plugin(type = Command.class, menuPath = "Plugins>CAREamics>Noise2Void")
public class App extends DynamicCommand implements Initializable {

    @Parameter
    private LogService logger;

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
            final App plugin = new App();
            context.inject(plugin);
            plugin.run();
        }
	}

    @Override
    public void run() {
        // get the current image
        // logger.info("Image count: " + WindowManager.getImageCount());
        ImagePlus imgp = WindowManager.getCurrentImage();
        if (imgp == null) {
            IJ.error("No image is available.");
            return;
        }

        // process the image by running the python script
        final Task task = processImage(imgp);
        
        if ( task.status != TaskStatus.COMPLETE )
            throw new RuntimeException("Python script failed with error: " + task.error);
        
        final NDArray prediction = (NDArray) task.outputs.get("prediction");
        // final Img<?> output = arrayToImage(prediction);
        // ShmImg<FloatType> img = new ShmImg<>(prediction);
        ArrayImg<FloatType, ?> view = NDArrays.asArrayImg(prediction);
        // NDArray copied = NDArrays.asNDArray(img);
        ImageJFunctions.show(view);
        // ImageJFunctions.wrap(prediction, "Prediction");

    }

    private Task processImage(final ImagePlus imgp) {
        logger.info("creating the uv environment...");
        final Environment env = createEnvironment();
        
        final String n2vScript = getN2VScript();
        // logger.info(n2vScript);
        
        final Map<String, Object> inputs = new HashMap<>();
        inputs.put("input_image", imageToAppose(imgp));
        
        inputs.put("num_epochs", 2);
        
        try (Service python = env.python()) {
            final Task task = python.task(n2vScript, inputs);
            
            // listen for task updates
            task.listen(event -> {
                switch (event.responseType) {
                    case UPDATE:
                        logger.info("[Python backend]" + event.message);
                        break;
                    
                    case COMPLETION:
                        IJ.log("Task completed successfully.");
                        break;
                    
                    case CANCELATION:
                        IJ.log("Task was cancelled.");
                        break;
                    
                    case FAILURE:
                        IJ.error("Task failed", event.task.error);
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
                .include("careamics==0.3.2", "appose>=0.12.0")
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
        try (InputStream instream = this.getClass().getClassLoader().getResourceAsStream("n2v/n2v.py")) {
            n2vScript = new String(instream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return n2vScript;
    }

}
