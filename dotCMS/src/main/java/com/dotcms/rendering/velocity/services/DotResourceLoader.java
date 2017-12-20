package com.dotcms.rendering.velocity.services;


import com.dotcms.repackage.org.apache.commons.collections.ExtendedProperties;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.InputStream;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class DotResourceLoader extends ResourceLoader {


    private static DotResourceLoader instance;


    public DotResourceLoader() {
        super();
    }

    
    @Override
    public InputStream getResourceStream(final String filePath) throws ResourceNotFoundException {
        if (!UtilMethods.isSet(filePath)) {
            throw new ResourceNotFoundException("cannot find resource");
        }


        final VelocityType type = VelocityType.resolveVelocityType(filePath);
        synchronized (filePath.intern()) {



            String paths = (filePath.charAt(0) == '/') ? filePath.substring(1, filePath.length()) : filePath;

            final String[] path = paths.split("[/\\.]", -1);
            final PageMode mode = PageMode.get(path[0]);
            final String id1 = path[1].indexOf("_") > -1 ? path[1].substring(0, path[1].indexOf("_")) : path[1];
            final String language = path[1].indexOf("_") > -1 ? path[1].substring(path[1].indexOf("_") + 1, path[1].length())
                    : String.valueOf(APILocator.getLanguageAPI()
                        .getDefaultLanguage()
                        .getId());

            final String id2 = path.length > 3 ? path[2] : null;


            Logger.debug(this, "DotResourceLoader:\tInode: " + id1);

            try {
                switch (type) {
                    case NOT_VELOCITY: {
                        CacheLocator.getVeloctyResourceCache()
                        .addMiss(path);
                    throw new ResourceNotFoundException("Cannot find velocity file : " + path);
                    }
                    case CONTAINER: {
                        return new ContainerLoader().writeObject(id1, id2, mode, language, filePath);
                    }
                    case TEMPLATE: {

                        return new TemplateLoader().writeObject(id1, id2, mode, language, filePath);

                    }
                    case CONTENT: {
                        return new ContentletLoader().writeObject(id1, id2, mode, language, filePath);
                    }
                    case FIELD: {
                        return new FieldLoader().writeObject(id1, id2, mode, language, filePath);
                    }
                    case CONTENT_TYPE: {
                        return new ContentTypeLoader().writeObject(id1, id2, mode, language, filePath);
                    }
                    case SITE: {
                        return new SiteLoader().writeObject(id1, id2, mode, language, filePath);

                    }
                    case HTMLPAGE: {
                        return new PageLoader().writeObject(id1, id2, mode, language, filePath);

                    }
                    case VELOCITY_MACROS: {
                        return VTLLoader.instance()
                            .writeObject(id1, id2, mode, language, filePath);
                    }
                    case VTL: {
                        return VTLLoader.instance()
                            .writeObject(id1, id2, mode, language, filePath);
                    }
                    case VELOCITY_LEGACY_VL: {
                        return VTLLoader.instance()
                            .writeObject(id1, id2, mode, language, filePath);
                    }
                    default: {
                        CacheLocator.getVeloctyResourceCache()
                            .addMiss(path);
                        throw new ResourceNotFoundException("Cannot find velocity file : " + path);
                    }
                }

            } catch (Exception e) {
                throw new ResourceNotFoundException("Cannot parse velocity file : " + path, e);
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



}
