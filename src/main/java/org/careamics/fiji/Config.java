package org.careamics.fiji;


public class Config {
    public String experimentName;
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
        String experimentName,
        String dataType,
        String axes,
        int[] patchSize,
        int batchSize,
        int numEpochs,
        int numSteps,
        String[] augmentations
    ) {
        this.experimentName = experimentName;
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
                "experimentName='" + experimentName + '\'' +
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
