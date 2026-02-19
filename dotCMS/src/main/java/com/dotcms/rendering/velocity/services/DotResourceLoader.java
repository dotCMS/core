package com.dotcms.rendering.velocity.services;


import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.util.concurrent.Striped;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class DotResourceLoader extends ResourceLoader {

    private static DotResourceLoader instance;

    private static final boolean useCache = Config.getBooleanProperty("VELOCITY_CACHING_ON", true);

    /**
     * Timeout in milliseconds for acquiring the resource generation lock.
     * If a resource takes longer than this to generate, waiting threads will timeout.
     * Configurable via VELOCITY_RESOURCE_LOAD_TIMEOUT_MS property.
     */
    private static final long RESOURCE_LOAD_TIMEOUT_MS =
            Config.getLongProperty("VELOCITY_RESOURCE_LOAD_TIMEOUT_MS", 15000L); // 15 seconds default

    /**
     * Number of lock stripes for resource generation.
     * Higher values reduce contention but use more memory.
     * Configurable via VELOCITY_RESOURCE_LOAD_STRIPES property.
     */
    private static final int RESOURCE_LOAD_STRIPES =
            Config.getIntProperty("VELOCITY_RESOURCE_LOAD_STRIPES", 64);

    /**
     * Striped lock to prevent duplicate resource generation.
     * Only one thread can generate a resource for a given key at a time.
     * Other threads waiting for the same key will wait until generation completes or timeout.
     */
    private static final Striped<Lock> resourceLock = Striped.lazyWeakLock(RESOURCE_LOAD_STRIPES);

    public DotResourceLoader() {
        super();
    }


    @Override
    public InputStream getResourceStream(final String filePath) throws ResourceNotFoundException {
        if (!UtilMethods.isSet(filePath)) {
            throw new ResourceNotFoundException("cannot find resource");
        }

        final VelocityResourceKey key = new VelocityResourceKey(filePath);

        Logger.debug(this, "DotResourceLoader:\t: " + key);

        // Use striped lock to prevent duplicate resource generation for the same key.
        // This prevents the "thundering herd" problem where multiple threads try to
        // generate the same expensive resource simultaneously.
        final Lock lock = resourceLock.get(key.path);
        boolean hasLock = false;

        try {
            hasLock = lock.tryLock(RESOURCE_LOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!hasLock) {
                Logger.warn(this, "Timeout waiting for resource generation lock: " + key.path +
                        " after " + RESOURCE_LOAD_TIMEOUT_MS + "ms");
                throw new ResourceNotFoundException(
                        "Timeout waiting for velocity resource generation: " + key.path);
            }

            return loadResource(key);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResourceNotFoundException("Interrupted while waiting for resource: " + key.path, e);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Logger.warn(this, "filePath: " + filePath + ", msg:" + e.getMessage(), e);
            CacheLocator.getVeloctyResourceCache().addMiss(key.path);
            throw new ResourceNotFoundException("Cannot parse velocity file : " + key.path, e);
        } finally {
            if (hasLock) {
                lock.unlock();
            }
        }
    }

    /**
     * Loads the velocity resource based on its type.
     * This method is called while holding the resource lock to prevent duplicate generation.
     *
     * @param key the velocity resource key
     * @return the resource as an InputStream
     * @throws Exception if resource loading fails
     */
    private InputStream loadResource(final VelocityResourceKey key) throws Exception {
        switch (key.type) {
            case CONTAINER:
                return new ContainerLoader().writeObject(key);
            case TEMPLATE:
                return new TemplateLoader().writeObject(key);
            case CONTENT:
                return new ContentletLoader().writeObject(key);
            case FIELD:
                return new FieldLoader().writeObject(key);
            case CONTENT_TYPE:
                return new ContentTypeLoader().writeObject(key);
            case SITE:
                return new SiteLoader().writeObject(key);
            case HTMLPAGE:
                return new PageLoader().writeObject(key);
            case VELOCITY_MACROS:
            case VTL:
            case VELOCITY_LEGACY_VL:
                return VTLLoader.instance().writeObject(key);
            default:
                return IncludeLoader.instance().writeObject(key);
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
