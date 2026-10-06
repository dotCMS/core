package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This is bean meant to read data from the input yaml file that describes the service
 * The file might look a bit like this:
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
 *   required: false
 *   envvar: "some value set via environment variable"
 *   envshow: true
 *
 */
public class AppSchema implements Serializable {

    protected final String name;

    protected final String description;

    protected final String iconUrl;

    protected final String icon;

    protected final String color;

    protected final Boolean allowExtraParameters;

    protected final Map<String, ParamDescriptor> params;

    /**
     * Convenience overload kept so legacy test code and call sites that predate the
     * {@code icon} / {@code color} fields don't need to be updated. Delegates with nulls.
     */
    @VisibleForTesting
    public AppSchema(
            final String name,
            final String description,
            final String iconUrl,
            final Boolean allowExtraParameters,
            final Map<String, ParamDescriptor> params) {
        this(name, description, iconUrl, null, null, allowExtraParameters, params);
    }

    /**
     * This constructor isn't used by the object mapper that reads the yml files.
     * it's only meant to be used for testing
     * @param name
     * @param description
     * @param iconUrl
     * @param icon Material icon name used as a fallback when no {@code iconUrl} is set
     * @param color Hex color (e.g. {@code #3b82f6}) or PrimeNG token (e.g. {@code blue}) used to tint the icon
     * @param allowExtraParameters
     */

    @VisibleForTesting
    @JsonCreator
    public AppSchema(
            @JsonProperty("name") final String name,
            @JsonProperty("description") final String description,
            @JsonProperty("iconUrl") final String iconUrl,
            @JsonProperty("icon") final String icon,
            @JsonProperty("color") final String color,
            @JsonProperty("allowExtraParameters") final Boolean allowExtraParameters,
            @JsonProperty("params") final Map<String, ParamDescriptor> params) {
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.icon = icon;
        this.color = color;
        this.allowExtraParameters = allowExtraParameters;
        this.params = params;
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
     * Material icon name used when no {@link #iconUrl} is set.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Hex color (e.g. {@code #3b82f6}) or PrimeNG token (e.g. {@code blue}) used to tint the icon.
     */
    public String getColor() {
        return color;
    }

    /**
     * Tells the API if we allow any additional beside the ones already defined in the params map.
     * @return
     */
    public boolean isAllowExtraParameters() {
        return allowExtraParameters;
    }

    /**
     * Tells the API if we allow any additional beside the ones already defined in the params map.
     * @return
     */
    public Boolean getAllowExtraParameters() {
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

    /**
     * Equals implementation
     * @param object
     * @return
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AppSchema appSchema = (AppSchema) object;
        return name.equals(appSchema.name) &&
                description.equals(appSchema.description) &&
                Objects.equals(iconUrl, appSchema.iconUrl) &&
                Objects.equals(icon, appSchema.icon) &&
                Objects.equals(color, appSchema.color) &&
                allowExtraParameters.equals(appSchema.allowExtraParameters) &&
                params.equals(appSchema.params);
    }

    /**
     * hash code impl
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, description, iconUrl, icon, color, allowExtraParameters, params);
    }
}
