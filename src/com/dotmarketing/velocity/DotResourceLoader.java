package com.dotmarketing.velocity;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.FieldServices;
import com.dotmarketing.services.HostServices;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.services.TemplateServices;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.LanguageWebAPI;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class DotResourceLoader extends ResourceLoader {

    final String[] velocityCMSExtenstions = { Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION"),
            Config.getStringProperty("VELOCITY_CONTENT_EXTENSION"), Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION"),
            Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION"), Config.getStringProperty("VELOCITY_CONTENT_MAP_EXTENSION"),
            Config.getStringProperty("VELOCITY_BANNER_EXTENSION"),Config.getStringProperty("VELOCITY_STRUCTURE_EXTENSION"),
            Config.getStringProperty("VELOCITY_FIELD_EXTENSION"),Config.getStringProperty("VELOCITY_HOST_EXTENSION")};

    private String VELOCITY_ROOT = null;
    private String VELOCITY_CONTAINER_EXTENSION = null;
    private String VELOCITY_CONTENT_EXTENSION = null;
    private String VELOCITY_CONTENT_MAP_EXTENSION = null;
    private String VELOCITY_FIELD_EXTENSION = null;
    private String VELOCITY_HTMLPAGE_EXTENSION = null;
    private String VELOCITY_TEMPLATE_EXTENSION = null;
    private String VELOCITY_STRUCTURE_EXTENSION = null;
    private String VELOCITY_BANNER_EXTENSION = null;
    private String VELOCITY_HOST_EXTENSION= null;
    private ContentletAPI conAPI = APILocator.getContentletAPI();
    private DotResourceCache resourceCache = CacheLocator.getVeloctyResourceCache();

    private static String velocityCanoncalPath;
    private static String assetCanoncalPath;
    private static String assetRealCanoncalPath;
    private static DotResourceLoader instance;

    /* (non-Javadoc)
     * @see org.apache.velocity.runtime.resource.loader.FileResourceLoader#init(org.apache.commons.collections.ExtendedProperties)
     */
    @Override
    public void init(ExtendedProperties extProps) {
    	VELOCITY_ROOT = Config.getStringProperty("VELOCITY_ROOT");
        VELOCITY_CONTAINER_EXTENSION = Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION");
        VELOCITY_FIELD_EXTENSION = Config.getStringProperty("VELOCITY_FIELD_EXTENSION");
        VELOCITY_CONTENT_EXTENSION = Config.getStringProperty("VELOCITY_CONTENT_EXTENSION");
        VELOCITY_CONTENT_MAP_EXTENSION = Config.getStringProperty("VELOCITY_CONTENT_MAP_EXTENSION");
        VELOCITY_HTMLPAGE_EXTENSION = Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION");
        VELOCITY_TEMPLATE_EXTENSION = Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION");
        VELOCITY_STRUCTURE_EXTENSION = Config.getStringProperty("VELOCITY_STRUCTURE_EXTENSION");
        VELOCITY_BANNER_EXTENSION = Config.getStringProperty("VELOCITY_BANNER_EXTENSION");
        VELOCITY_HOST_EXTENSION=Config.getStringProperty("VELOCITY_HOST_EXTENSION");

        String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
        if (velocityRootPath.startsWith("/WEB-INF")) {
            velocityRootPath = FileUtil.getRealPath(velocityRootPath);
        }

        VELOCITY_ROOT = velocityRootPath + File.separator;

        File f = new File(VELOCITY_ROOT);
        try {
            if(f.exists()){
                   velocityCanoncalPath = f.getCanonicalPath();
            }
        } catch (IOException e) {
        	Logger.fatal(this,e.getMessage(),e);
        }

        try {
            if(UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))){
                f = new File(Config.getStringProperty("ASSET_REAL_PATH"));
                if(f.exists()){
                        assetRealCanoncalPath = f.getCanonicalPath();
                }
            }
        } catch (IOException e) {
        	Logger.fatal(this,e.getMessage(),e);
        }

        try {
            if(UtilMethods.isSet(Config.getStringProperty("ASSET_PATH"))){
                f = new File(FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH")));
                if(f.exists()){
                    assetCanoncalPath = f.getCanonicalPath();
                }
            }
        } catch (IOException e) {
            Logger.fatal(this,e.getMessage(),e);
        }
        instance = this;
    }

    public DotResourceLoader() {
        super();
    }

    private boolean isACMSVelocityFile(String arg0) {

        for (int i = 0; i < velocityCMSExtenstions.length; i++) {
            if (arg0.endsWith(velocityCMSExtenstions[i])) {
                return true;
            }
        }
        return false;
    }

    public InputStream getResourceStream(String arg0) throws ResourceNotFoundException {
    	if(!UtilMethods.isSet(arg0)) {
            throw new ResourceNotFoundException("cannot find resource");
        }
        long timer = System.currentTimeMillis();
        InputStream result = null;

        synchronized (arg0.intern()) {
	        try {
	            if(!UtilMethods.isSet(arg0)) {
	               throw new ResourceNotFoundException("cannot find resource");
	            }

	            Logger.debug(this, "Thread " + Thread.currentThread().getId() + ":" + Thread.currentThread().getName() + " VelocityKey " + arg0 + " Time " + timer);

	            if (isACMSVelocityFile(arg0)) {
	            	result = new BufferedInputStream(generateStream(arg0));
	            }else{
	            	boolean serveFile = false;
	            	Logger.debug(this, "Not a CMS Velocity File : " + arg0);

	            	java.io.File f=null;
	            	String lookingFor="";
	            	if (arg0.startsWith("dynamic")) {
	            		lookingFor =ConfigUtils.getDynamicContentPath() + File.separator +  "velocity" + File.separator+arg0;

	            	} else {
	            		lookingFor = VELOCITY_ROOT + arg0;
	            	}
	            	f = new java.io.File(lookingFor);
	                if(!f.exists()){
	                    f = new java.io.File(arg0);
	                }
	                if(!f.exists()){
	                	throw new ResourceNotFoundException("cannot find resource");
	                }
	            	String canon = f.getCanonicalPath();
	            	File dynamicContent=new File(ConfigUtils.getDynamicContentPath());

	                if(assetRealCanoncalPath != null && canon.startsWith(assetRealCanoncalPath)){
	                    serveFile = true;
	                }
	                else if(velocityCanoncalPath != null && canon.startsWith(velocityCanoncalPath)){
	                    serveFile = true;
	                }
	                else if (assetCanoncalPath != null && canon.startsWith(assetCanoncalPath)){
	                    serveFile = true;
	                }
	                else if (canon.startsWith(dynamicContent.getCanonicalPath())) {
	                	serveFile =true;
	                }
	                if(!serveFile){
	                    Logger.warn(this, "POSSIBLE HACK ATTACK DotResourceLoader: " + lookingFor);
	                    throw new ResourceNotFoundException("cannot find resource");
	                }
	                result = new BufferedInputStream(new FileInputStream(f));
	            }
	        }catch (Exception e) {
	            Logger.warn(this,"Error ocurred finding resource '" + arg0 + "' exception: " + e.toString());
	            if(e instanceof ResourceNotFoundException){
	            	throw (ResourceNotFoundException)e;
	            }
	            try {
					result = new ByteArrayInputStream("".getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e1) {
					Logger.error(DotResourceLoader.class,e1.getMessage(),e1);
				}
	        }
        }
        if(result == null){
        	try {
				result = new ByteArrayInputStream("".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Logger.error(DotResourceLoader.class,e.getMessage(),e);
			}
        }

        Logger.debug(this,"Thread " + Thread.currentThread().getId() + ":" + Thread.currentThread().getName() + " VelocityKey " + arg0 + " Time " + System.currentTimeMillis() + String.format(" time consumed for resource %s: %d ms\n", arg0, System.currentTimeMillis() - timer));
        return result;
    }

    private InputStream generateStream(String arg0) throws Exception {
    	User user=APILocator.getUserAPI().getSystemUser();

        // get the identifier
        String x;
        int startSub = 0;
        int endSub = arg0.length();
        if(arg0.lastIndexOf('\\') > -1){
        	startSub = arg0.lastIndexOf("\\") + 1;
        }else if(arg0.lastIndexOf('/') > -1){
        	startSub = arg0.lastIndexOf('/') + 1;
        }
        if(arg0.lastIndexOf(".") > -1){
        	endSub = arg0.lastIndexOf(".");
        }
        x = arg0.substring(startSub, endSub);

        Logger.debug(this,"DotResourceLoader:\tInode: " + x);

        boolean preview = arg0.indexOf("working") > -1;

        InputStream result = new ByteArrayInputStream("".getBytes());
        if (arg0.endsWith(VELOCITY_CONTAINER_EXTENSION)) {

            try {
                //Integer.parseInt(x);
                Identifier identifier = APILocator.getIdentifierAPI().find(x);
                VersionableAPI versionableAPI=APILocator.getVersionableAPI();
                Container container = null;
                if (preview) {
                	container=(Container)versionableAPI.findWorkingVersion(identifier, user, true);
                } else {
                	container=(Container)versionableAPI.findLiveVersion(identifier, user, true);
                }

                Logger.debug(this,"DotResourceLoader:\tWriting out container inode = " + container.getInode());

                result = ContainerServices.buildVelocity(container, identifier, preview);
            } catch (NumberFormatException e) {
            	CacheLocator.getVeloctyResourceCache().addMiss(arg0);
                Logger.warn(this,"getResourceStream: Invalid resource path provided = " + arg0 + ", request discarded.");
                try {
    				return new ByteArrayInputStream("".getBytes("UTF-8"));
    			} catch (UnsupportedEncodingException e1) {
    				Logger.error(DotResourceLoader.class,e1.getMessage(),e1);
    			}
            }
        }else if (arg0.endsWith(VELOCITY_CONTENT_EXTENSION)) {
            String language = "";
            if (x.indexOf("_") > -1) {
                Logger.debug(this,"x=" + x);
                language = x.substring(x.indexOf("_") + 1, x.length());
                Logger.debug(this,"language=" + language);
                x = x.substring(0, x.indexOf("_"));
                Logger.debug(this,"x=" + x);
            }
            try {
                //Integer.parseInt(x);
                //Identifier identifier = (Identifier) InodeFactory.getInode(x, Identifier.class);
            	Identifier identifier = APILocator.getIdentifierAPI().find(x);
                Contentlet contentlet = null;
                if(CacheLocator.getVeloctyResourceCache().isMiss(arg0)){
                	if(LanguageWebAPI.canDefaultContentToDefaultLanguage()) {
                		 LanguageAPI langAPI = APILocator.getLanguageAPI();
                		 language = Long.toString(langAPI.getDefaultLanguage().getId());
                	} else {
                		throw new ResourceNotFoundException("Contentlet is a miss in the cache");
                	}
            	}
                
                try {
	                contentlet = conAPI.findContentletByIdentifier(identifier.getInode(), !preview,new Long(language) , APILocator.getUserAPI().getSystemUser(), true);
                } catch (DotContentletStateException e) {
                    contentlet = null;
                }
                
                if(contentlet == null || !InodeUtils.isSet(contentlet.getInode()) || contentlet.isArchived()){
                    
                    LanguageAPI langAPI = APILocator.getLanguageAPI();
                    long lid = langAPI.getDefaultLanguage().getId();
                    if(lid!=Long.parseLong(language)) {
                        Contentlet cc = conAPI.findContentletByIdentifier(identifier.getInode(), !preview,lid , APILocator.getUserAPI().getSystemUser(), true);
                        if(cc!=null && UtilMethods.isSet(cc.getInode()) 
                                 && !cc.isArchived() && LanguageWebAPI.canApplyToAllLanguages(cc)) {
                            contentlet = cc;
                        } else {
                            CacheLocator.getVeloctyResourceCache().addMiss(arg0);
                            throw new ResourceNotFoundException("Contentlet is a miss in the cache");
                        }
                    }
                }

                Logger.debug(this,"DotResourceLoader:\tWriting out contentlet inode = " + contentlet.getInode());

                result = ContentletServices.buildVelocity(contentlet, identifier, preview);
            } catch (NumberFormatException e) {
                Logger.warn(this,"getResourceStream: Invalid resource path provided = " + arg0 + ", request discarded.");
                try {
    				return new ByteArrayInputStream("".getBytes("UTF-8"));
    			} catch (UnsupportedEncodingException e1) {
    				Logger.error(DotResourceLoader.class,e1.getMessage(),e1);
    			}
            } catch (DotContentletStateException e) {
            	CacheLocator.getVeloctyResourceCache().addMiss(arg0);
                Logger.debug(this,"getResourceStream: Invalid resource path provided = " + arg0 + ", request discarded.");
                try {
    				return new ByteArrayInputStream("".getBytes("UTF-8"));
    			} catch (UnsupportedEncodingException e1) {
    				Logger.error(DotResourceLoader.class,e1.getMessage(),e1);
    			}
            }
        }else if (arg0.endsWith(VELOCITY_FIELD_EXTENSION)) {
        	//long contentletInode;
        	//long fieldInode;
        	if (x.indexOf("_") > -1) {
        		String fieldID = x.substring(x.indexOf("_") + 1, x.length());
        		String conInode = x.substring(0,x.indexOf("_"));
        		//contentletInode = Integer.parseInt(conInode);
        		//fieldInode = Integer.parseInt(fieldID);
        		result = FieldServices.buildVelocity(fieldID, conInode, preview);
        	}

        }else if (arg0.endsWith(VELOCITY_CONTENT_MAP_EXTENSION)) {
            try {
	            String language = "";
	            if (x.indexOf("_") > -1) {
	                Logger.debug(this,"x=" + x);
	                language = x.substring(x.indexOf("_") + 1, x.length());
	                Logger.debug(this,"language=" + language);
	                x = x.substring(0, x.indexOf("_"));
	                Logger.debug(this,"x=" + x);
	            }

	            Contentlet contentlet = null;
	            if(CacheLocator.getVeloctyResourceCache().isMiss(arg0)){
            		throw new ResourceNotFoundException("Contentlet is a miss in the cache");
            	}
	            
	            try {
	                contentlet = conAPI.findContentletByIdentifier(new String(x), !preview,new Long(language) , APILocator.getUserAPI().getSystemUser(), true);
	            }
	            catch(Exception ex) {
	                contentlet = null;
	            }
	            
	            if(contentlet == null || !InodeUtils.isSet(contentlet.getInode())){
                	CacheLocator.getVeloctyResourceCache().addMiss(arg0);
                	throw new ResourceNotFoundException("Contentlet is a miss in the cache");
                }

	            Logger.debug(this,"DotResourceLoader:\tWriting out contentlet inode = " + contentlet.getInode());

	            result = ContentletMapServices.buildVelocity(contentlet, preview);
            } catch (DotContentletStateException e) {
            	CacheLocator.getVeloctyResourceCache().addMiss(arg0);
                Logger.debug(this,"getResourceStream: Invalid resource path provided = " + arg0 + ", request discarded.");
                try {
    				return new ByteArrayInputStream("".getBytes("UTF-8"));
    			} catch (UnsupportedEncodingException e1) {
    				Logger.error(DotResourceLoader.class,e1.getMessage(),e1);
    			}
            }
        }else if (arg0.endsWith(VELOCITY_HTMLPAGE_EXTENSION)) {
            try {
                //Integer.parseInt(x);
                //Identifier identifier = (Identifier) InodeFactory.getInode(x, Identifier.class);
            	Identifier identifier = APILocator.getIdentifierAPI().find(x);
            	VersionableAPI versionableAPI=APILocator.getVersionableAPI();
                HTMLPage page = null;
                if (preview) {
                	page=(HTMLPage) versionableAPI.findWorkingVersion(identifier, user, true);
                } else {
                	page=(HTMLPage) versionableAPI.findLiveVersion(identifier, user, true);
                }

                Logger.debug(this,"DotResourceLoader:\tWriting out HTMLpage inode = " + page.getInode());

                if (!InodeUtils.isSet(page.getInode())) {
                    throw new ResourceNotFoundException("Page " + arg0 + "not found error 404");
                } else {
                	result = PageServices.buildStream(page, preview);
                }
            } catch (NumberFormatException e) {
                Logger.warn(this,"getResourceStream: Invalid resource path provided = " + arg0 + ", request discarded.");
                throw new ResourceNotFoundException("Invalid resource path provided = " + arg0);
            }
        }else if (arg0.endsWith(VELOCITY_HOST_EXTENSION)) {

                //Integer.parseInt(x)

                Host host= APILocator.getHostAPI().find(x, APILocator.getUserAPI().getSystemUser(), false);

                 if (!InodeUtils.isSet(host.getInode()))
                Logger.debug(this,"host not found");
                 else
                	result = HostServices.buildStream(host, preview);


        }else if (arg0.endsWith(VELOCITY_TEMPLATE_EXTENSION)) {
            try {
                //Integer.parseInt(x);
                //Identifier identifier = (Identifier) InodeFactory.getInode(x, Identifier.class);
            	Identifier identifier = APILocator.getIdentifierAPI().find(x);
            	VersionableAPI versionableAPI=APILocator.getVersionableAPI();
                Template template = null;
                if (preview) {
                	template = (Template) versionableAPI.findWorkingVersion(identifier, user, true);
                } else {
                	template = (Template) versionableAPI.findLiveVersion(identifier, user, true);
                }

                Logger.debug(this,"DotResourceLoader:\tWriting out Template inode = " + template.getInode());

                result = TemplateServices.buildVelocity(template, preview);
            } catch (NumberFormatException e) {
                Logger.warn(this,"getResourceStream: Invalid resource path provided = " + arg0 + ", request discarded.");
                throw new ResourceNotFoundException("Invalid resource path provided = " + arg0);
            }
        }else if (arg0.endsWith(VELOCITY_STRUCTURE_EXTENSION))
        {
            try
            {
                //Integer.parseInt(x);
                Structure  structure = (Structure) InodeFactory.getInode(x, Structure.class);
                result = StructureServices.buildVelocity(structure);
            }
            catch(NumberFormatException e)
            {
                Logger.warn(this,"getResourceStream: Invalid resource path provided = " + arg0 + ", request discarded.");
                throw new ResourceNotFoundException("Invalid resource path provided = " + arg0);
            }
        }else{
        	throw new ResourceNotFoundException("Unable to build the resource");
        }
//        if(UtilMethods.isSet(result)){
//        	StringBuilder sb = new StringBuilder();
//        	BufferedReader reader = new BufferedReader(new InputStreamReader(result));
//        	String line = null;
//        	try {
//        		while ((line = reader.readLine()) != null) {
//        			sb.append(line + "\n");
//        		}
//        	} catch (IOException e) {
//        		Logger.error(this , e.getMessage(),e);
//        	}
//        	result.reset();
//        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.apache.velocity.runtime.resource.loader.FileResourceLoader#getLastModified(org.apache.velocity.runtime.resource.Resource)
     */
    @Override
    public long getLastModified(Resource resource) {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.apache.velocity.runtime.resource.loader.FileResourceLoader#isSourceModified(org.apache.velocity.runtime.resource.Resource)
     */
    @Override
    public boolean isSourceModified(Resource resource) {
        return false;
    }

    public static DotResourceLoader getInstance(){
    	return instance;
    }

}