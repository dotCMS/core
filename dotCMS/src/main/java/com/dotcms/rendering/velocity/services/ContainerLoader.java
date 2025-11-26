package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
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
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.apache.velocity.runtime.resource.ResourceManager;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.PERIOD;

/**
 * @author will
 */
public class ContainerLoader implements DotLoader {

    public static final String SHOW_PRE_POST_LOOP="SHOW_PRE_POST_LOOP";
    public static final String FILE_CONTAINER_PATH_SEPARATOR_IN_VELOCITY_KEY = "%";
    public static final String HOST_NAME_SEPARATOR_IN_VELOCITY_KEY = "#";
    public static final String RESOLVE_RELATIVE = "resolve_relative";

    @Override
    public InputStream writeObject(final VelocityResourceKey key)
            throws DotDataException, DotSecurityException {

        try {
            final String containerIdOrPath = key.path.split(StringPool.FORWARD_SLASH)[2]
                    .replaceAll(RESOLVE_RELATIVE + FILE_CONTAINER_PATH_SEPARATOR_IN_VELOCITY_KEY, "")
                    .replaceAll(FILE_CONTAINER_PATH_SEPARATOR_IN_VELOCITY_KEY, StringPool.FORWARD_SLASH)
                    .replaceAll(HOST_NAME_SEPARATOR_IN_VELOCITY_KEY, PERIOD);
            final Optional<Container> optionalContainer = APILocator.getContainerAPI().findContainer(containerIdOrPath, APILocator.systemUser(), key.mode.showLive, key.mode.respectAnonPerms);

            if (optionalContainer.isEmpty()) {

                final DotStateException dotStateException = new DotStateException("Cannot find container for : " + key);
                new ContainerExceptionNotifier(dotStateException, UtilMethods.isSet(key.id1) ? key.id1 : key.path).notifyUser();
                throw dotStateException;
            }

            final Container container = optionalContainer.get();
            if (container.isArchived()) {

                throw new DotStateException("The container  : " + key  + " is archived");
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
        final Host      host      = APILocator.getHostAPI().find(containerFolder.getHostId(), APILocator.systemUser(), false);
        final Container container = new Container();
        if (UtilMethods.isSet(host) && UtilMethods.isSet(host.getHostname())
                && UtilMethods.isSet(containerFolder) && UtilMethods.isSet(containerFolder.getPath())) {

            final String path = "//" + host.getHostname() + containerFolder.getPath();
            container.setIdentifier(path);
        }

        for(final PageMode mode:PageMode.values()) {

            // Here's an example of how the key actually looks like when stored in the the velocity 2 cache.
            // `1/LIVE/a050073a-a31e-4aab-9307-86bfb248096a/1550262106697.container`
            // However when calling  DotResourceLoader.getResourceStream(path..
            // it looks like this: `/LIVE/a050073a-a31e-4aab-9307-86bfb248096a/1550262106697.container` no leading `1`
            // That leading character `1` comes from ResourceManagerImpl.getResource(.. it's the resource type defined in ResourceManager.RESOURCE_TEMPLATE
            this.invalidateContainer(fileAssetContainer, cacheKeyMask, velocityResourceCache, mode, fileAssetContainer.getIdentifier());

            if (UtilMethods.isSet(container.getIdentifier())) {
                // Now removing by path //demo.dotcms.com/application/containers/large-column/
                this.invalidateContainer(container, cacheKeyMask, velocityResourceCache, mode, ContainerUUID.UUID_DEFAULT_VALUE);
            }
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

    private void invalidateContainer(final Container container,
                                     final String cacheKeyMask,
                                     final DotResourceCache velocityResourceCache,
                                     final PageMode mode,
                                     final String uuid) {

        final VelocityResourceKey key = new VelocityResourceKey(container, uuid, mode);
        final String identifierKey = String.format(cacheKeyMask, ResourceManager.RESOURCE_TEMPLATE, key.path);
        Logger.debug(this,()->String.format("Invalidating asset key: '%s'", identifierKey));
        velocityResourceCache.remove(identifierKey);
    }

    private String getDataDotIdentifier (final Container container) {

        return container instanceof FileAssetContainer?
                FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) container):
                container.getIdentifier();

    }

    private InputStream buildVelocity(final Container container, final String uuid,
                                      final PageMode mode,
                                      final String filePath) throws DotDataException, DotSecurityException {

        
        final String transformedUUID = ContainerUUID.UUID_LEGACY_VALUE.equals(uuid) ? ContainerUUID.UUID_START_VALUE : uuid;

        final ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        final StringBuilder velocityCodeBuilder = new StringBuilder();
        final List<ContainerStructure> containerContentTypeList = APILocator.getContainerAPI()
                .getContainerStructures(container);

        // let's write this puppy out to our file
        velocityCodeBuilder.append("#set ($SERVER_NAME =$host.getHostname() )");
        velocityCodeBuilder.append("#set ($CONTAINER_IDENTIFIER = '")
            .append(container.getIdentifier())
            .append("')");
        velocityCodeBuilder.append("#set ($CONTAINER_UNIQUE_ID = '")
            .append(transformedUUID)
            .append("')");
        velocityCodeBuilder.append("#set ($CONTAINER_INODE = '")
            .append(container.getInode())
            .append("')");
        velocityCodeBuilder.append("#set ($CONTAINER_MAX_CONTENTLETS = ")
            .append(container.getMaxContentlets())
            .append(")");
        velocityCodeBuilder.append("#set ($containerInode = '")
            .append(container.getInode())
            .append("')");

        // START CONTENT LOOP

        // the viewtool call to get the list of contents for the container
        String apiCall="$containerAPI.getPersonalizedContentList(\"$!HTMLPAGE_IDENTIFIER\",\"$!CONTAINER_IDENTIFIER\", \"$!CONTAINER_UNIQUE_ID\")";
        
        velocityCodeBuilder.append("#set ($CONTENTLETS = ")
        .append(apiCall)
        .append(")");
        velocityCodeBuilder.append("#set ($CONTAINER_NUM_CONTENTLETS = ${CONTENTLETS.size()})");


        velocityCodeBuilder.append("#set ($CONTAINER_NAME = \"")
            .append(UtilMethods.espaceForVelocity(container.getTitle()))
            .append("\")");

        if (UtilMethods.isSet(container.getNotes())) {
            velocityCodeBuilder.append("#set ($CONTAINER_NOTES = \"")
                .append(UtilMethods.espaceForVelocity(container.getNotes()))
                .append("\")");
        } else {
            velocityCodeBuilder.append("#set ($CONTAINER_NOTES = \"\")");
        }

        // if the container needs to get its contentlets
        if (container.getMaxContentlets() > 0) {

            // pre loop if it exists
            if (UtilMethods.isSet(container.getPreLoop())) {
                velocityCodeBuilder.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                velocityCodeBuilder.append(container.getPreLoop());
                velocityCodeBuilder.append("#end");
            }


            if (mode == PageMode.EDIT_MODE) {
                final StringWriter editWrapperDiv = new StringWriter();

                editWrapperDiv.append("<div")
                    .append(" data-dot-object=")
                    .append("\"container\"")
                    .append(" data-dot-inode=")
                    .append("\"" + container.getInode() + "\"")
                    .append(" data-dot-identifier=")
                    .append("\"" + this.getDataDotIdentifier(container) + "\"")
                    .append(" data-dot-uuid=")
                    .append("\"" + transformedUUID + "\"")
                    .append(" data-max-contentlets=")
                    .append("\"" + container.getMaxContentlets() + "\"")
                    .append(" data-dot-accept-types=")
                    .append("\"");

                final Iterator<ContainerStructure> contentTypeIterator =
                        containerContentTypeList.iterator();
                while (contentTypeIterator.hasNext()) {

                    final ContainerStructure containerContentType = contentTypeIterator.next();
                    try {
                        final ContentType contentType = typeAPI.find(containerContentType.getStructureId());
                        editWrapperDiv.append(contentType.variable());
                        editWrapperDiv.append(",");
                    } catch (DotDataException | DotSecurityException e) {
                        Logger.warn(this.getClass(), "unable to find content type:" + containerContentType);
                    }
                }

                editWrapperDiv.append("WIDGET,FORM");
                editWrapperDiv.append("\"");
                editWrapperDiv.append(" data-dot-can-add=\"$containerAPI.getBaseContentTypeUserHasPermissionToAdd($containerInode)\"");
                editWrapperDiv.append(">");

                velocityCodeBuilder.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                velocityCodeBuilder.append(editWrapperDiv);
                velocityCodeBuilder.append("#end");
            }
            // WARNING: Make no changes to the variable names used here without having checked first its usage
            // e.g. ContentletDetail.java uses _show_working_

            //Let's find out if the time-machine attribute is set
            velocityCodeBuilder.append("#set($_timeMachineOn=false)")
            .append("#if($UtilMethods.isSet($request.getSession(false)) && $request.session.getAttribute(\""+PageResource.TM_DATE+"\"))")
                .append("#set($_timeMachineOn=true)")
                .append("#set($_tmdate=$date.toDate($webapi.parseLong($request.session.getAttribute(\""+PageResource.TM_DATE+"\"))))")
            .append("#end")
            .append("#if($request.getAttribute(\""+PageResource.TM_DATE+"\"))")
               .append("#set($_timeMachineOn=true)")
               .append("#set($_tmdate=$date.toDate($webapi.parseLong($request.getAttribute(\""+PageResource.TM_DATE+"\"))))")
            .append("#end");

            // FOR LOOP
            velocityCodeBuilder
            .append("#foreach ($contentletId in $CONTENTLETS )")
              .append("#set($dotContentMap=$dotcontent.load($contentletId))")
              .append("#set($_show_working_=false)");

                //Time-machine block begin
                  velocityCodeBuilder.append("#if($_timeMachineOn) ");

                      velocityCodeBuilder
                      .append("#set($_ident=$webapi.findIdentifierById($contentletId))");

                      // if the content has expired we rewrite the identifier, so it isn't loaded
                      velocityCodeBuilder
                      .append("#if($UtilMethods.isSet($_ident.sysExpireDate) && $_tmdate.after($_ident.sysExpireDate))")
                        .append("#set($contentletId='')")
                     .append("#end");

                      // if the content should be published then force to show the working version
                      velocityCodeBuilder
                       .append("#if($UtilMethods.isSet($_ident.sysPublishDate) && ($_tmdate.equals($_ident.sysPublishDate) || $_tmdate.after($_ident.sysPublishDate)))")
                        .append("#set($_show_working_=true)")
                       .append("#end");

                      velocityCodeBuilder.append("#if(!$UtilMethods.isSet($user)) ")
                        .append("#set($user = $cmsuser.getLoggedInUser($request)) ")
                      .append("#end");
                  //end of time-machine block
                  velocityCodeBuilder.append("#end");

                    velocityCodeBuilder
                    .append("#set($CONTENT_INODE = '')")
                    .append("#set($CONTENT_BASE_TYPE = '')")
                    .append("#set($CONTENT_LANGUAGE = '')")
                    .append("#set($ContentletTitle = '')")
                    .append("#set($CONTENT_TYPE_ID = '')")
                    .append("#set($CONTENT_TYPE = '')")
                    .append("#set($CONTENT_VARIANT = '')")
                    .append("#set($ON_NUMBER_OF_PAGES = '')");

                    // read in the content
                    velocityCodeBuilder
                    .append("#if($contentletId != '')")
                      .append("#contentDetail($contentletId)")
                    .append("#end");

                velocityCodeBuilder.append("#set($HAVE_A_VERSION=($CONTENT_INODE != ''))");

                if (mode == PageMode.EDIT_MODE) {
                    velocityCodeBuilder.append("<div")
                        .append(" data-dot-object=")
                        .append("\"contentlet\"")
                        .append(" data-dot-on-number-of-pages=")
                        .append("\"$ON_NUMBER_OF_PAGES\"")
                        .append(" data-dot-inode=")
                        .append("\"$CONTENT_INODE\"")
                        .append(" data-dot-identifier=")
                        .append("\"$contentletId\"")
                        .append(" data-dot-type=")
                        .append("\"$CONTENT_TYPE\"")
                        .append(" data-dot-variant=")
                        .append("\"$CONTENT_VARIANT\"")
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
                velocityCodeBuilder.append("#if($HAVE_A_VERSION)");
                
                    // ##Checking permission to see content
                    if (mode.showLive) {
                        velocityCodeBuilder.append("#if($_show_working_ || $contents.doesUserHasPermission($CONTENT_INODE, 1, $user, true))");
                    }
                    
                        // ### START BODY ###
                        velocityCodeBuilder.append("#if($isWidget==true)").append("$widgetCode");
                        velocityCodeBuilder.append("#elseif($isForm==true)").append("$formCode");
                        velocityCodeBuilder.append("#else");

                            for (int i = 0; i < containerContentTypeList.size(); i++) {
                                ContainerStructure cs = containerContentTypeList.get(i);
                                String ifElse = (i == 0) ? "if" : "elseif";
                                velocityCodeBuilder.append("#").append(ifElse)
                                        .append("($ContentletStructure ==\"")
                                        .append(cs.getStructureId()).append("\")");
                                velocityCodeBuilder.append(cs.getCode());
                            }
                            if (!containerContentTypeList.isEmpty()) {
                                velocityCodeBuilder.append("#end");
                            }
                            
                        // ### END BODY ###
                        velocityCodeBuilder.append("#end");
        
                    if (mode.showLive) {
                        velocityCodeBuilder.append("#end");
                    }
                    
                // end if content exists in language 
                velocityCodeBuilder.append("#end");

               // end content dot-data-content
                if (mode == PageMode.EDIT_MODE) {
                    velocityCodeBuilder.append("</div>");
                }

                velocityCodeBuilder.append("#set($dotContentMap='')");
                // ##End of foreach loop
            velocityCodeBuilder.append("#end");
                
            // end content dot-data-container
            if (mode == PageMode.EDIT_MODE) {
                velocityCodeBuilder.append("#if($");
                velocityCodeBuilder.append(SHOW_PRE_POST_LOOP);
                velocityCodeBuilder.append(")");
                velocityCodeBuilder.append("</div>");
                velocityCodeBuilder.append("#end");
            }


            // post loop if it exists
            if (UtilMethods.isSet(container.getPostLoop())) {
                velocityCodeBuilder.append("#if($" +  SHOW_PRE_POST_LOOP + ")");
                velocityCodeBuilder.append(container.getPostLoop());
                velocityCodeBuilder.append("#end");
            }

            // end if maxContentlets >0
        } else {

            velocityCodeBuilder.append(container.getCode());
        }


        final String containerCode = velocityCodeBuilder.toString();
        Logger.debug(this, "DotResourceLoader:\tWriting out container code = " + containerCode);
        return writeOutVelocity(filePath, containerCode);
    }






}
