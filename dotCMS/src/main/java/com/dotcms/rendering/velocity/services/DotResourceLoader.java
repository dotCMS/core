package com.dotcms.rendering.velocity.services;


import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.InputStream;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class DotResourceLoader extends ResourceLoader {


    private static DotResourceLoader instance;

    private static boolean useCache = Config.getBooleanProperty("VELOCITY_CACHING_ON", true);
    public DotResourceLoader() {
        super();
    }


    /**
     * Opens the Velocity source for a resource path, choosing the loader by the path's type.
     *
     * <p>A failure normally records a cache miss and is reported as a
     * {@link ResourceNotFoundException}. With S3 asset storage on, a {@link DotDataException}
     * from a loader that reads a stored file (VTL, macro, legacy VL and include files) means the
     * file could not be restored from storage. It is reported as a plain {@link VelocityException}
     * and no miss is recorded, so the next render tries the storage again.
     *
     * @param filePath the Velocity resource path
     * @return the resource source
     * @throws ResourceNotFoundException if the path is empty or the resource cannot be loaded
     */
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
                Logger.warn(this, "filePath: " + filePath + ", msg:" + e.getMessage(), e);
                if (com.dotcms.storage.AssetStorageFeature.isEnabled()
                        && e instanceof DotDataException && readsStoredFile(key.type)) {
                    throw new VelocityException("Unable to load stored resource: " + key.path, e);
                }
                CacheLocator.getVeloctyResourceCache().addMiss(key.path);
                throw new ResourceNotFoundException("Cannot parse velocity file : " + key.path, e);
            }
        }

    }

    /**
     * Tells whether a resource type is loaded from a file that S3 asset storage may have to
     * restore. These are the types {@link #getResourceStream} hands to {@link VTLLoader} and
     * {@link IncludeLoader}; the other loaders build their source from the database.
     *
     * @param type the resource type
     * @return {@code true} for VTL, macro, legacy VL and include files
     */
    private static boolean readsStoredFile(final VelocityType type) {
        switch (type) {
            case CONTAINER:
            case TEMPLATE:
            case CONTENT:
            case FIELD:
            case CONTENT_TYPE:
            case SITE:
            case HTMLPAGE:
                return false;
            default:
                return true;
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
