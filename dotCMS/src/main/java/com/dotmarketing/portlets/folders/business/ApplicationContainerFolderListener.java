package com.dotmarketing.portlets.folders.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.services.ContainerLoader;
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
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

/**
 * Folder listener for the application/container folder
 * @author jsanca
 */
public class ApplicationContainerFolderListener implements FolderListener {

    private final ContainerAPI  containerAPI        = APILocator.getContainerAPI();
    private final PermissionAPI permissionAPI       = APILocator.getPermissionAPI();
    private final IdentifierAPI identifierAPI       = APILocator.getIdentifierAPI();
    private final MultiTreeAPI  multiTreeAPI        = APILocator.getMultiTreeAPI();

    @Override
    public void folderChildModified(FolderEvent folderEvent) {

        // todo: create here the invalidation
        // 1) if the file is content type, check if it exists
        // 2) if it is container, preloop or postloop
        // 3) checks if the container.vtl exists
        // 4) invalidate the container
    }

    @Override
    public void folderChildDeleted(final FolderEvent folderEvent) {

        if (null != folderEvent && null != folderEvent.getChild()) {

            final String childName       = folderEvent.getChildName();
            final Folder containerFolder = folderEvent.getParent();

            try {

                final Container container = this.containerAPI
                        .getContainerByFolder(containerFolder, folderEvent.getUser(),false);

                if (null != container && UtilMethods.isSet(container.getIdentifier())) {

                    this.invalidateContainerCache(container);
                    //this.invalidatedRelatedPages (container);

                    // removing the whole container folder, so remove the relationship
                    if (Constants.CONTAINER_META_INFO_FILE_NAME.equals(childName)) {
                        this.removeContainerFromTemplate(container, folderEvent.getUser());
                    }

                    Logger.debug(this, ()->"The child: " + childName + " on the folder: " +
                            containerFolder + ", has been removed, so the container was invalidated");
                }
            } catch (DotSecurityException | DotDataException e) {

                Logger.debug(this, "The child: " + childName + " on the folder: " +
                        containerFolder + ", has been removed, BUT the container could not be invalidated", e);
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
