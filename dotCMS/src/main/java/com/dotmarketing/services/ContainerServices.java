package com.dotmarketing.services;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.DotResourceCache;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.runtime.resource.ResourceManager;

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

        List<ContainerStructure> csList = new ArrayList<>();
        try{
            csList = APILocator.getContainerAPI().getContainerStructures(container);
        }catch(Exception e){
            throw new DotStateException(e.getMessage());
        }

        //  let's write this puppy out to our file
        sb.append("#set ($SERVER_NAME =\"$host.getHostname()\" ) ");
        sb.append("#set ($CONTAINER_IDENTIFIER_INODE = '" ).append(identifier.getInode() ).append( "')");
        sb.append("#set ($CONTAINER_INODE = '" ).append(container.getInode() ).append( "')");
        sb.append("#set ($CONTAINER_MAX_CONTENTLETS = " ).append( container.getMaxContentlets()).append( ")");
        // commented by issue-2093
//        Structure st = CacheLocator.getContentTypeCache().getStructureByInode(container.getStructureInode());
//        sb.append("#set ($CONTAINER_STRUCTURE_NAME = \"" ).append( (UtilMethods.isSet(st.getName())?st.getName():"") ).append( "\")");
        sb.append("#set ($STATIC_CONTAINER = " ).append( !UtilMethods.isSet(container.getLuceneQuery()) ).append(")");
        sb.append("#set ($SORT_PAGE = \"" ).append( container.getSortContentletsBy() ).append( "\")");
        sb.append("#set ($containerInode = '" ).append( container.getInode() ).append( "')");

        if(EDIT_MODE) {
	        //Permissions to read/use the container in order to be able to add content to it and reorder content
	        sb.append("#set ($USE_CONTAINER_PERMISSION = $USE_CONTAINER_PERMISSION" ).append( identifier.getInode() ).append( ")");

	        //Permissions to edit the container based on write permission ).append( access to the portlet
	        sb.append("#set ($EDIT_CONTAINER_PERMISSION = $EDIT_CONTAINER_PERMISSION" ).append( identifier.getInode() ).append( ")");

	        //Permissions over the structure to add new contents
	        sb.append("#set ($ADD_CONTENT_PERMISSION = $ADD_CONTENT_PERMISSION" ).append( identifier.getInode() ).append( ")");
        }

        sb.append("#set ($CONTENTLETS = $contentletList" ).append( identifier.getInode() ).append( ")");
        sb.append("#set ($CONTAINER_NUM_CONTENTLETS = $totalSize" ).append( identifier.getInode() ).append( ")");

        sb.append("#set ($CONTAINER_NAME = \"" ).append( UtilMethods.espaceForVelocity(container.getTitle()) ).append( "\")");
        // commented by issue-2093
//        sb.append("#set ($CONTAINER_STRUCTURE_NAME = \"" ).append( UtilMethods.espaceForVelocity(st.getName()) ).append( "\")");
        if (UtilMethods.isSet(container.getNotes()))
        	sb.append("#set ($CONTAINER_NOTES = \"" ).append( UtilMethods.espaceForVelocity(container.getNotes()) ).append( "\")");
        else sb.append("#set ($CONTAINER_NOTES = \"\")");

        /*
         * isDynamic means that the content list will be pulled from lucene.
         */
        if (isDynamic) {
            String luceneQuery = container.getLuceneQuery();
            sb.append("#set ($CONTENTS_PER_PAGE = \"$CONTAINER_MAX_CONTENTLETS\")");
            sb.append("#if ($request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_per_page\"))");
            sb.append("#set ($CONTENTS_PER_PAGE = $request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_per_page\"))");
            sb.append("#end ");
            sb.append("#set ($CONTENTS_PER_PAGE = $request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_per_page\"))");
            sb.append("#end");
            sb.append("#set ($CURRENT_PAGE = \"1\")");
            sb.append("#if ($request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_page\"))");
            sb.append("#set ($CURRENT_PAGE = $request.getParameter(\"cont_" ).append( identifier.getInode() ).append( "_page\"))");
            sb.append("#end");
            sb.append("#set ($LUCENE_QUERY = \"" ).append( luceneQuery ).append( "\")");
        }

        // if the container needs to get its contentlets
        if (container.getMaxContentlets() > 0) {
            sb.append("#if($EDIT_MODE) ");


            // To edit the look, see WEB-INF/velocity/static/preview/container_controls.vtl
            sb.append("<div class='dotContainer'> ");
            sb.append("#end");

            // pre loop if it exists
            if(UtilMethods.isSet(container.getPreLoop())){
                sb.append(container.getPreLoop());
            }

            sb.append("#foreach ($contentletId in $contentletList" ).append( identifier.getInode() ).append( ")");

       		//##Checking of contentlet is parseable and not throwing errors
           	if (EDIT_MODE) {
           		  sb.append("#if($webapi.canParseContent($contentletId,true))");
           	}
           	    //sb.append("\n#if($webapi.canParseContent($contentletId,"+EDIT_MODE+")) ");
           	    sb.append("#set($_show_working_=false) ");

           	    // if timemachine future enabled
           	    sb.append("#if($UtilMethods.isSet($request.getSession(false)) && $request.session.getAttribute(\"tm_date\"))");
           	    sb.append("#set($_tmdate=$date.toDate($webapi.parseLong($request.session.getAttribute(\"tm_date\")))) ");
           	    sb.append("#set($_ident=$webapi.findIdentifierById($contentletId)) ");

           	    // if the content has expired we rewrite the identifier so it isn't loaded
           	    sb.append("#if($UtilMethods.isSet($_ident.sysExpireDate) && $_tmdate.after($_ident.sysExpireDate))");
           	    sb.append("#set($contentletId='') ");
           	    sb.append("#end");

           	    // if the content should be published then force to show the working version
           	    sb.append("#if($UtilMethods.isSet($_ident.sysPublishDate) && $_tmdate.after($_ident.sysPublishDate))");
           	    sb.append("#set($_show_working_=true) ");
           	    sb.append("#end");

           	    sb.append("#if(! $webapi.contentHasLiveVersion($contentletId) && ! $_show_working_) ")
           	      .append("#set($contentletId='')") // working contentlet still not published
           	      .append("#end");

           	    sb.append("#end");

           		sb.append("#set($CONTENT_INODE = '')");
           		sb.append("#if($contentletId != '') ");
           	    sb.append("#contentDetail($contentletId) ");
           	    sb.append("#end");
           	    sb.append("#if($CONTENT_INODE != '')");

               	if (!EDIT_MODE) {
                    sb.append("#set($_hasPermissionToViewContent = $contents.doesUserHasPermission($CONTENT_INODE, 1, $user, true))");
                   	//##Checking permission to see content
               		sb.append("#if($_hasPermissionToViewContent)");
               	}


                String code =  "";
                

                //### HEADER ###
                String startTag = "${contentletStart}";
                if(!code.contains(startTag)) {
                	sb.append("#if($EDIT_MODE)");
                    	sb.append("<div class=\"dotContentlet\">");
                    	sb.append(" ");
                    	//An empty div is added here because in Internet Explorer, there is a styling issue
                        //http://jira.dotmarketing.net/browse/DOTCMS-1974
                    	sb.append("<div>");
                    sb.append(" #end ");
                }
                else {
                	String headerString = "#if($EDIT_MODE)" +
                			"<div class=\"dotContentlet\">" + "<div>" +
                			"#end ";
                	code = code.replace(startTag,headerString);
                }
                //### END HEADER ###

                //### BODY ###
                String endTag = "${contentletEnd}";
                boolean containsEndTag = code.contains(endTag);
                if(containsEndTag)
                {
                	String footerString = "#if($EDIT_MODE && ${contentletId.indexOf(\".structure\")}==-1) " +
                			"$velutil.mergeTemplate('static/preview_mode/content_controls.vtl') " +
                			" #end " +
                			"#if($EDIT_MODE) " +
                			"<div class=\"dotClear\"></div></div>" +
                			"#end ";
                	code = code.replace(endTag,footerString);
                }

                sb.append("#if($isWidget == true)");
                	sb.append("$widgetCode");
                sb.append(" #else ");
/*
                      for (ContainerStructure cs : csList) {                	sb.append("#if($ContentletStructure ==\"" + cs.getStructureId() + "\")" );
                	sb.append(cs.getCode() );
                	sb.append("#end");
                      }
                 */
                for (int i=0;i<csList.size();i++) {
                    ContainerStructure cs = csList.get(i);
                    String ifelse = (i==0) ? "if" : "elseif";
                    sb.append("#"+ifelse+ "($ContentletStructure ==\"" + cs.getStructureId() + "\")" );
                    sb.append(cs.getCode());
                }
                if(csList.size()>0){
                    sb.append("#end");
                }
                sb.append(" #end ");
              //The empty div added for styling issue in Internet Explorer is closed here
                //http://jira.dotmarketing.net/browse/DOTCMS-1974
            	sb.append("#if($EDIT_MODE)");
                sb.append("</div>");
                sb.append("#end ");
                //### END BODY ###

                //### FOOTER ###

                if(!containsEndTag)
                {
                	sb.append("#if($EDIT_MODE && ${contentletId.indexOf(\".structure\")}==-1)");
                	    sb.append("#contentDetail($contentletId)");
                		sb.append("$velutil.mergeTemplate('static/preview_mode/content_controls.vtl')");
                	sb.append("#end ");
                	sb.append("#if($EDIT_MODE) ");
                		sb.append("<div class=\"dotClear\"></div></div>");
                	sb.append("#end ");
                }
                //### END FOOTER ###

                if (!EDIT_MODE) {
	                //##End of checking permission to see content
	       			sb.append("#end ");
                }
                //##Ends the inner canParse call
                sb.append("#end ");
       		//##Case the contentlet is not parseable and throwing errors
            if (EDIT_MODE) {
                sb.append("#else ");
                	sb.append("#set($CONTENT_INODE =\"$webapi.getContentInode($contentletId)\")");
                	sb.append("#set($EDIT_CONTENT_PERMISSION =\"$webapi.getContentPermissions($contentletId)\")");

                	sb.append("<div class=\"dotContentlet\">");
                    sb.append("	Content Parse Error. Check your Content Code. ");
                    sb.append("$velutil.mergeTemplate('static/preview_mode/content_controls.vtl')");
                    sb.append("<div class=\"dotClear\"></div></div>");
                sb.append("#end ");

            }


       		//##End of foreach loop
            sb.append("#end ");

            // post loop if it exists

            if(UtilMethods.isSet(container.getPostLoop())){
                sb.append(container.getPostLoop());
            }
            //close our container preview mode div
            sb.append("#if($EDIT_MODE) ");
            	sb.append("$velutil.mergeTemplate('static/preview_mode/container_controls.vtl')");
                sb.append("</div>");
            sb.append("#end ");

        }
        else {

            sb.append(container.getCode());
        }

        final String folderPath = (!EDIT_MODE) ? "live" + File.separator: "working" + File.separator;
        final String filePath = folderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION");
        final String VelocityFilePath = ConfigUtils.getDynamicVelocityPath() + File.separator + filePath;

        if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
            try (
                    BufferedOutputStream tmpOut = new BufferedOutputStream(Files.newOutputStream(Paths.get(VelocityFilePath)));
                    OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration())
            ){
                out.write(sb.toString());
                out.flush();
            } catch (Exception e) {
                Logger.error(ContentletServices.class, e.toString(), e);
            }

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
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;
        String filePath = folderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION");
        java.io.File f  = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
    }
}