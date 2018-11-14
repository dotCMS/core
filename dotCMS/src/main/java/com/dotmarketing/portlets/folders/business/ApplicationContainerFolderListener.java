package com.dotmarketing.portlets.folders.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;
import static com.dotmarketing.util.StringUtils.builder;

/**
 * Folder listener for the application/container folder
 * @author jsanca
 */
public class ApplicationContainerFolderListener implements FolderListener {

    private final ContainerAPI   containerAPI        = APILocator.getContainerAPI();
    private final PermissionAPI  permissionAPI       = APILocator.getPermissionAPI();
    private final IdentifierAPI  identifierAPI       = APILocator.getIdentifierAPI();
    private final MultiTreeAPI   multiTreeAPI        = APILocator.getMultiTreeAPI();
    private final HostAPI        hostAPI             = APILocator.getHostAPI();

    @Override
    public void folderChildModified(final FolderEvent folderEvent) {

        if (null != folderEvent && null != folderEvent.getChild()) {

            final String childName       = folderEvent.getChildName();
            final Folder containerFolder = folderEvent.getParent();

            if (this.isValidChild(folderEvent, childName, containerFolder)) {

                try {

                    final Container container = this.containerAPI
                            .getContainerByFolder(containerFolder, folderEvent.getUser(), false);

                    if (null != container && UtilMethods.isSet(container.getIdentifier())) {

                        this.invalidateContainerCache(container);
                        //this.invalidatedRelatedPages (container);

                        if (Constants.CONTAINER_META_INFO_FILE_NAME.equals(childName)) {
                            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(container);
                        }

                        Logger.debug(this, () -> "The child: " + childName + " on the folder: " +
                                containerFolder + ", has been removed, so the container was invalidated");
                    }
                } catch (DotSecurityException | DotDataException e) {

                    Logger.debug(this, "The child: " + childName + " on the folder: " +
                            containerFolder + ", has been removed, BUT the container could not be invalidated", e);
                }
            }
        }
    }

    @Override
    public void folderChildDeleted(final FolderEvent folderEvent) {

        if (null != folderEvent && null != folderEvent.getChild()) {

            final String childName       = folderEvent.getChildName();
            final Folder containerFolder = folderEvent.getParent();

            if (this.isSpecialAsset (childName) || isContentType(folderEvent, childName)) {
                try {

                    final Container container = this.containerAPI
                            .getContainerByFolder(containerFolder, folderEvent.getUser(), false);

                    if (null != container && UtilMethods.isSet(container.getIdentifier())) {

                        this.invalidateContainerCache(container);
                        //this.invalidatedRelatedPages (container);

                        // removing the whole container folder, so remove the relationship
                        if (Constants.CONTAINER_META_INFO_FILE_NAME.equals(childName)) {
                            this.removeContainerFromTemplate(container, folderEvent.getUser());
                        }

                        Logger.debug(this, () -> "The child: " + childName + " on the folder: " +
                                containerFolder + ", has been removed, so the container was invalidated");
                    }
                } catch (DotSecurityException | DotDataException e) {

                    Logger.debug(this, "The child: " + childName + " on the folder: " +
                            containerFolder + ", has been removed, BUT the container could not be invalidated", e);
                }
            }
        }
    } // folderChildDeleted.

    @WrapInTransaction
    private void removeContainerFromTemplate(final Container container, final User user) throws DotDataException {

        if(this.permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user)) {

            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(container);
            final Identifier identifier = this.identifierAPI.find(container);
            this.multiTreeAPI.deleteMultiTreeByIdentifier(identifier);
        }
    } // removeContainerFromTemplate.


    private boolean isValidChild(final FolderEvent folderEvent,
                                 final String childName,
                                 final Folder containerFolder) {

        try {

            if (!this.isSpecialAsset (childName) && !isContentType(folderEvent, childName)) {

                return false; // is not a content type. do not do anything
            }

            if (!this.hasContainerAsset(this.hostAPI.find
                    (containerFolder.getHostId(), folderEvent.getUser(), false), containerFolder)) {
                // has not the container.vtl asset
                return false;
            }
        } catch (DotSecurityException | DotDataException e) {
            // is not a content type. do not do anything
            return false;
        }

        return true;
    }

    private boolean isContentType(final FolderEvent folderEvent, final String childName)  {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(folderEvent.getUser());
        final String contentTypeVarName = this.normalizeVelocityNameFromVTLName(childName);

        try {

            if (null == contentTypeAPI.find(contentTypeVarName)) {
                return false;
            }
        } catch (DotSecurityException | DotDataException e) {
            // is not a content type. do not do anything
            return false;
        }

        return true;
    }

    private String normalizeVelocityNameFromVTLName(final String childName) {

        if (UtilMethods.isSet(childName) && childName.endsWith(Constants.VELOCITY_FILE_EXTENSION)) {

            return childName.replace(Constants.VELOCITY_FILE_EXTENSION, StringPool.BLANK);
        }
        return childName;
    }

    private boolean hasContainerAsset(final Host host, final Folder folder) {
        try {

            final Identifier identifier = this.identifierAPI.find(host, builder(folder.getPath(),
                    Constants.CONTAINER_META_INFO_FILE_NAME).toString());
            return null != identifier && UtilMethods.isSet(identifier.getId());
        } catch (Exception  e) {
            return false;
        }
    }

    private boolean isSpecialAsset(final String childName) {

        return ContainerAPI.CONTAINER_META_INFO.contains(childName) ||
                ContainerAPI. POST_LOOP.contains(childName) ||
                ContainerAPI.PRE_LOOP.contains(childName);
    }

    // todo: we should not need this
    /**private void invalidatedRelatedPages(final Container container) throws DotDataException, DotSecurityException {

        final List<IHTMLPage> pageList = this.htmlPageAssetAPI.
                getHTMLPagesByContainer(container.getIdentifier());

        if (UtilMethods.isSet(pageList)) {

            for (final IHTMLPage page : pageList) {

                new PageLoader().invalidate(page);
            }
        }
    }*/

    private void invalidateContainerCache(final Container container) {

        new ContainerLoader().invalidate(container);
        CacheLocator.getContainerCache().remove(container);
        CacheLocator.getContentTypeCache().removeContainerStructures
                (container.getIdentifier(), container.getInode());
    }
}
