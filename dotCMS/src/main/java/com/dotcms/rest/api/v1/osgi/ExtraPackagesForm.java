package com.dotcms.rest.api.v1.osgi;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form for extra packages
 * @author jsanca
 */
public class ExtraPackagesForm extends Validated {

    @NotNull
    private final String packages;

    @JsonCreator
    public ExtraPackagesForm(@JsonProperty("packages") final String packages) {
        this.packages = packages;
        checkValid();
    }

    public String getPackages() {

        return packages;
    }
}
