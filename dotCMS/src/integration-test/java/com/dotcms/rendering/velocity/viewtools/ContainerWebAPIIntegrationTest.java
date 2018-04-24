package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.liferay.portal.model.User;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration test of {@link ContainerWebAPI}
 */
public class ContainerWebAPIIntegrationTest extends IntegrationTestBase {

    private static User user;
    private static User adminUser;
    private Container container;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user = APILocator.getUserAPI().getUsersByNameOrEmail("bill@dotcms.com", 0, 1).get(0);
        adminUser = APILocator.getUserAPI().getUsersByNameOrEmail("admin@dotcms.com", 0, 1).get(0);
    }

    @Before
    public void prepareEachTest() throws Exception {
        container = this.createContainer(user, adminUser);
    }

    @Test
    public void testDoesUserHasPermissionToAddWidget_UserWithoutPermission_ReturnFalse() throws DotDataException, DotSecurityException {
        ContainerWebAPI containerWebAPI = new ContainerWebAPI();
        boolean doesUserHasPermissionToAddWidget = containerWebAPI.doesUserHasPermissionToAddWidget(container.getInode());

        assertFalse(doesUserHasPermissionToAddWidget);
    }

    @Test
    public void testDoesUserHasPermissionToAddWidget_UserWithPermission_ReturnTrue() throws DotDataException, DotSecurityException {

        /*ContainerWebAPI containerWebAPI = new ContainerWebAPI();

        ContentType widget = APILocator.getContentTypeAPI(adminUser).findByType(BaseContentType.WIDGET).get(0);

        Permission permission = new Permission();
        permission.setPermission(PermissionAPI.PERMISSION_READ);
        permission.setInode(widget.inode());
        Role role = APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).get(0);
        permission.setRoleId(role.getId());
        APILocator.getPermissionAPI().save(permission, widget, adminUser, false);*/

        List<ContentType> byType = APILocator.getContentTypeAPI(user).findByType(BaseContentType.WIDGET);
        assertEquals("sadasdasd", byType.get(0).name());
        //boolean doesUserHasPermissionToAddWidget = containerWebAPI.doesUserHasPermissionToAddWidget(container.getInode());

        //assertTrue(doesUserHasPermissionToAddWidget);
    }

    @After
    public void cleanUp() throws Exception {
        HibernateUtil.startTransaction();
        APILocator.getContainerAPI().delete(container, adminUser, false);
        HibernateUtil.commitTransaction();
    }

    private Container createContainer(User user, User adminUser) throws DotDataException, DotSecurityException {
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

        containerStructures.add(containerStructure);

        Container containerSaved = APILocator.getContainerAPI().save(container, containerStructures, defaultHost, adminUser, false);

        HibernateUtil.commitTransaction();

        return containerSaved;
    }
}
