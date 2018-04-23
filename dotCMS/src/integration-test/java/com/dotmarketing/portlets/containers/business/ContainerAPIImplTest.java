package com.dotmarketing.portlets.containers.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test of {@link ContainerAPIImpl}
 */
public class ContainerAPIImplTest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void getContentTypesInContainer() throws DotDataException, DotSecurityException {
        Container container = null;
        ContentType contentType = null;

        User user = APILocator.getUserAPI().getUsersByNameOrEmail("bill@dotcms.com", 0, 1).get(0);
        User adminUser = APILocator.getUserAPI().getUsersByNameOrEmail("admin@dotcms.com", 0, 1).get(0);

        try {
            contentType = createContentType(adminUser);
            container = createContainer(user, adminUser, contentType);

            ContainerAPIImpl containerAPI = new ContainerAPIImpl();
            List<ContentType> contentTypesInContainer = containerAPI.getContentTypesInContainer(user, container);

            assertEquals(1, contentTypesInContainer.size());
            assertEquals("Document", ((ContentType) contentTypesInContainer.get(0)).name());
        } finally {
            HibernateUtil.startTransaction();
            if (container != null) {
                APILocator.getContainerAPI().delete(container, adminUser, false);
            }

            if (contentType != null) {
                APILocator.getContentTypeAPI(adminUser).delete(contentType);
            }
            HibernateUtil.commitTransaction();
        }
    }

    private Container createContainer(User user, User adminUser, ContentType contentType) throws DotDataException, DotSecurityException {
        HibernateUtil.startTransaction();
        Host defaultHost = APILocator.getHostAPI().findDefaultHost( user, false );
        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        Container container = new Container();
        container.setFriendlyName("test container");
        container.setTitle("this is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");

        List<ContainerStructure> containerStructures = new ArrayList<ContainerStructure>();

        ContainerStructure containerStructure = new ContainerStructure();
        containerStructure.setStructureId(contentTypeAPI.find("Document").inode());
        containerStructure.setCode("this is the code");

        ContainerStructure containerStructure2 = new ContainerStructure();
        containerStructure2.setStructureId(contentType.inode());
        containerStructure2.setCode("this is the code");

        containerStructures.add(containerStructure);
        containerStructures.add(containerStructure2);

        Container containerSaved = APILocator.getContainerAPI().save(container, containerStructures, defaultHost, adminUser, false);

        HibernateUtil.commitTransaction();

        return containerSaved;
    }

    private ContentType createContentType(User user) throws DotSecurityException, DotDataException {
        Host host = APILocator.getHostAPI().findDefaultHost(user, false);

        Folder folder = APILocator.getFolderAPI()
                .createFolders("/folderMoveSourceTest"+System.currentTimeMillis(), host, user, false);

        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("description")
                .folder(folder.getInode()).host(host.getInode())
                .name("ContentTypeTesting")
                .owner("owner")
                .variable("velocityVarNameTesting")
                .build();


        ContentType contentTypeSaved = APILocator.getContentTypeAPI(user).save(contentType);

        /*Permission permission = new Permission();
        permission.setPermission(PermissionAPI.PERMISSION_PUBLISH);
        permission.setInode(contentTypeSaved.inode());
        Role role = APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).get(0);
        permission.setRoleId(role.getId());
        APILocator.getPermissionAPI().save(permission, contentTypeSaved, user, false);
        */

        return contentTypeSaved;
    }

}
