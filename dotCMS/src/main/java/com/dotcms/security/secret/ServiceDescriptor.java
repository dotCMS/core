package com.dotcms.security.secret;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
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
public class ServiceDescriptor {

    private String key;

    private String name;

    private String description;

    private String iconUrl;

    private boolean allowExtraParameters;

    private Map<String,Param> params;

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
    public ServiceDescriptor(final String key, final String name, final String description,
            final String iconUrl, final boolean allowExtraParameters) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.allowExtraParameters = allowExtraParameters;
    }

    /**
     * Required by the Object Mapper that reads the yml files.
     * Because of this the attributes on this class can not be made final.
     */
    public ServiceDescriptor() {

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
     * Holds the definition of the params expected by the service
     * @return
     */
    public Map<String, Param> getParams() {
        if(null == params){
            params = new HashMap<>();
        }
        return params;
    }

    public void addParam(final String name, final String value, final boolean hidden,
            final Type type, final String label, final String hint, final boolean required) {
        getParams().put(name, Param.newParam(value, hidden, type, label, hint, required));
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final ServiceDescriptor that = (ServiceDescriptor) object;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
