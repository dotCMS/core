package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

        TestDataUtils.getFormContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                null);
        TestDataUtils
                .getWidgetContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        null);
    }

    private static User createUser() throws DotDataException, DotSecurityException {

        final User user = new UserDataGen().nextPersisted();
        assertNotNull(user.getUserId());

        return user;
    }

    @Before
    public void prepareEachTest() throws Exception {
        user = createUser();
        container = this.createContainer(adminUser);

        final VelocityContext velocityContext = mock(VelocityContext.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final ViewContext viewContext = mock(ViewContext.class);
        final HttpSession session = mock(HttpSession.class);

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
        final boolean doesUserHasPermissionToAddWidget = containerWebAPI.doesUserHasPermissionToAddWidget(container.getInode());
        assertFalse(doesUserHasPermissionToAddWidget);
    }

    @Test
    public void testDoesUserHasPermissionToAddWidget_GivenUserWithPermission_ShouldReturnTrue() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        final boolean doesUserHasPermissionToAddWidget = containerWebAPI.doesUserHasPermissionToAddWidget(container.getInode());

        assertTrue(doesUserHasPermissionToAddWidget);
    }

    @Test
    public void testDoesUserHasPermissionToAddForm_GivenUserWithPermission_ShouldReturnTrue() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);
        final boolean doesUserHasPermissionToAddForm = containerWebAPI.doesUserHasPermissionToAddForm(container.getInode());

        assertTrue(doesUserHasPermissionToAddForm);
    }

    @Test
    public void testDoesUserHasPermissionToAddForm_GivenUserWithoutPermission_ShouldReturnFalse() throws DotDataException, DotSecurityException {
        final  boolean doesUserHasPermissionToAddForm = containerWebAPI.doesUserHasPermissionToAddForm(container.getInode());
        assertFalse(doesUserHasPermissionToAddForm);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithoutPermission_ShouldReturnEmptyString() throws DotDataException, DotSecurityException {
        final String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        assertEquals("", baseContentTypeUserHasPermissionToAdd);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithWidgetPermission_ShouldReturnWidgetString() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        final String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        assertEquals("WIDGET", baseContentTypeUserHasPermissionToAdd);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithFormPermission_ShouldReturnFormString() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);

        final String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        assertEquals("FORM", baseContentTypeUserHasPermissionToAdd);
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithWidgetFormPermission_ShouldReturnWidgetFormString() throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        final String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        final List<String> baseContentTypes = Arrays.asList(baseContentTypeUserHasPermissionToAdd.split(","));

        assertEquals(2, baseContentTypes.size());
        assertTrue(baseContentTypes.contains("WIDGET"));
        assertTrue(baseContentTypes.contains("FORM"));
    }

    @Test
    public void testGetBaseContentTypeUserHasPermissionToAdd_GivenUserWithWidgetFormContentPermission_ShouldReturnWidgetFormContentString()
            throws DotDataException, DotSecurityException {
        this.addPermissionToBaseType(BaseContentType.FORM);
        this.addPermissionToBaseType(BaseContentType.WIDGET);

        final ContentType genericContentContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("webPageContent");
        this.addPermission(genericContentContentType.inode(), genericContentContentType);

        final String baseContentTypeUserHasPermissionToAdd = containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(container.getInode());
        final List<String> baseContentTypes = Arrays.asList(baseContentTypeUserHasPermissionToAdd.split(","));

        assertEquals(3, baseContentTypes.size());
        assertTrue(baseContentTypes.contains("WIDGET"));
        assertTrue(baseContentTypes.contains("FORM"));
        assertTrue(baseContentTypes.contains("CONTENT"));
    }

    private void addPermissionToBaseType(BaseContentType baseContentType) throws DotSecurityException, DotDataException {
        final ContentType contentType = APILocator.getContentTypeAPI(adminUser)
                .findByType(baseContentType).get(0);
        this.addPermission(contentType.inode(), contentType);
    }

    private void addPermission(final String permissionableInode, final Permissionable permissionable)
            throws DotDataException, DotSecurityException {
        final Role role = APILocator.getRoleAPI().getUserRole(user);

        final Permission permission = new Permission();
        permission.setPermission(PermissionAPI.PERMISSION_READ);
        permission.setInode(permissionableInode);
        permission.setRoleId(role.getId());

        APILocator.getPermissionAPI().save(permission, permissionable, adminUser, false);
    }

    private Container createContainer(final User adminUser) throws DotDataException,
            DotSecurityException {

        HibernateUtil.startTransaction();
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost( adminUser, false );
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(adminUser);

        final Container container = new Container();
        container.setFriendlyName("test container");
        container.setTitle("this is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");

        final List<ContainerStructure> containerStructures = new ArrayList<ContainerStructure>();

        final ContainerStructure containerStructure = new ContainerStructure();
        containerStructure.setStructureId(contentTypeAPI.find("webPageContent").inode());
        containerStructure.setCode("this is the code");

        containerStructures.add(containerStructure);

        final Container containerSaved = APILocator.getContainerAPI().save(container, containerStructures, defaultHost, adminUser, false);

        HibernateUtil.commitTransaction();

        return containerSaved;
    }

    /**
     * Method to Test: {@link ContainerWebAPI#getPersonalizedContentList(String, String, String)}
     * When: A Page with a advanced template has a content
     * Should: Return the content's id
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void whenPageUseAdvanceTemplate() throws DotDataException, DotSecurityException {
        final String uuid = ParseContainer.getDotParserContainerUUID("1");
        final PageMode mode = PageMode.PREVIEW_MODE;
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();

        final HttpServletRequest request = mockHttpServletRequest(mode);

        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MultiTree multiTree = new MultiTreeDataGen()
                .setContentlet(contentlet)
                .setPage(htmlPageAsset)
                .setContainer(container)
                .setInstanceID(uuid)
                .nextPersisted();

        final PageRenderUtil pageRenderUtil = new PageRenderUtil(
                htmlPageAsset,
                APILocator.systemUser(),
                mode,
                1,
                host
        );

        final Context velocityContext  = pageRenderUtil
                .addAll(VelocityUtil.getInstance().getContext(request, response));

        final ContainerWebAPI containerWebAPI = new ContainerWebAPI();
        containerWebAPI.init(velocityContext);
        final List<String> personalizedContentList =
                containerWebAPI.getPersonalizedContentList(
                        htmlPageAsset.getIdentifier(),
                        container.getIdentifier(),
                        uuid
                );

        assertEquals(1, personalizedContentList.size());
        assertEquals(contentlet.getIdentifier(), personalizedContentList.get(0));
    }

    @NotNull
    private HttpServletRequest mockHttpServletRequest(PageMode mode) {
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getParameter(com.dotmarketing.util.WebKeys.PAGE_MODE_PARAMETER)).thenReturn(mode.toString());
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());
        return request;
    }

    /**
     * Method to Test: {@link ContainerWebAPI#getPersonalizedContentList(String, String, String)}
     * When: A Page with a not advanced template has a content
     * Should: Return the content's id
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void whenPageUseNotAdvanceTemplate() throws DotDataException, DotSecurityException {
        final String uuid = "1";
        final PageMode mode = PageMode.PREVIEW_MODE;
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container.getIdentifier())
                .next();
        final Template template = new TemplateDataGen()
            .drawedBody(templateLayout)
            .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();


        final HttpServletRequest request = mockHttpServletRequest(mode);

        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MultiTree multiTree = new MultiTreeDataGen()
                .setContentlet(contentlet)
                .setPage(htmlPageAsset)
                .setContainer(container)
                .setInstanceID(uuid)
                .nextPersisted();

        final PageRenderUtil pageRenderUtil = new PageRenderUtil(
                htmlPageAsset,
                APILocator.systemUser(),
                mode,
                1,
                host
        );

        final Context velocityContext  = pageRenderUtil
                .addAll(VelocityUtil.getInstance().getContext(request, response));

        final ContainerWebAPI containerWebAPI = new ContainerWebAPI();
        containerWebAPI.init(velocityContext);
        final List<String> personalizedContentList =
                containerWebAPI.getPersonalizedContentList(
                        htmlPageAsset.getIdentifier(),
                        container.getIdentifier(),
                        uuid
                );

        assertEquals(1, personalizedContentList.size());
        assertEquals(contentlet.getIdentifier(), personalizedContentList.get(0));
    }
}
