package com.dotcms.rest.api.v1.system;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates a key value pair
 * @author jsanca
 */
public class KeyValueForm extends Validated {

    @NotNull
    private final String key;

    @NotNull
    private final String value;

    @JsonCreator
    public KeyValueForm(@JsonProperty("key") final String key,
                        @JsonProperty("value") final String value) {
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
