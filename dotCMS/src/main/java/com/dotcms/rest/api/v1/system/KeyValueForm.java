package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

/**
 * Encapsulates a key value pair
 */
public class KeyValueForm extends Validated {

    @NotNull
    private final String key;

    @NotNull
    private final String value;

    public KeyValueForm(final String key, final String value) {
        this.key = key;
        this.value = value;
        this.checkValid();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
