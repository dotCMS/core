package com.dotcms.rest.api.v1.experiment;

public class ExperimentVariant {
    private DotCMSVariant variant;
    private int trafficPercentage;
    private boolean original;

    public ExperimentVariant(DotCMSVariant variant,
            int trafficPercentage,
            boolean original) {

        this.variant = variant;
        this.trafficPercentage = trafficPercentage;
        this.original = original;
    }

    public DotCMSVariant getVariant() {
        return variant;
    }

    public int getTrafficPercentage() {
        return trafficPercentage;
    }

    public boolean isOriginal() {
        return original;
    }
}
