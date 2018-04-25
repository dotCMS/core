package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.org.apache.http.*;
import com.dotcms.repackage.org.apache.http.params.HttpParams;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Http;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test of {@link ContainerWebAPI}
 */
public class ContainerWebAPIIntegrationTest extends IntegrationTestBase {

    private User user;
    private static User adminUser;
    private Container container;
    private ContainerWebAPI containerWebAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        adminUser = APILocator.getUserAPI().getUsersByNameOrEmail("admin@dotcms.com", 0, 1).get(0);
    }

    private static User createUser() throws DotDataException, DotSecurityException {
        HibernateUtil.startTransaction();
        User user = APILocator.getUserAPI().createUser("new.user@test.com", "new.user@test.com");
        user.setFirstName("Test-11962");
        user.setLastName("User-11962");

        APILocator.getUserAPI().save(user, adminUser, false);
        assertNotNull(user.getUserId());

        HibernateUtil.closeAndCommitTransaction();
        return APILocator.getUserAPI().getUsersByNameOrEmailOrUserID(user.getEmailAddress(), 0, 1).get(0);
    }

    @Before
    public void prepareEachTest() throws Exception {
        user = createUser();
        container = this.createContainer(user, adminUser);

        VelocityContext velocityContext = mock(VelocityContext.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ViewContext viewContext = mock(ViewContext.class);
        HttpSession session = mock(HttpSession.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(user.getUserId());

        containerWebAPI = new ContainerWebAPI();
        containerWebAPI.init(viewContext);
    }

    @Test
    public void testDoesUserHasPermissionToAddWidget_GivenUserWithoutPermission_ShouldReturnFalse() throws DotDataException, DotSecurityException {
        boolean doesUserHasPermissionToAddWidget = containerWebAPI.doesUserHasPermissionToAddWidget(container.getInode());
        assertFalse(doesUserHasPermissionToAddWidget);
    }

    @Test
    public void testDoesUserHasPermissionToAddWidget_GivenUserWithPermission_ShouldReturnTrue() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        boolean doesUserHasPermissionToAddWidget = containerWebAPI.doesUserHasPermissionToAddWidget(container.getInode());

        assertTrue(doesUserHasPermissionToAddWidget);
    }

    @Test
    public void testDoesUserHasPermissionToAddForm_GivenUserWithPermission_ShouldReturnTrue() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);
        boolean doesUserHasPermissionToAddForm = containerWebAPI.doesUserHasPermissionToAddForm(container.getInode());

        assertTrue(doesUserHasPermissionToAddForm);
    }

    @Test
    public void testDoesUserHasPermissionToAddForm_GivenUserWithoutPermission_ShouldReturnFalse() throws DotDataException, DotSecurityException {
        boolean doesUserHasPermissionToAddForm = containerWebAPI.doesUserHasPermissionToAddForm(container.getInode());
        assertFalse(doesUserHasPermissionToAddForm);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithoutPermission_ShouldReturnEmptyString() throws DotDataException, DotSecurityException {
        String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        assertEquals("", baseContentTypeUserHasPermissionToAdd);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithWidgetPermission_ShouldReturnWidgetString() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        assertEquals("WIDGET", baseContentTypeUserHasPermissionToAdd);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithFormPermission_ShouldReturnFormString() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);

        String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        assertEquals("FORM", baseContentTypeUserHasPermissionToAdd);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithWidgetFormPermission_ShouldReturnWidgetFormString() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        List<String> baseContentTypes = Arrays.asList(baseContentTypeUserHasPermissionToAdd.split(","));

        assertEquals(2, baseContentTypes.size());
        assertTrue(baseContentTypes.contains("WIDGET"));
        assertTrue(baseContentTypes.contains("FORM"));
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithWidgetFormContentPermission_ShouldReturnWidgetFormContentString() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        ContentType documentContent = APILocator.getContentTypeAPI(user).find("Document");
        this.addPermission(documentContent.inode(), documentContent);

        String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        List<String> baseContentTypes = Arrays.asList(baseContentTypeUserHasPermissionToAdd.split(","));

        assertEquals(3, baseContentTypes.size());
        assertTrue(baseContentTypes.contains("WIDGET"));
        assertTrue(baseContentTypes.contains("FORM"));
        assertTrue(baseContentTypes.contains("CONTENT"));
    }

    @After
    public void cleanUp() throws Exception {
        HibernateUtil.startTransaction();
        APILocator.getContainerAPI().delete(container, adminUser, false);
        APILocator.getUserAPI().delete(user, adminUser, false);
       HibernateUtil.commitTransaction();
    }

    private void addPermissionToBaseType(BaseContentType baseContentType) throws DotSecurityException, DotDataException {
        ContentType widget = APILocator.getContentTypeAPI(adminUser).findByType(baseContentType).get(0);
        this.addPermission(widget.inode(), widget);
    }

    private void addPermission(String permissionableInode, Permissionable permissionable) throws DotDataException, DotSecurityException {
        Role role = APILocator.getRoleAPI().getUserRole(user);

        Permission permission = new Permission();
        permission.setPermission(PermissionAPI.PERMISSION_READ);
        permission.setInode(permissionableInode);
        permission.setRoleId(role.getId());

        APILocator.getPermissionAPI().save(permission, permissionable, adminUser, false);
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
