package com.dotcms.security.apps;

import static org.apache.commons.io.FilenameUtils.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;

/***
 * {@inheritDoc}
 */
public class AppDescriptorImpl implements AppDescriptor {

    @JsonIgnore
    private transient final String fileName;

    private final String key;

    private final String name;

    private final String description;

    private final String iconUrl;

    private final Boolean allowExtraParameters;

    private final Map<String, ParamDescriptor> params;

    /**
     *
     * @param fileName
     * @param name
     * @param description
     * @param iconUrl
     * @param allowExtraParameters
     */

    public AppDescriptorImpl(final String fileName,
            final String name,
            final String description,
            final String iconUrl,
            final Boolean allowExtraParameters,
            final Map<String, ParamDescriptor> params) {
        this.fileName = fileName;
        this.key = removeExtension(fileName);
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.allowExtraParameters = allowExtraParameters;
        this.params = params;
    }

    /**
     *
     *
     * @param appSchema
     */
    public AppDescriptorImpl(final String fileName, final AppSchema appSchema) {
        this(fileName, appSchema.getName(), appSchema.getDescription(), appSchema.getIconUrl(),
                appSchema.getAllowExtraParameters(), appSchema.getParams());
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    public boolean isAllowExtraParameters() {
        return allowExtraParameters;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    public Boolean getAllowExtraParameters() {
        return allowExtraParameters;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    public Map<String, ParamDescriptor> getParams() {
        return new LinkedHashMap<>(params);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AppDescriptorImpl that = (AppDescriptorImpl) object;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
