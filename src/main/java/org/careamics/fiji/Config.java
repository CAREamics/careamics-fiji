package org.careamics.fiji;

import java.util.HashMap;
import java.util.Map;


public class Config {
    public String experimentName;
    public String axes;
    public int[] patchSize;
    public int batchSize;
    public int numEpochs;
    public int numSteps;
    public String[] augmentations;
    public Integer numChannels;  // can be null if not specified
    public String trainSource;
    public boolean fromDisk;

    // constructors
    public Config() {
        
    }

    public Config(
        String experimentName,
        String axes,
        int[] patchSize,
        int batchSize,
        int numEpochs,
        int numSteps,
        Integer numChannels,
        String[] augmentations,
        String trainSource,
        boolean fromDisk
    ) {
        this.experimentName = experimentName;
        this.axes = axes;
        this.augmentations = augmentations;
        this.patchSize = patchSize;
        this.batchSize = batchSize;
        this.numEpochs = numEpochs;
        this.numSteps = numSteps;
        this.numChannels = numChannels;
        this.trainSource = trainSource;
        this.fromDisk = fromDisk;
    }

    public Map<String, Object> toDictionary() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("experiment_name", experimentName);
        // dict.put("data_type", dataType);
        dict.put("axes", axes);
        dict.put("patch_size", patchSize);
        dict.put("batch_size", batchSize);
        dict.put("num_epochs", numEpochs);
        dict.put("num_steps", numSteps);
        // dict.put("augmentations", augmentations);
        dict.put("num_channels", numChannels);
        dict.put("train_source", trainSource);
        dict.put("from_disk", fromDisk);
        return dict;
    }


    @Override
    public String toString() {
        return "Config{" +
                "experimentName='" + experimentName + '\'' +
                ", axes='" + axes + '\'' +
                ", patchSize=" + java.util.Arrays.toString(patchSize) +
                ", batchSize=" + batchSize +
                ", numEpochs=" + numEpochs +
                ", numSteps=" + numSteps +
                ", augmentations=" + java.util.Arrays.toString(augmentations) +
                ", numChannels=" + numChannels +
                ", trainSource='" + trainSource + '\'' +
                '}';
    }

}
