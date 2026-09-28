package org.careamics.fiji;

import java.util.HashMap;
import java.util.Map;


public class Config {
    public String experimentName;
    public String dataType;
    public String axes;
    public int[] patchSize;
    public int batchSize;
    public int numEpochs;
    public int numSteps;
    public String[] augmentations;
    public int numChannels;

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
        int numChannels,
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
        this.numChannels = numChannels;
    }

    public Map<String, Object> toDictionary() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("experiment_name", experimentName);
        dict.put("data_type", dataType);
        dict.put("axes", axes);
        dict.put("patch_size", patchSize);
        dict.put("batch_size", batchSize);
        dict.put("num_epochs", numEpochs);
        dict.put("num_steps", numSteps);
        // dict.put("augmentations", augmentations);
        dict.put("num_channels", numChannels);
        return dict;
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
                ", numChannels=" + numChannels +
                '}';
    }

}
