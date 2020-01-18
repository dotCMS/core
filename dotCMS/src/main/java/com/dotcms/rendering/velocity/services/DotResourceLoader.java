package com.dotcms.rendering.velocity.services;


import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.InputStream;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class DotResourceLoader extends ResourceLoader {


    private static DotResourceLoader instance;

    private static boolean useCache = Config.getBooleanProperty("VELOCITY_CACHING_ON", true);
    public DotResourceLoader() {
        super();
    }


    @Override
    public InputStream getResourceStream(final String filePath) throws ResourceNotFoundException {
        if (!UtilMethods.isSet(filePath)) {
            throw new ResourceNotFoundException("cannot find resource");
        }



        synchronized (filePath.intern()) {

            VelocityResourceKey key = new VelocityResourceKey(filePath);

            Logger.debug(this, "DotResourceLoader:\t: " + key);

            try {
                switch (key.type) {
                    case CONTAINER: {
                        return new ContainerLoader().writeObject(key);
                    }
                    case TEMPLATE: {
                        return new TemplateLoader().writeObject(key);
                    }
                    case CONTENT: {
                        return new ContentletLoader().writeObject(key);
                    }
                    case FIELD: {
                        return new FieldLoader().writeObject(key);
                    }
                    case CONTENT_TYPE: {
                        return new ContentTypeLoader().writeObject(key);
                    }
                    case SITE: {
                        return new SiteLoader().writeObject(key);
                    }
                    case HTMLPAGE: {
                        return new PageLoader().writeObject(key);
                    }
                    case VELOCITY_MACROS: {
                        return VTLLoader.instance().writeObject(key);
                    }
                    case VTL: {
                        return VTLLoader.instance().writeObject(key);
                    }
                    case VELOCITY_LEGACY_VL: {
                        return VTLLoader.instance().writeObject(key);
                    }
                    default: {
                        return IncludeLoader.instance().writeObject(key);
                    }
                }

            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                CacheLocator.getVeloctyResourceCache().addMiss(key.path);
                throw new ResourceNotFoundException("Cannot parse velocity file : " + key.path, e);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.velocity.runtime.resource.loader.FileResourceLoader#getLastModified(org.apache.
     * velocity.runtime.resource.Resource)
     */
    @Override
    public long getLastModified(Resource resource) {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.velocity.runtime.resource.loader.FileResourceLoader#isSourceModified(org.apache.
     * velocity.runtime.resource.Resource)
     */
    @Override
    public boolean isSourceModified(Resource resource) {
        return false;
    }

    public static DotResourceLoader getInstance() {
        if (instance == null) {
            synchronized (DotResourceLoader.class) {
                if (instance == null) {
                    instance = new DotResourceLoader();
                }
            }
        }
        return instance;
    }


    @Override
    public void init(ExtendedProperties configuration) {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean isCachingOn() {
        return useCache;
    }



}
