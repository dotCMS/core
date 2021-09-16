package com.dotcms.rest.api.v2.languages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MakeDefaultLangForm {

    private final boolean fireTransferAssetsJob;

    @JsonCreator
    public MakeDefaultLangForm(
            @JsonProperty("fireTransferAssetsJob") final boolean fireTransferAssetsJob
    ) {
        this.fireTransferAssetsJob = fireTransferAssetsJob;
    }

    public boolean isFireTransferAssetsJob() {
        return fireTransferAssetsJob;
    }
}
