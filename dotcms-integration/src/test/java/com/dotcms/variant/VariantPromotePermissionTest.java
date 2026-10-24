package com.dotcms.variant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.api.v1.variants.ResponseEntityVariantView;
import com.dotcms.rest.api.v1.variants.VariantForm;
import com.dotcms.rest.api.v1.variants.VariantResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers the permission rules around promoting a {@link Variant}: {@link VariantAPIImpl#promote}
 * refuses, before touching anything, a user who lacks PUBLISH on the content or pages the
 * promotion would replace, and {@link VariantResource} only serves users with access to the
 * Experiments tool.
 */
public class VariantPromotePermissionTest {

    private static final int EDIT = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE;
    private static final int PUBLISH = EDIT | PermissionAPI.PERMISSION_PUBLISH;

    private static Host site;
    private static Field titleField;
    private static ContentType contentType;
    private static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        site = new SiteDataGen().nextPersisted();
        titleField = new FieldDataGen().type(TextField.class).name("title")
                .velocityVarName("title").next();
        contentType = new ContentTypeDataGen().host(site).field(titleField).nextPersisted();
        adminUser = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com",
                APILocator.systemUser(), false);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * Given Scenario: A back-end user with no permission on the contentlet that has a version in
     *                 the variant promotes it.
     * ExpectedResult: A {@link DotSecurityException}; the DEFAULT working version is unchanged.
     */
    @Test
    public void promoteWithoutPermissionOnContent_shouldBeDenied() throws Exception {
        final User user = backendUser();
        final Contentlet contentlet = new ContentletDataGen(contentType).host(site)
                .setProperty(titleField.variable(), "default").nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();
        ContentletDataGen.createNewVersion(contentlet, variant,
                Map.of(titleField.variable(), "variant"));

        assertDenied(variant, user);
        assertEquals("default", workingTitle(contentlet, VariantAPI.DEFAULT_VARIANT));
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * Given Scenario: The contentlet has a live version in the variant and the user has EDIT but
     *                 not PUBLISH on it.
     * ExpectedResult: A {@link DotSecurityException}; neither DEFAULT version is changed.
     */
    @Test
    public void promoteLiveContentWithEditOnly_shouldBeDenied() throws Exception {
        final User user = backendUser();
        final Contentlet contentlet = new ContentletDataGen(contentType).host(site)
                .setProperty(titleField.variable(), "default").nextPersistedAndPublish();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Contentlet variantVersion = ContentletDataGen.createNewVersion(contentlet, variant,
                Map.of(titleField.variable(), "variant"));
        APILocator.getContentletAPI().publish(variantVersion, APILocator.systemUser(), false);
        grant(contentlet, user, EDIT);

        assertDenied(variant, user);
        assertEquals("default", workingTitle(contentlet, VariantAPI.DEFAULT_VARIANT));
        assertEquals("default", liveTitle(contentlet, VariantAPI.DEFAULT_VARIANT));
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * Given Scenario: The contentlet only has a working version in the variant and the user has
     *                 EDIT on it.
     * ExpectedResult: The variant is promoted and the DEFAULT working version carries its value.
     */
    @Test
    public void promoteWorkingContentWithEdit_shouldSucceed() throws Exception {
        final User user = backendUser();
        final Contentlet contentlet = new ContentletDataGen(contentType).host(site)
                .setProperty(titleField.variable(), "default").nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Contentlet variantVersion = ContentletDataGen.createNewVersion(contentlet, variant,
                Map.of(titleField.variable(), "variant"));
        grant(contentlet, user, EDIT);
        unlock(contentlet, variantVersion);

        APILocator.getVariantAPI().promote(variant, user);

        assertEquals("variant", workingTitle(contentlet, VariantAPI.DEFAULT_VARIANT));
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * Given Scenario: The contentlet has a live version in the variant and the user has PUBLISH
     *                 on it.
     * ExpectedResult: The variant is promoted; the DEFAULT live version carries its value.
     */
    @Test
    public void promoteLiveContentWithPublish_shouldSucceed() throws Exception {
        final User user = backendUser();
        final Contentlet contentlet = new ContentletDataGen(contentType).host(site)
                .setProperty(titleField.variable(), "default").nextPersistedAndPublish();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Contentlet variantVersion = ContentletDataGen.createNewVersion(contentlet, variant,
                Map.of(titleField.variable(), "variant"));
        APILocator.getContentletAPI().publish(variantVersion, APILocator.systemUser(), false);
        grant(contentlet, user, PUBLISH);
        unlock(contentlet, variantVersion);

        APILocator.getVariantAPI().promote(variant, user);

        assertEquals("variant", liveTitle(contentlet, VariantAPI.DEFAULT_VARIANT));
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * Given Scenario: The variant holds no content versions, only a page layout (multi-tree) for a
     *                 page the user cannot PUBLISH.
     * ExpectedResult: A {@link DotSecurityException}; the page's DEFAULT layout is untouched.
     */
    @Test
    public void promoteLayoutWithoutPublishOnPage_shouldBeDenied() throws Exception {
        final User user = backendUser();
        final PageWithLayouts fixture = pageWithDefaultAndVariantLayout();

        assertDenied(fixture.variant, user);

        final List<MultiTree> defaultLayout = defaultLayoutOf(fixture.page);
        assertEquals(1, defaultLayout.size());
        assertEquals(fixture.defaultContent.getIdentifier(), defaultLayout.get(0).getContentlet());
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * Given Scenario: Same layout-only variant, but the user has PUBLISH on the page.
     * ExpectedResult: The variant's layout replaces the page's DEFAULT layout.
     */
    @Test
    public void promoteLayoutWithPublishOnPage_shouldSucceed() throws Exception {
        final User user = backendUser();
        final PageWithLayouts fixture = pageWithDefaultAndVariantLayout();
        grant(fixture.page, user, PUBLISH);

        APILocator.getVariantAPI().promote(fixture.variant, user);

        final List<MultiTree> defaultLayout = defaultLayoutOf(fixture.page);
        assertEquals(1, defaultLayout.size());
        assertEquals(fixture.variantContent.getIdentifier(), defaultLayout.get(0).getContentlet());
    }

    /**
     * Method to test: {@link VariantResource#addVariant(HttpServletRequest, javax.servlet.http.HttpServletResponse, VariantForm)}
     * Given Scenario: A back-end user without the Experiments tool in any of their layouts calls
     *                 the resource.
     * ExpectedResult: The request is rejected by the tool gate; no variant is created.
     */
    @Test
    public void addVariantWithoutExperimentsTool_shouldBeRejected() throws Exception {
        final User user = backendUser();
        final String name = "variant-" + System.nanoTime();

        try {
            new VariantResource().addVariant(requestFor(user), new MockHttpResponse().response(),
                    new VariantForm(name, "no tool"));
            fail("A user without the Experiments tool should not be able to create a variant");
        } catch (final com.dotcms.rest.exception.SecurityException e) {
            // expected: the InitBuilder tool gate refuses the request
        }

        assertTrue(APILocator.getVariantAPI().get(name).isEmpty());
    }

    /**
     * Method to test: {@link VariantResource#addVariant(HttpServletRequest, javax.servlet.http.HttpServletResponse, VariantForm)}
     * Given Scenario: A back-end user whose role has a layout with the Experiments tool, and a
     *                 CMS Administrator, each call the resource.
     * ExpectedResult: Both create their variant.
     */
    @Test
    public void addVariantWithExperimentsTool_shouldSucceed() throws Exception {
        final Layout layout = new LayoutDataGen()
                .portletIds(PortletID.EXPERIMENTS.toString()).nextPersisted();
        final Role role = new RoleDataGen().layout(layout).nextPersisted();
        final User experimentsUser = new UserDataGen()
                .roles(role, APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();

        final String byUser = "variant-" + System.nanoTime();
        final ResponseEntityVariantView userView = new VariantResource().addVariant(
                requestFor(experimentsUser), new MockHttpResponse().response(),
                new VariantForm(byUser, "with tool"));
        assertNotNull(userView.getEntity());
        assertTrue(APILocator.getVariantAPI().get(byUser).isPresent());

        final String byAdmin = "variant-" + System.nanoTime();
        new VariantResource().addVariant(requestFor(adminUser), new MockHttpResponse().response(),
                new VariantForm(byAdmin, "admin"));
        assertTrue(APILocator.getVariantAPI().get(byAdmin).isPresent());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** A page, its DEFAULT layout content, and a variant whose layout points at other content. */
    private static final class PageWithLayouts {
        private HTMLPageAsset page;
        private Contentlet defaultContent;
        private Contentlet variantContent;
        private Variant variant;
    }

    /**
     * Builds a page whose DEFAULT layout holds one contentlet and whose layout in a fresh variant
     * holds a different contentlet. Neither the page nor the contentlets have a version in the
     * variant, so promoting it only replaces the layout.
     */
    private static PageWithLayouts pageWithDefaultAndVariantLayout() {
        final PageWithLayouts fixture = new PageWithLayouts();
        fixture.variant = new VariantDataGen().nextPersisted();
        fixture.defaultContent = new ContentletDataGen(contentType).host(site)
                .setProperty(titleField.variable(), "in default layout").nextPersisted();
        fixture.variantContent = new ContentletDataGen(contentType).host(site)
                .setProperty(titleField.variable(), "in variant layout").nextPersisted();

        final Container container = new ContainerDataGen().site(site).nextPersisted();
        final Template template = new TemplateDataGen().site(site)
                .withContainer(container.getIdentifier()).nextPersisted();
        fixture.page = new HTMLPageDataGen(site, template).nextPersisted();

        new MultiTreeDataGen().setPage(fixture.page).setContainer(container)
                .setContentlet(fixture.defaultContent).setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();
        new MultiTreeDataGen().setPage(fixture.page).setContainer(container)
                .setContentlet(fixture.variantContent).setVariant(fixture.variant)
                .nextPersisted();
        return fixture;
    }

    private static List<MultiTree> defaultLayoutOf(final HTMLPageAsset page)
            throws DotDataException {
        return APILocator.getMultiTreeAPI()
                .getMultiTreesByVariant(page.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name());
    }

    private static void assertDenied(final Variant variant, final User user) throws Exception {
        try {
            APILocator.getVariantAPI().promote(variant, user);
            fail("Promoting the variant should have been denied");
        } catch (final DotSecurityException e) {
            assertTrue(e.getMessage().contains(variant.name()));
        }
    }

    private static String workingTitle(final Contentlet contentlet, final Variant variant)
            throws DotDataException, DotSecurityException {
        return title(contentlet, false, variant);
    }

    private static String liveTitle(final Contentlet contentlet, final Variant variant)
            throws DotDataException, DotSecurityException {
        return title(contentlet, true, variant);
    }

    private static String title(final Contentlet contentlet, final boolean live,
            final Variant variant) throws DotDataException, DotSecurityException {
        final Contentlet found = APILocator.getContentletAPI().findContentletByIdentifier(
                contentlet.getIdentifier(), live, contentlet.getLanguageId(), variant.name(),
                APILocator.systemUser(), false);
        assertNotNull(found);
        return found.getStringProperty(titleField.variable());
    }

    /**
     * Releases the locks the system user holds after creating the fixtures, so a limited user
     * can check the versions out during promotion (a lock held by someone else refuses it).
     */
    private static void unlock(final Contentlet... versions)
            throws DotDataException, DotSecurityException {
        for (final Contentlet version : versions) {
            APILocator.getContentletAPI().unlock(version, APILocator.systemUser(), false);
        }
    }

    /** A back-end user with a fresh role and no permissions on anything. */
    private static User backendUser() throws DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        return new UserDataGen()
                .roles(role, APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
    }

    /** Gives the user's own role the given bits on the asset as an individual permission. */
    private static void grant(final Permissionable asset, final User user, final int bits)
            throws DotDataException, DotSecurityException {
        final Role userRole = APILocator.getRoleAPI().loadRoleByKey(user.getUserId());
        final Permission permission = new Permission(asset.getPermissionId(), userRole.getId(),
                bits, true);
        APILocator.getPermissionAPI().save(permission, asset, APILocator.systemUser(), false);
    }

    /** A request whose session carries the given user, the way a logged-in call does. */
    private static HttpServletRequest requestFor(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(site.getHostname(), "/")
                                        .request()).request()).request());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID,
                user.getUserId());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }
}
