package org.careamics.fiji;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import ij.IJ;
// import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import net.imagej.ImageJ;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.appose.NDArrays;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

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
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type=Command.class, menuPath="Plugins>CAREamics>Noise2Void")
public class App extends DynamicCommand implements Initializable {

    @Parameter
	private LogService logService;

    @Override
	public void initialize() {
		
	}

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        // ij.ui().showUI();
        ij.launch(args);

        // ij.command().run(HelloWorld.class, true);

        // ask the user for a file to open
        final File file = new File("/Users/mehdi.seifi/Projects/CAREamics/tmp_data_src/data/SEM/val/val.tif");
        // ij.ui().chooseFile(null, "open");

        if (file != null) {
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(App.class, true);
        }
    }

    @Override
    public void run() {
        // System.out.println("Hello World!");
        logService.info("Hello World!");

        String uvProjectToml = null;
        String n2vScript = null;

        logService.info(uvProjectToml);

        final Environment env = createEnvironment();

        final String n2vScript = getN2VScript();

        // System.out.println(n2vScript);
        logService.info(n2vScript);
    }

    private Environment createEnvironment() {
        try {
            String tomlFile = this.getClass().getClassLoader().getResource("pyproject.toml").getPath();
            final Environment env = Appose.uv().file(tomlFile)
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
        try (InputStream instream = this.getClass().getClassLoader().getResourceAsStream("n2v/n2v.py")) {
            n2vScript = new String(instream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return n2vScript;
    }

    // private String getPyProjectToml() throws IOException {
    //     String pyProjectToml = "";
    //     try (InputStream instream = this.getClass().getClassLoader().getResourceAsStream("pyproject.toml")) {
    //         pyProjectToml = new String(instream.readAllBytes(), StandardCharsets.UTF_8);

    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }

    //     return pyProjectToml;
    // }
}
