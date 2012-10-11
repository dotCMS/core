package com.dotmarketing.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;

/**
 * @author will
 */
public class ContainerServices {

    public static void invalidate(Container container) throws DotStateException, DotDataException {

        Identifier identifier = APILocator.getIdentifierAPI().find(container);
        invalidate(container, identifier, false);
        invalidate(container, identifier, true);

    }

    public static void invalidate(Container container, boolean EDIT_MODE) throws DotStateException, DotDataException {

        Identifier identifier = APILocator.getIdentifierAPI().find(container);
        invalidate(container, identifier, EDIT_MODE);

    }
    
    public static InputStream buildVelocity(Container container, Identifier identifier, boolean EDIT_MODE) {

    	InputStream result;
    	StringBuilder sb = new StringBuilder();
    	
        boolean isDynamic = UtilMethods.isSet(container.getLuceneQuery());
         
        //  let's write this puppy out to our file
        sb.append("#set ($SERVER_NAME =\"$host.getHostname()\" )\n");
        sb.append("#set ($CONTAINER_IDENTIFIER_INODE = '" ).append(identifier.getInode() ).append( "')\n");
        sb.append("#set ($CONTAINER_INODE = '" ).append(container.getInode() ).append( "')\n");
        sb.append("#set ($CONTAINER_MAX_CONTENTLETS = " ).append( container.getMaxContentlets()).append( ")\n");
        Structure st = (Structure) InodeFactory.getInode(container.getStructureInode(), Structure.class);
        sb.append("#set ($CONTAINER_STRUCTURE_NAME = \"" ).append( (UtilMethods.isSet(st.getName())?st.getName():"") ).append( "\")\n");
        sb.append("#set ($STATIC_CONTAINER = " ).append( !UtilMethods.isSet(container.getLuceneQuery()) ).append(")\n");
        sb.append("#set ($SORT_PAGE = \"" ).append( container.getSortContentletsBy() ).append( "\")\n");
        sb.append("#set ($containerInode = '" ).append( container.getInode() ).append( "')\n");

        if(EDIT_MODE) {
	        //Permissions to read/use the container in order to be able to add content to it and reorder content
	        sb.append("#set ($USE_CONTAINER_PERMISSION = $USE_CONTAINER_PERMISSION" ).append( identifier.getInode() ).append( ")\n");

	        //Permissions to edit the container based on write permission ).append( access to the portlet
	        sb.append("#set ($EDIT_CONTAINER_PERMISSION = $EDIT_CONTAINER_PERMISSION" ).append( identifier.getInode() ).append( ")\n");
	
	        //Permissions over the structure to add new contents
	        sb.append("#set ($ADD_CONTENT_PERMISSION = $ADD_CONTENT_PERMISSION" ).append( identifier.getInode() ).append( ")\n");
        }
        
        sb.append("#set ($CONTENTLETS = $contentletList" ).append( identifier.getInode() ).append( ")\n");
        sb.append("#set ($CONTAINER_NUM_CONTENTLETS = $totalSize" ).append( identifier.getInode() ).append( ")\n");
        
        sb.append("#set ($CONTAINER_NAME = \"" ).append( UtilMethods.espaceForVelocity(container.getTitle()) ).append( "\")\n");
        sb.append("#set ($CONTAINER_STRUCTURE_NAME = \"" ).append( UtilMethods.espaceForVelocity(st.getName()) ).append( "\")\n");
        if (UtilMethods.isSet(container.getNotes())) 
        	sb.append("#set ($CONTAINER_NOTES = \"" ).append( UtilMethods.espaceForVelocity(container.getNotes()) ).append( "\")\n");
        else sb.append("#set ($CONTAINER_NOTES = \"\")\n");
        
        /*
         * isDynamic means that the content list will be pulled from lucene.
         */
        if (isDynamic) {
            String luceneQuery = container.getLuceneQuery();
            sb.append("#set ($CONTENTS_PER_PAGE = \"$CONTAINER_MAX_CONTENTLETS\")\n");
            sb.append("#if ($request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_per_page\"))\n");
            sb.append("     #set ($CONTENTS_PER_PAGE = $request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_per_page\"))\n");
            sb.append("#end\n");
            sb.append("#set ($CURRENT_PAGE = \"1\")\n");
            sb.append("#if ($request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_page\"))\n");
            sb.append("     #set ($CURRENT_PAGE = $request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_page\"))\n");
            sb.append("#end\n");
            sb.append("#set ($LUCENE_QUERY = \"" ).append( luceneQuery ).append( "\")\n");
        }
        
        // if the container needs to get its contentlets
        if (container.getMaxContentlets() > 0) {
            sb.append("#if($EDIT_MODE)\n");
            
            
            // To edit the look, see WEB-INF/velocity/static/preview/container_controls.vtl
            sb.append("<div class='dotContainer'>\n");
            sb.append("#end\n");
            
            // pre loop if it exists
            if(UtilMethods.isSet(container.getPreLoop())){
                sb.append(container.getPreLoop());
            }
            
            //let's do the search of contentlets using lucene query 
            if (isDynamic) {
               Structure containerStructure = (Structure) InodeFactory.getInode(container.getStructureInode(), Structure.class);

                sb.append("#set ($contentletResultsMap" ).append( identifier.getInode() ).append( 
                        " = $contents.searchWithLuceneQuery(\"").append( containerStructure.getInode() ).append("\", " ).append(
                                "\"$LUCENE_QUERY\", " ).append(
                                "\"$SORT_PAGE\", " ).append(
                                "$CURRENT_PAGE, $CONTENTS_PER_PAGE))\n");
                sb.append("#set ($contentletList" ).append( identifier.getInode() ).append( 
                        " = $contents.getContentIdentifiersFromLuceneHits($contentletResultsMap" ).append( identifier.getInode() ).append( ".get(\"assets\")))\n");
                
                sb.append("#set ($HAS_NEXT_PAGE = $contentletResultsMap" ).append( identifier.getInode() ).append( ".get(\"has_next_page\"))\n");
                sb.append("#set ($HAS_PREVIOUS_PAGE = $contentletResultsMap" ).append( identifier.getInode() ).append( ".get(\"has_previous_page\"))\n");
                sb.append("#set ($TOTAL_CONTENTS = $contentletResultsMap" ).append( identifier.getInode() ).append( ".get(\"total_records_int\"))\n");
                sb.append("#set ($TOTAL_PAGES = $contentletResultsMap" ).append( identifier.getInode() ).append( ".get(\"total_pages_int\"))\n");
                sb.append("#set ($CONTENTLETS = $contentletList" ).append( identifier.getInode() ).append( ")\n");
                sb.append("#set ($CONTAINER_NUM_CONTENTLETS = $totalSize" ).append( identifier.getInode() ).append( ")\n");
            }
                        
            sb.append("\n#foreach ($contentletId in $contentletList" ).append( identifier.getInode() ).append( ")\n");            
            
       		//##Checking of contentlet is parseable and not throwing errors
           	if (EDIT_MODE) {
           		  sb.append("\n#if($webapi.canParseContent($contentletId,true))\n");
           	}
           	    //sb.append("\n#if($webapi.canParseContent($contentletId,"+EDIT_MODE+"))\n");
           	
           		sb.append("\n#set($CONTENT_INODE = '')\n");
           	    sb.append("\n#getContentDetail($contentletId)\n");
           	    sb.append("\n#if($CONTENT_INODE != '')\n");

               	if (!EDIT_MODE) {
                    sb.append("\n#set($_hasPermissionToViewContent = $contents.doesUserHasPermission($CONTENT_INODE, 1, $user, true))\n");
                   	//##Checking permission to see content
               		sb.append("\n#if($_hasPermissionToViewContent)\n");
               	} 
                
                String code = container.getCode();
                
                //### HEADER ###
                String startTag = "${contentletStart}";
                if(!code.contains(startTag))
                {
                	sb.append("#if($EDIT_MODE)\n");
                    	sb.append("<div class=\"dotContentlet\">");
                    	sb.append("\n");
                    	//An empty div is added here because in Internet Explorer, there is a styling issue
                        //http://jira.dotmarketing.net/browse/DOTCMS-1974
                    	sb.append("<div>\n");
                    sb.append("#end\n");
                }
                else
                {
                	String headerString = "#if($EDIT_MODE)\n" +
                			"<div class=\"dotContentlet\">\n" + "<div>" +
                			"#end\n";
                	code = code.replace(startTag,headerString);
                }
                //### END HEADER ###
                
                //### BODY ###   
                String endTag = "${contentletEnd}";
                boolean containsEndTag = code.contains(endTag);
                if(containsEndTag)
                {
                	String footerString = "#if($EDIT_MODE && ${contentletId.indexOf(\".structure\")}==-1)\n" +
                			"$velutil.mergeTemplate('static/preview_mode/content_controls.vtl')\n" +
                			"#end\n" +
                			"#if($EDIT_MODE)\n" +
                			"<div class=\"dotClear\"></div></div>" +
                			"#end\n";                		
                	code = code.replace(endTag,footerString);
                }               
                
                sb.append("#if($isWidget == true)\n");
                	sb.append("$widgetCode\n");
                sb.append("#else\n");
                	sb.append(code ).append( "\n");
                sb.append("#end");
              //The empty div added for styling issue in Internet Explorer is closed here
                //http://jira.dotmarketing.net/browse/DOTCMS-1974
            	sb.append("#if($EDIT_MODE)");
                sb.append("\n</div>\n");
                sb.append("#end");
                //### END BODY ###
                
                //### FOOTER ###
                
                if(!containsEndTag)
                {
                	sb.append("#if($EDIT_MODE && ${contentletId.indexOf(\".structure\")}==-1)\n");
                	    sb.append("#getContentDetail($contentletId)\n");
                		sb.append("$velutil.mergeTemplate('static/preview_mode/content_controls.vtl')\n");
                	sb.append("#end\n");
                	sb.append("#if($EDIT_MODE)\n");
                		sb.append("<div class=\"dotClear\"></div></div> ");
                	sb.append("#end\n");
                }               
                //### END FOOTER ###                
                
                if (!EDIT_MODE) {
	                //##End of checking permission to see content
	       			sb.append("\n#end\n");
                }
                //##Ends the inner canParse call 
                sb.append("\n#end\n");
       		//##Case the contentlet is not parseable and throwing errors
            if (EDIT_MODE) {
                sb.append("\n#else\n");
                	sb.append("#set($CONTENT_INODE =\"$webapi.getContentInode($contentletId)\")");
                	sb.append("#set($EDIT_CONTENT_PERMISSION =\"$webapi.getContentPermissions($contentletId)\")");
               	
                	sb.append("<div class=\"dotContentlet\">");
                    sb.append("	Content Parse Error. Check your Content Code. ");
                    sb.append("$velutil.mergeTemplate('static/preview_mode/content_controls.vtl')");
                    sb.append("<div class=\"dotClear\"></div></div> ");
                sb.append("\n#end\n");
                
            }
       			
       		//##End of foreach loop
            sb.append("\n#end\n");
            
            // post loop if it exists
           
            if(UtilMethods.isSet(container.getPostLoop())){
                sb.append(container.getPostLoop());
            }
            //close our container preview mode div
            sb.append("#if($EDIT_MODE)\n");
            	sb.append("$velutil.mergeTemplate('static/preview_mode/container_controls.vtl')");
                sb.append("</div>");
            sb.append("#end\n");
            
        }
        else {

            sb.append(container.getCode());
        }
  
        try {
            String folderPath = (!EDIT_MODE) ? "live" + File.separator: "working" + File.separator;
            String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
            if (velocityRootPath.startsWith("/WEB-INF")) {
                velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
            }
            velocityRootPath += File.separator;
            String filePath = folderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION");

            if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
            	java.io.BufferedOutputStream tmpOut = new java.io.BufferedOutputStream(new java.io.FileOutputStream(new java.io.File(ConfigUtils.getDynamicVelocityPath()+File.separator + filePath)));
	            //Specify a proper character encoding
	            OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());
	            out.write(sb.toString());
	            out.flush();
	            out.close();
	            tmpOut.close();
            }
        } catch (Exception e) {
            Logger.error(ContentletServices.class, e.toString(), e);
        }
        
        try {
			result = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(sb.toString().getBytes());
			Logger.error(ContainerServices.class,e1.getMessage(), e1);
		}
        return result;
    }
    
    public static void invalidate(Container container, Identifier identifier, boolean EDIT_MODE) {
    	removeContainerFile(container, identifier, EDIT_MODE);
    }
    
    public static void unpublishContainerFile(Container container) throws DotStateException, DotDataException {

        Identifier identifier = APILocator.getIdentifierAPI().find(container);
        removeContainerFile(container, identifier, false);
    }
    
    public static void removeContainerFile(Container container, boolean EDIT_MODE) throws DotStateException, DotDataException {

        Identifier identifier = APILocator.getIdentifierAPI().find(container);
        removeContainerFile(container, identifier, EDIT_MODE);
    }
    
    public static void removeContainerFile (Container container, Identifier identifier, boolean EDIT_MODE) {
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
        String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
        if (velocityRootPath.startsWith("/WEB-INF")) {
            velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
        }
        velocityRootPath += java.io.File.separator;
        String filePath = folderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION");
        java.io.File f  = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
    }    
}