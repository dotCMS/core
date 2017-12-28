package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author will
 */
public class ContainerLoader implements DotLoader {

public static final String SHOW_PRE_POST_LOOP="SHOW_PRE_POST_LOOP";
    @Override
    public InputStream writeObject(String id1, String id2, PageMode mode, String language, final String filePath)
            throws DotDataException, DotSecurityException {

        VersionableAPI versionableAPI = APILocator.getVersionableAPI();
        Container container = null;
        if (mode.showLive) {
            container = (Container) versionableAPI.findLiveVersion(id1, sysUser(), true);
        } else {
            container = (Container) versionableAPI.findWorkingVersion(id1, sysUser(), true);
        }

        Logger.debug(this, "DotResourceLoader:\tWriting out container inode = " + container.getInode());

        return this.buildVelocity(container, id2, mode, filePath);
    }


    @Override
    public void invalidate(Object obj, PageMode mode) {
        Container container = (Container) obj;
        String folderPath = mode.name() + java.io.File.separator;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;
        String filePath = folderPath + container.getIdentifier() + "." + VelocityType.CONTAINER.fileExtension;
        java.io.File f = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath);
    }

    private InputStream buildVelocity(Container container, String uuid, PageMode mode, String filePath) throws DotDataException, DotSecurityException {

        ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        StringBuilder sb = new StringBuilder();


        List<ContainerStructure> csList = APILocator.getContainerAPI()
                .getContainerStructures(container);


        // let's write this puppy out to our file
        sb.append("#set ($SERVER_NAME =$host.getHostname() )");
        sb.append("#set ($CONTAINER_IDENTIFIER_INODE = '")
            .append(container.getIdentifier())
            .append("')");
        sb.append("#set ($CONTAINER_UNIQUE_ID = '")
            .append(uuid)
            .append("')");
        sb.append("#set ($CONTAINER_INODE = '")
            .append(container.getInode())
            .append("')");
        sb.append("#set ($CONTAINER_MAX_CONTENTLETS = ")
            .append(container.getMaxContentlets())
            .append(")");
        sb.append("#set ($containerInode = '")
            .append(container.getInode())
            .append("')");

        sb.append("#set ($CONTENTLETS = $contentletList")
            .append(container.getIdentifier())
            .append(uuid)
            .append(")");
        sb.append("#set ($CONTAINER_NUM_CONTENTLETS = $totalSize")
            .append(container.getIdentifier())
            .append(uuid)
            .append(")");

        sb.append("#if(!$CONTAINER_NUM_CONTENTLETS)")
            .append("#set($CONTAINER_NUM_CONTENTLETS = 0)")
            .append("#end");



        sb.append("#set ($CONTAINER_NAME = \"")
            .append(UtilMethods.espaceForVelocity(container.getTitle()))
            .append("\")");

        if (UtilMethods.isSet(container.getNotes())) {
            sb.append("#set ($CONTAINER_NOTES = \"")
                .append(UtilMethods.espaceForVelocity(container.getNotes()))
                .append("\")");
        } else {
            sb.append("#set ($CONTAINER_NOTES = \"\")");
        }


        if (mode == PageMode.EDIT_MODE) {
            StringWriter editWrapperDiv = new StringWriter();
            editWrapperDiv.append("<div")
                .append(" data-dot-object=")
                .append("\"container\"")
                .append(" data-dot-inode=")
                .append("\"" + container.getInode() + "\"")
                .append(" data-dot-identifier=")
                .append("\"" + container.getIdentifier() + "\"")
                .append(" data-dot-uuid=")
                .append("\"" + uuid + "\"")
                .append(" data-max-contentlets=")
                .append("\"" + container.getMaxContentlets() + "\"")
                .append(" data-dot-accept-types=")
                .append("\"");
            Iterator<ContainerStructure> it= csList.iterator();
            while (it.hasNext()) {
                ContainerStructure struct = it.next();
                try {
                    ContentType t = typeAPI.find(struct.getStructureId());
                    editWrapperDiv.append(t.variable());
                    if(it.hasNext()) {
                        editWrapperDiv.append(",");
                    }
                } catch (DotDataException | DotSecurityException e) {
                    Logger.warn(this.getClass(), "unable to find content type:" + struct);
                }
            }
            editWrapperDiv.append("\">");
            sb.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
            sb.append(editWrapperDiv);
            sb.append("#end");

        }



        // if the container needs to get its contentlets
        if (container.getMaxContentlets() > 0) {


            // pre loop if it exists
            if (UtilMethods.isSet(container.getPreLoop())) {
                sb.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                sb.append(container.getPreLoop());
                sb.append("#end");
            }
            // sb.append("$contentletList" + identifier.getId() + uuid + "<br>");

            // START CONTENT LOOP
            sb.append("#foreach ($contentletId in $contentletList")
                .append(container.getIdentifier())
                .append(uuid)
                .append(")");



            // sb.append("\n#if($webapi.canParseContent($contentletId,"+EDIT_MODE+")) ");
            sb.append("#set($_show_working_=false)");

            // if timemachine future enabled
            sb.append("#if($UtilMethods.isSet($request.getSession(false)) && $request.session.getAttribute(\"tm_date\"))");
            sb.append("#set($_tmdate=$date.toDate($webapi.parseLong($request.session.getAttribute(\"tm_date\"))))");
            sb.append("#set($_ident=$webapi.findIdentifierById($contentletId))");

            // if the content has expired we rewrite the identifier so it isn't loaded
            sb.append("#if($UtilMethods.isSet($_ident.sysExpireDate) && $_tmdate.after($_ident.sysExpireDate))");
            sb.append("#set($contentletId='')");
            sb.append("#end");

            // if the content should be published then force to show the working version
            sb.append("#if($UtilMethods.isSet($_ident.sysPublishDate) && $_tmdate.after($_ident.sysPublishDate))");
            sb.append("#set($_show_working_=true)");
            sb.append("#end");

            sb.append("#if(! $webapi.contentHasLiveVersion($contentletId) && ! $_show_working_)")
                .append("#set($contentletId='')") // working contentlet still not published
                .append("#end");
            sb.append("#end");

            sb.append("#set($CONTENT_INODE = '')");
            sb.append("#if($contentletId != '')");
            sb.append("#contentDetail($contentletId)");
            sb.append("#end");

            if (mode == PageMode.EDIT_MODE) {

                sb.append("<div")
                    .append(" data-dot-object=")
                    .append("\"contentlet\"")
                    .append(" data-dot-inode=")
                    .append("\"$CONTENT_INODE\"")
                    .append(" data-dot-identifier=")
                    .append("\"$IDENTIFIER_INODE\"")
                    .append(" data-dot-type=")
                    .append("\"$CONTENT_TYPE\"")
                    .append(" data-dot-basetype=")
                    .append("\"$CONTENT_BASE_TYPE\"")
                    .append(" data-dot-lang=")
                    .append("\"$CONTENT_LANGUAGE\"")
                    .append(">");


            }
            // ##Checking permission to see content
            if (mode.showLive) {
                sb.append("#if($contents.doesUserHasPermission($CONTENT_INODE, 1, $user, true))");
            }

            // ### START BODY ###
            sb.append("#if($isWidget)");
                sb.append("$widgetCode");
            sb.append("#else");

                for (int i = 0; i < csList.size(); i++) {
                    ContainerStructure cs = csList.get(i);
                    String ifelse = (i == 0) ? "if" : "elseif";
                    sb.append("#" + ifelse + "($ContentletStructure ==\"" + cs.getStructureId() + "\")");
                    sb.append(cs.getCode());
                }
                if (csList.size() > 0) {
                    sb.append("#end");
                }
                // ### END BODY ###
                sb.append("#end");
            
               if (mode.showLive) {
                    sb.append("#end");
                }
     
    
               // end content dot-data-content
               if (mode == PageMode.EDIT_MODE) {
                   sb.append("</div>");
               }
                // ##End of foreach loop
                sb.append("#end");
                
            // end content dot-data-container
            if (mode == PageMode.EDIT_MODE) {
                sb.append("</div>");
            }


            // post loop if it exists
            if (UtilMethods.isSet(container.getPostLoop())) {
                sb.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                sb.append(container.getPostLoop());
                sb.append("#end ");
            }

        } else {

            sb.append(container.getCode());
        }



        return writeOutVelocity(filePath, sb.toString());
    }






}
