package com.dotcms.security.apps;

import static org.apache.commons.io.FilenameUtils.removeExtension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import java.util.Map;

/***
 * This is a bean used to compile the info coming from the yml which is read into the bean  {@link AppSchema }
 * This class is basically an extension that adds the key and the file-name
 */
public class AppDescriptorImpl extends AppSchema implements AppDescriptor {

    @JsonIgnore
    private final String fileName;

    @JsonIgnore
    private final boolean systemApp;

    /**
     * Application key
     */
    private final String key;

    /**
     * Takes all the params individually to build the AppDescriptor
     * Used by a DataGen Builder pattern
     * @param fileName
     * @param name
     * @param description
     * @param iconUrl
     * @param allowExtraParameters
     */
    @VisibleForTesting
    public AppDescriptorImpl(final String fileName,
            final boolean systemApp,
            final String name,
            final String description,
            final String iconUrl,
            final Boolean allowExtraParameters,
            final Map<String, ParamDescriptor> params) {
        super(name, description, iconUrl, allowExtraParameters, params);
        this.fileName = fileName;
        this.systemApp =  systemApp;
        this.key = removeExtension(fileName);
    }

    /**
     * Takes a file name and an appSchema to build AppsDescriptor
     * @param fileName
     * @param appSchema
     */
    AppDescriptorImpl(final String fileName, final boolean systemApp, final AppSchema appSchema) {
        this(fileName,systemApp, appSchema.getName(), appSchema.getDescription(), appSchema.getIconUrl(),
                appSchema.getAllowExtraParameters(), appSchema.getParams());
    }

    /**
     * file name property is only of interest to members of this package
     * @return
     */
    String getFileName() {
        return fileName;
    }

    /**
     * The key is extracted from the file name and aggregated as it is needed by the functionality
     * @return
     */
    public String getKey() {
        return key;
    }


    /**
     * This tells you if this app is a system app
     * if false, it means the app has been uploaded by a user
     * @return
     */
    public boolean isSystemApp() {
        return systemApp;
    }
}
