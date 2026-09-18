package org.careamics.fiji;


public class Config {
    public String experiment_name;
    public String dataType;
    public String axes;
    public int[] patchSize;
    public int batchSize;
    public int numEpochs;
    public int numSteps;
    public String[] augmentations;

    // constructors
    public Config() {
        
    }

    public Config(
        String experiment_name,
        String dataType,
        String axes,
        int[] patchSize,
        int batchSize,
        int numEpochs,
        int numSteps,
        String[] augmentations
    ) {
        this.experiment_name = experiment_name;
        this.dataType = dataType;
        this.axes = axes;
        this.augmentations = augmentations;
        this.patchSize = patchSize;
        this.batchSize = batchSize;
        this.numEpochs = numEpochs;
        this.numSteps = numSteps;
    }

    @Override
    public String toString() {
        return "Config{" +
                "experiment_name='" + experiment_name + '\'' +
                ", dataType='" + dataType + '\'' +
                ", axes='" + axes + '\'' +
                ", patchSize=" + java.util.Arrays.toString(patchSize) +
                ", batchSize=" + batchSize +
                ", numEpochs=" + numEpochs +
                ", numSteps=" + numSteps +
                ", augmentations=" + java.util.Arrays.toString(augmentations) +
                '}';
    }

}
