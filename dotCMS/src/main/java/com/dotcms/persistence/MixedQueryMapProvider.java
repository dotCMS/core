package com.dotcms.persistence;


import com.liferay.util.PropertiesUtil;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.ReflectionUtils.newInstance;

/**
 * Mixed Query Map Provider creates a map using first the classpath files, then the filesystem files and finally
 * the {@link QueryMap} implementations.
 *
 * The way to get the resources will depend on the strategy.
 *
 * @author jsanca
 */
public class MixedQueryMapProvider implements QueryMapProvider {

    private final ResourceNameStrategy classPathResourceNameStrategy;
    private final ResourceNameStrategy fileSystemResourceNameStrategy;
    private final ResourceNameStrategy queryMapResourceNameStrategy;

    public MixedQueryMapProvider(final ResourceNameStrategy classPathResourceNameStrategy,
                                 final ResourceNameStrategy fileSystemResourceNameStrategy,
                                 final ResourceNameStrategy queryMapResourceNameStrategy) {

        this.classPathResourceNameStrategy = classPathResourceNameStrategy;
        this.fileSystemResourceNameStrategy = fileSystemResourceNameStrategy;
        this.queryMapResourceNameStrategy = queryMapResourceNameStrategy;
    }

    @Override
    public Map<String, String> getQueryMap(final Class persistanceClass) {

        final Map<String, String> queryMap = map();
        // first class path files.
        final List<String> classpathResourceNames  =
                this.classPathResourceNameStrategy.getResourceNames(persistanceClass);
        final String genericClasspathResource      =
                this.getGenericResource (classpathResourceNames);

        final List<String> filesystemResourceNames =
                this.fileSystemResourceNameStrategy.getResourceNames(persistanceClass);
        final String genericFilesystemResource     =
                this.getGenericResource (filesystemResourceNames);

        final List<String> queryMapResourceNames   =
                this.queryMapResourceNameStrategy.getResourceNames(persistanceClass);
        final String genericQueryMapResource       =
                this.getGenericResource (queryMapResourceNames);

        // loads the first ones, the generic's ones
        this.loadProperties      (genericClasspathResource,  queryMap);
        this.loadProperties      (genericFilesystemResource, queryMap);
        this.loadProperties      (genericQueryMapResource,   queryMap);

        // load the rest (the vendor specific)
        this.loadProperties      (classpathResourceNames,    queryMap);
        this.loadProperties      (filesystemResourceNames,   queryMap);
        this.loadQueryMapClasses (queryMapResourceNames,     queryMap);

        return queryMap;
    } // getQueryMap.

    private String getGenericResource(final List<String> classpathResourceNames) {

        String genericResource = null;

        if (null != classpathResourceNames && classpathResourceNames.size() >= 1) {

            genericResource = classpathResourceNames.get(0);
            classpathResourceNames.remove(0);
        }

        return genericResource;
    } // getGenericResource.

    protected void loadProperties(final List<String> resourceNames,
                                           final Map<String, String> queryMap) {

        if (null != resourceNames) {
            for (String resourceName : resourceNames) {

                this.loadProperties(resourceName, queryMap);
            }
        }
    } // loadProperties.

    protected void loadProperties(final String resourceName,
                                  final Map<String, String> queryMap) {

        Properties propertiesQuery = null;

        if (null != resourceName) {

            propertiesQuery = PropertiesUtil.load(resourceName);
            if (null != propertiesQuery) {

                queryMap.putAll(PropertiesUtil.fromProperties(propertiesQuery));
            }
        }
    } // loadProperties.

    private void loadQueryMapClasses(final List<String> queryMapResourceNames,
                                     final Map<String, String> queryMap) {

        QueryMap queryMapInstance = null;

        if (null != queryMapResourceNames) {

            for (String queryMapResource : queryMapResourceNames) {

                queryMapInstance = (QueryMap)newInstance(queryMapResource);
                if (null != queryMapInstance) {

                    queryMap.putAll(queryMapInstance.getQueryMap());
                }
            }
        }
    } // loadQueryMapClasses.
} // E:O:F:MixedQueryMapProvider.
