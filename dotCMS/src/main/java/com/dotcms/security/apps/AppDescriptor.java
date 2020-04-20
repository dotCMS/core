package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This is bean meant to read data from the input yaml file that describes the service
 * The file might look a bit like this:
 * key: "slack-service"
 * name: "Slack"
 * description: Slack emerges as an internal tool used by the company Tiny Speck
 *
 * iconUrl: "/slackIcon.png"
 * allowExtraParameters:false
 * params:
 *  param1:
 *   value: "value-1"
 *   hidden: false
 *   type: "STRING"
 *   label: "label"
 *   hint: "hint"
 *  required: false
 *
 */
public class AppDescriptor {

    private final String key;

    private final String name;

    private final String description;

    private final String iconUrl;

    private final boolean allowExtraParameters;

    private final Map<String, ParamDescriptor> params;

    /**
     * This constructor isn't used by the object mapper that reads the yml files.
     * it's only meant to be used for testing
     * @param key
     * @param name
     * @param description
     * @param iconUrl
     * @param allowExtraParameters
     */

    @VisibleForTesting
    @JsonCreator
    public AppDescriptor(@JsonProperty("key") final String key,
            @JsonProperty("name") final String name,
            @JsonProperty("description") final String description,
            @JsonProperty("iconUrl") final String iconUrl,
            @JsonProperty("allowExtraParameters") final boolean allowExtraParameters,
            @JsonProperty("params") final Map<String, ParamDescriptor> params) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.allowExtraParameters = allowExtraParameters;
        this.params = params;
    }

    /**
     * Service unique identifier
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * Any name
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Any meaningful read
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * an avatar URL
     * @return
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * Tells the API if we allow any additional beside the ones already defined in the params map.
     * @return
     */
    public boolean isAllowExtraParameters() {
        return allowExtraParameters;
    }

    /**
     * Holds the definition of the params expected by the service.
     * This method returns a defensive copy.
     * @return
     */
    public Map<String, ParamDescriptor> getParams() {
        return new LinkedHashMap<>(params);
    }

    public void addParam(final String name, final String value, final boolean hidden,
            final Type type, final String label, final String hint, final boolean required) {
        params.put(name, ParamDescriptor.newParam(value, hidden, type, label, hint, required));
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AppDescriptor that = (AppDescriptor) object;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
