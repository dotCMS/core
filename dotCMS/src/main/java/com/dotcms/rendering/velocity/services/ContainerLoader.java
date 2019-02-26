package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerExceptionNotifier;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import org.apache.velocity.runtime.resource.ResourceManager;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author will
 */
public class ContainerLoader implements DotLoader {

    public static final String SHOW_PRE_POST_LOOP="SHOW_PRE_POST_LOOP";

    @Override
    public InputStream writeObject(final VelocityResourceKey key)
            throws DotDataException, DotSecurityException {

        final ContainerFinderStrategyResolver resolver   =
                ContainerFinderStrategyResolver.getInstance();
        final Optional<ContainerFinderStrategy> strategy =
                resolver.get(key);

        try {
            final Container container = strategy.isPresent() ?
                    strategy.get().apply(key) : resolver.getDefaultStrategy().apply(key);

            if (null == container) {

                final DotStateException dotStateException = new DotStateException("Cannot find container for : " + key);
                new ContainerExceptionNotifier(dotStateException, UtilMethods.isSet(key.id1) ? key.id1 : key.path).notifyUser();
                throw new DotStateException("cannot find container for : " + key);
            }

            Logger.debug(this, "DotResourceLoader:\tWriting out container inode = " + container.getInode());

            return this.buildVelocity(container, key.id2, key.mode, key.path);
        } catch (DotStateException e) {

            if (ExceptionUtil.causedBy(e, NotFoundInDbException.class)) {
                new ContainerExceptionNotifier(e, UtilMethods.isSet(key.id1) ? key.id1 : key.path).notifyUser();
            }
            throw e;
        }
    }


    @Override
    public void invalidate(Object obj, PageMode mode) {
        final Container container = (Container) obj;

        final VelocityResourceKey key =
                new VelocityResourceKey(container, mode);
        final DotResourceCache    veloctyResourceCache =
                CacheLocator.getVeloctyResourceCache();

        veloctyResourceCache.remove(key);
    }

    public void invalidate(final FileAssetContainer fileAssetContainer, final Folder containerFolder, final String fileAssetName) throws DotDataException, DotSecurityException{
        final String cacheKeyMask = "%d%s";
        final DotResourceCache velocityResourceCache = CacheLocator.getVeloctyResourceCache();
        for(final PageMode mode:PageMode.values()){
        final VelocityResourceKey key = new VelocityResourceKey(fileAssetContainer, fileAssetContainer.getIdentifier(), mode);

        // Here's an example of how the key actually looks like when stored in the the velocity 2 cache.
        // `1/LIVE/a050073a-a31e-4aab-9307-86bfb248096a/1550262106697.container`
        // However when calling  DotResourceLoader.getResourceStream(path..
        // it looks like this: `/LIVE/a050073a-a31e-4aab-9307-86bfb248096a/1550262106697.container` no leading `1`
        // That leading character `1` comes from ResourceManagerImpl.getResource(.. it's the resource type defined in ResourceManager.RESOURCE_TEMPLATE
            final String identifierKey = String.format(cacheKeyMask, ResourceManager.RESOURCE_TEMPLATE, key.path);
            Logger.debug(this,()->String.format("Invalidating asset key: '%s'",identifierKey));
            velocityResourceCache.remove(identifierKey);
        }
        // Sometimes the in-cache key is the file site/path or path it self.
        // it's pretty difficult at this point knowing exactly what was used to put this thing in cache. So here we go trying a few options

        final Set<FileAsset> fileAssetSet = FileAssetContainerUtil.getInstance().findContainerAssets(containerFolder);
        if(UtilMethods.isSet(fileAssetName) && null != fileAssetSet ){
           final Set<FileAsset> matchingFiles = fileAssetSet.stream().filter(fileAsset ->  fileAssetName.equals(fileAsset.getFileName())).collect(Collectors.toSet());
           for(final FileAsset fileAsset:matchingFiles){
               final String fileAssetPath = fileAsset.getFileAsset().getPath();
               //e.g. /Users/fabrizzio/code/servers/server1/assets/5/8/58c39dc2-16a8-4268-b5d2-e609afb158a7/fileAsset/blog.vtl
               Logger.debug(this,()->String.format("Invalidating asset-path key '%s'",fileAssetPath));
               velocityResourceCache.remove(fileAssetPath);
               //e.g. 1/Users/fabrizzio/code/servers/server1/assets/5/8/58c39dc2-16a8-4268-b5d2-e609afb158a7/fileAsset/blog.vtl
               final String fileAssetPathKey = String.format(cacheKeyMask, ResourceManager.RESOURCE_TEMPLATE, fileAssetPath);
               Logger.debug(this,()->String.format("Invalidating asset-path with resource key '%s'",fileAssetPathKey));
               velocityResourceCache.remove(fileAssetPathKey);
           }
        }
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






        // if the container needs to get its contentlets
        if (container.getMaxContentlets() > 0) {


            // pre loop if it exists
            if (UtilMethods.isSet(container.getPreLoop())) {
                sb.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                sb.append(container.getPreLoop());
                sb.append("#end");
            }


            if (mode == PageMode.EDIT_MODE) {
                final StringWriter editWrapperDiv = new StringWriter();

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
                        editWrapperDiv.append(",");
                    } catch (DotDataException | DotSecurityException e) {
                        Logger.warn(this.getClass(), "unable to find content type:" + struct);
                    }
                }

                editWrapperDiv.append("WIDGET,FORM");
                editWrapperDiv.append("\"");
                editWrapperDiv.append(" data-dot-can-add=\"$containerAPI.getBaseContentTypeUserHasPermissionToAdd($containerInode)\"");
                editWrapperDiv.append(">");

                sb.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                sb.append(editWrapperDiv);
                sb.append("#end");

            }
            
            
            
            
            
            
            
            
            // sb.append("$contentletList" + identifier.getId() + uuid + "<br>");

            // START CONTENT LOOP
            sb.append("#foreach ($contentletId in $contentletList") // todo:
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
                sb.append("#set($CONTENT_BASE_TYPE = '')");
                sb.append("#set($CONTENT_LANGUAGE = '')");
                sb.append("#set($ContentletTitle = '')");
                sb.append("#set($CONTENT_TYPE_ID = '')");
                sb.append("#set($CONTENT_TYPE = '')");
                
                // read in the content
                sb.append("#if($contentletId != '')");
                sb.append("#contentDetail($contentletId)");
                sb.append("#end");
    
                sb.append("#set($HAVE_A_VERSION=($CONTENT_INODE != ''))");
                
                if (mode == PageMode.EDIT_MODE) {
                    sb.append("<div")
                        .append(" data-dot-object=")
                        .append("\"contentlet\"")
                        .append(" data-dot-inode=")
                        .append("\"$CONTENT_INODE\"")
                        .append(" data-dot-identifier=")
                        .append("\"$contentletId\"")
                        .append(" data-dot-type=")
                        .append("\"$CONTENT_TYPE\"")
                        .append(" data-dot-basetype=")
                        .append("\"$CONTENT_BASE_TYPE\"")
                        .append(" data-dot-lang=")
                        .append("\"$CONTENT_LANGUAGE\"")
                        .append(" data-dot-title=")
                        .append("\"$UtilMethods.javaScriptify($ContentletTitle)\"")
                        .append(" data-dot-can-edit=")
                        .append("\"$contents.doesUserHasPermission($CONTENT_INODE, 2, true)\"")
                        .append(" data-dot-content-type-id=")
                        .append("\"$CONTENT_TYPE_ID\"")
                        .append(" data-dot-has-page-lang-version=")
                        .append("\"$HAVE_A_VERSION\"")
                        .append(">");
                }
                

                // if content exists in language 
                sb.append("#if($HAVE_A_VERSION)");
                
                    // ##Checking permission to see content
                    if (mode.showLive) sb.append("#if($contents.doesUserHasPermission($CONTENT_INODE, 1, $user, true))");
                    
                        // ### START BODY ###
                        sb.append("#if($isWidget==true)").append("$widgetCode");
                        sb.append("#elseif($isForm==true)").append("$formCode");
                        sb.append("#else");
            
                            for (int i = 0; i < csList.size(); i++) {
                                ContainerStructure cs = csList.get(i);
                                String ifelse = (i == 0) ? "if" : "elseif";
                                sb.append("#" + ifelse + "($ContentletStructure ==\"" + cs.getStructureId() + "\")");
                                sb.append(cs.getCode());
                            }
                            if (csList.size() > 0) sb.append("#end");
                            
                        // ### END BODY ###
                        sb.append("#end");
        
                    if (mode.showLive) sb.append("#end");
                    
                // end if content exists in language 
                sb.append("#end");

               // end content dot-data-content
            if (mode == PageMode.EDIT_MODE) sb.append("</div>");
                
                // ##End of foreach loop
            sb.append("#end");
                
            // end content dot-data-container
            if (mode == PageMode.EDIT_MODE) {
                sb.append("#if($");
                sb.append(SHOW_PRE_POST_LOOP);
                sb.append(")");
                sb.append("</div>");
                sb.append("#end");
            }


            // post loop if it exists
            if (UtilMethods.isSet(container.getPostLoop())) {
                sb.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                sb.append(container.getPostLoop());
                sb.append("#end");
            }

            // end if maxContentlets >0
        } else {

            sb.append(container.getCode());
        }



        return writeOutVelocity(filePath, sb.toString());
    }






}
