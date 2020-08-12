package com.dotcms.rendering.velocity.viewtools;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Jonathan Gamba
 * Date: 10/25/12
 */
public class DotTemplateTool implements ViewTool {

    private HttpServletRequest request;
    private User sysUser = null;

    private static Cache<String, Map<String, Object>> cache = CacheBuilder.<String, Map<String, Object>>newBuilder()
        .expireAfterWrite(Config.getLongProperty("TEMPLATE_THEME_CACHE_TTL_MILLIS", 5000), TimeUnit.MILLISECONDS)
        .build(); 
    private static Cache<String, TemplateLayout> layoutCache = CacheBuilder.<String,TemplateLayout>newBuilder()
        .expireAfterWrite(Config.getLongProperty("TEMPLATE_THEME_CACHE_TTL_MILLIS", 5000), TimeUnit.MILLISECONDS)
        .build();

    private static class DrawedBody {
        private String title;
        private String drawedBody;

        private DrawedBody(String title, String drawedBody) {
            this.title = title;
            this.drawedBody = drawedBody;
        }

        public String getTitle() {
            return title;
        }

        public String getDrawedBody() {
            return drawedBody;
        }
    }

    /**
     * @param initData the ViewContext that is automatically passed on view tool initialization, either in the request or the application
     * @return
     * @see ViewTool, ViewContext
     */
    public void init ( Object initData ) {
        ViewContext context = (ViewContext) initData;
        request = context.getRequest();

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
    public static TemplateLayout themeLayout ( final String themeInode )
            throws DotDataException, DotSecurityException {
        final User user = APILocator.getUserAPI().getSystemUser();
        return themeLayout( themeInode, user, false );
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
    public static TemplateLayout themeLayout (final String themeInode,
                                              final User user,
                                              final Boolean isPreview )
            throws DotDataException, DotSecurityException {
        final String key = themeInode + isPreview;
        if (!UtilMethods.isSet(themeInode)) {
            return null;
        }

        TemplateLayout layout = layoutCache.getIfPresent(key);
        if(layout == null) {
            layout = getLayout(themeInode, isPreview, getDrawedBody(themeInode, user));
        }

        return layout;
    }

    public static void removeFromLayoutCache(final String templateInode){
        layoutCache.invalidate(templateInode + false);
        layoutCache.invalidate(templateInode + true);
    }

    private static DrawedBody getDrawedBody(String themeInode, User user) throws DotDataException, DotSecurityException {
        final Identifier ident = APILocator.getIdentifierAPI().findFromInode(themeInode);
        final Template template = APILocator.getTemplateAPI().findWorkingTemplate(ident.getId(), user, false);

        if (!template.isDrawed()){
            throw new RuntimeException("Template with inode: " + themeInode + " is not drawed");
        }

        return new DrawedBody(template.getTitle(), template.getDrawedBody());
    }

    private DrawedBody getDrawedBody() {
        return new DrawedBody((String) request.getAttribute("title"), (String) request.getAttribute("designedBody"));
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
    public TemplateLayout themeLayout ( final String themeInode, final Boolean isPreview ) throws DotDataException, DotSecurityException {
        final String key = themeInode + isPreview;
        TemplateLayout layout = layoutCache.getIfPresent(key);

        if(layout==null) {

            final DrawedBody drawedBody;

            if (UtilMethods.isSet(themeInode)) {
                drawedBody = getDrawedBody(themeInode, sysUser);
            } else {
                drawedBody = getDrawedBody();
            }

            layout = getLayout(themeInode, isPreview, drawedBody);
        }

        return layout;
    }

    private static TemplateLayout getLayout ( final String themeInode, final Boolean isPreview,
                                              final DrawedBody drawedBody)
            throws DotDataException {

        String key = themeInode + isPreview;

        //Parse and return the layout for this template
        TemplateLayout layout;

        final String drawedBodyAsString = drawedBody.getDrawedBody();
        if (!UtilMethods.isSet(drawedBodyAsString)){
            throw new RuntimeException("Template with inode: " + themeInode + " has not drawedBody");
        }

        layout = getTemplateLayout(isPreview, drawedBodyAsString);

        layout.setTitle(drawedBody.getTitle());
        layoutCache.put(key, layout);

        return layout;
    }

    public static TemplateLayout getTemplateLayout(String drawedBodyAsString) {
        TemplateLayout layout;
        try {
            layout = getTemplateLayoutFromJSON(drawedBodyAsString);
        } catch (IOException e) {
            layout = DesignTemplateUtil.getDesignParameters(drawedBodyAsString, false);
        }
        return layout;
    }

    private static TemplateLayout getTemplateLayout(Boolean isPreview, String drawedBodyAsString) {
        TemplateLayout layout;
        try {
            layout = getTemplateLayoutFromJSON(drawedBodyAsString);
        } catch (IOException e) {
            layout = DesignTemplateUtil.getDesignParameters(drawedBodyAsString, isPreview);
        }
        return layout;
    }

    public static TemplateLayout getTemplateLayoutFromJSON(String json)  throws IOException{
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
    public static Map<String, Object> theme ( final String themeFolderInode, final String hostId )
            throws DotDataException, DotSecurityException {

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
        themeMap.put( "title", themeFolder.getName());
        cache.put(key, themeMap);
      }
        return themeMap;
    }

}