package com.dotcms.rest.api.v1.apps;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Form used to feed-in secrets
 */
public class SecretForm extends Validated {

    @NotNull
    private final Map<String, Input> inputParams;

    @JsonCreator
    public SecretForm(final Map<String, Input> inputParams) {
        super();
        this.inputParams = inputParams;
    }

    /**
     * Param Name and Value Map
     * @return
     */
    Map<String, Input> getInputParams() {
        return inputParams;
    }

    void destroySecretTraces() {
        final Map<String, Input> inputParams = getInputParams();
        if (UtilMethods.isSet(inputParams)) {
            for (final Input input : inputParams.values()) {
                input.destroySecret();
            }
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }
}
