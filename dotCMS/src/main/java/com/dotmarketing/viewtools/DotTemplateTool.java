package com.dotmarketing.viewtools;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.transform.JsonTransformer;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.liferay.portal.model.User;

/**
 * Created by Jonathan Gamba
 * Date: 10/25/12
 */
public class DotTemplateTool implements ViewTool {

    private static HttpServletRequest request;
    Context ctx;
    private static User sysUser = null;
    private static Cache<String, Map<String, Object>> cache = CacheBuilder.<String, Map<String, Object>>newBuilder()
        .expireAfterWrite(Config.getLongProperty("TEMPLATE_THEME_CACHE_TTL_MILLIS", 5000), TimeUnit.MILLISECONDS)
        .build(); 
    private static Cache<String, TemplateLayout> layoutCache = CacheBuilder.<String,TemplateLayout>newBuilder()
        .expireAfterWrite(Config.getLongProperty("TEMPLATE_THEME_CACHE_TTL_MILLIS", 5000), TimeUnit.MILLISECONDS)
        .build(); 
    /**
     * @param initData the ViewContext that is automatically passed on view tool initialization, either in the request or the application
     * @return
     * @see ViewTool, ViewContext
     */
    public void init ( Object initData ) {
        ViewContext context = (ViewContext) initData;
        request = context.getRequest();
        ctx = context.getVelocityContext();
        try {
			sysUser = APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			Logger.error(DotTemplateTool.class,e.getMessage(),e);
		}
    }

    /**
     * Given a theme id we will parse it and return the Layout for the given template
     *
     * @param themeInode
     * @return
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    public static TemplateLayout themeLayout ( String themeInode ) throws DotDataException, DotSecurityException {
        return themeLayout( themeInode, false );
    }

    /**
     * Given a theme id we will parse it and return the Layout for the given template
     *
     * @param themeInode
     * @param isPreview
     * @return
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    public static TemplateLayout themeLayout ( final String themeInode, final Boolean isPreview ) throws DotDataException, DotSecurityException {
        String key = themeInode + isPreview;
        TemplateLayout layout = layoutCache.getIfPresent(key);

        if(layout==null) {
            String title = null;
            String drawedBody;
            if (UtilMethods.isSet(themeInode)) {
                Identifier ident = APILocator.getIdentifierAPI().findFromInode(themeInode);

                Template template = APILocator.getTemplateAPI().findWorkingTemplate(ident.getId(), sysUser, false);

                if (!template.getInode().equals(themeInode)) {
                    template = (Template) InodeFactory.getInode(themeInode, Template.class);
                }

                drawedBody = template.getDrawedBody();
                title = template.getTitle();
            } else {
                drawedBody = (String) request.getAttribute("designedBody");
                if (request.getAttribute("title") != null) {
                    title = (String) request.getAttribute("title");
                }
            }

            //Parse and return the layout for this template

            try {
                layout = getTemplateLayoutFromJSON(drawedBody);
            } catch (IOException e) {
                layout = DesignTemplateUtil.getDesignParameters(drawedBody, isPreview);
            }

            layout.setTitle(title);
            layoutCache.put(key, layout);
        }

        return layout;
    }

    private static TemplateLayout getTemplateLayoutFromJSON(String json)  throws IOException{
        return JsonTransformer.mapper.readValue(json, TemplateLayout.class);

    }

    /**
     * Method that will create a map of required data for the Layout template, basically paths
     * where the different elements of the theme can be found.
     *
     * @param themeFolderInode
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static Map<String, Object> theme ( String themeFolderInode, String hostId ) throws DotDataException, DotSecurityException {

        //Get the theme folder
        Folder themeFolder = APILocator.getFolderAPI().find( themeFolderInode, APILocator.getUserAPI().getSystemUser(), false );
        return setThemeData( themeFolder, hostId );
    }

    /**
     * Method that will create a map of required data for the Layout template, basically paths
     * where the different elements of the theme can be found.
     *
     * @param themeFolderPath
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static Map<String, Object> themeByPath ( String themeFolderPath, String hostId ) throws DotDataException, DotSecurityException {

    	
    	
    	if(themeFolderPath ==null ){
    		return null;
    	}
    	
    	
    	// get theme host
    	if(themeFolderPath.startsWith("//")){
    		String[] uriArray = themeFolderPath.split("/");
    		String hostName = uriArray[2];
    		
    		hostId = APILocator.getHostAPI().resolveHostName(hostName, APILocator.getUserAPI().getSystemUser(), true).getIdentifier();
    		
    		java.io.StringWriter sw = new java.io.StringWriter();
    		
    		for(int i= 3;i< uriArray.length;i++){
    			sw.append("/");
    			sw.append(uriArray[i]);
    			
    		}
    		themeFolderPath = sw.toString();
    		
    	}
    	
    	
    	
        //Get the theme folder
        Folder themeFolder = APILocator.getFolderAPI().findFolderByPath( themeFolderPath, hostId, APILocator.getUserAPI().getSystemUser(), false );
        
        
        
        
        
        
        return setThemeData( themeFolder, hostId );
    }

    /**
     * Method that will create a map of required data for the Layout template, basically paths
     * where the different elements of the theme can be found.
     *
     * @param themeFolder
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    
    private static Map<String, Object> setThemeData ( Folder themeFolder, String hostId ) throws DotDataException, DotSecurityException {

      String key = "themeMap" + themeFolder.getIdentifier();
      Map<String, Object> themeMap = cache.getIfPresent(key);
      if(themeMap==null){
        themeMap = new HashMap<String, Object>();
        //Get the theme files
        List<FileAsset> themeFiles = APILocator.getFileAssetAPI().findFileAssetsByFolder( themeFolder, APILocator.getUserAPI().getSystemUser(), false );

        //We need to verify if we have the template.vtl file
        FileAsset themeTemplate = null;
        Boolean haveHtmlHead = false;
        for ( FileAsset themeFile : themeFiles ) {
            if ( Template.THEME_TEMPLATE.equals( themeFile.getFileName() ) ) {
                themeTemplate = themeFile;
            } else if ( Template.THEME_HTML_HEAD.equals( themeFile.getFileName() ) ) {
                haveHtmlHead = true;
            }
        }

        //Getting the theme path
        String themePath;
        if ( themeFolder.getHostId().equals( hostId ) ) {
            themePath = Template.THEMES_PATH + themeFolder.getName() + "/";
        } else {
            Host themeHost = APILocator.getHostAPI().find( themeFolder.getHostId(), APILocator.getUserAPI().getSystemUser(), false );
            themePath = "//" + themeHost.getHostname() + Template.THEMES_PATH + themeFolder.getName() + "/";
        }

        //Getting the template.vtl file path
        String themeTemplatePath;
        if ( UtilMethods.isSet( themeTemplate ) && InodeUtils.isSet( themeTemplate.getInode() ) ) {
            themeTemplatePath = themeTemplate.getFileAsset().getPath();
        } else {//If the theme doesn't provide a template.vtl file lest use ours
            themeTemplatePath = "static/template/" + Template.THEME_TEMPLATE;
        }

        //Setting required theme data for the Layout template
        
        themeMap.put( "path", themePath );
        themeMap.put( "templatePath", themeTemplatePath );
        themeMap.put( "htmlHead", haveHtmlHead );
        cache.put(key, themeMap);
      }
        return themeMap;
    }

}