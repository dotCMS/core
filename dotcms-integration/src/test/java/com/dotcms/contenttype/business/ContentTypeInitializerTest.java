package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.org.directwebremoting.guice.ApplicationScoped;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.FireActionForm;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import org.junit.runner.RunWith;

/**
 * Test for the {@link ContentTypeInitializer}
 * @author jsanca
 */
public class ContentTypeInitializerTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#search(String, int, int, String, User, boolean)}
     * Given Scenario: Creates a few favorite pages
     * ExpectedResult: On search, since favorite pages are system content, they should not be returned
     *
     */
    @Test
    public void test_system_field() throws Exception {

        final ContentType contentType = Try.of(()-> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();
        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();
        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();

        final StringBuffer luceneQuery = new StringBuffer();
        luceneQuery.append("+contentType:"+contentType.variable() + " ");
        luceneQuery.append("+" + ESMappingConstants.SYSTEM_TYPE + ":false ");
        final List<Contentlet> contentlets = APILocator.getContentletAPI().search(luceneQuery.toString(),
                0, 10, null, APILocator.systemUser(), false);

        Assert.assertNotNull("The contentlets shouldn't be null", contentlets);
        Assert.assertTrue("Should not return any contentlet since they are system contentlets",contentlets.isEmpty());
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: If the content type exists is being deleted, and try the initializer to see if works
     * ExpectedResult: The content type should be created
     *
     */
    @Test
    public void test_content_type_init() throws Exception {
        // make sure its cached. see https://github.com/dotCMS/dotCMS/issues/2465
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        ContentType contentType = Try.of(()-> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        if (null != contentType) {
            contentTypeAPI.delete(contentType);
        }

        new ContentTypeInitializer().init();

        contentType = Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        Assert.assertNotNull("The content type dotFavoritePage shouldn't be null", contentType);

        Assert.assertTrue("CT : "+contentType, contentType.fieldMap().get("order").indexed());
        //we make sure the url field isn't unique
        Assert.assertFalse("CT : "+contentType,contentType.fieldMap().get("url").unique());

        //we make sure there is no content type using the legacy name
        Assert.assertNull(Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.LEGACY_FAVORITE_PAGE_VAR_NAME)).getOrNull());

        checkPermissionsOnContentlet(contentType);
    }

    /** USE, EDIT, PUBLISH and EDIT_PERMISSIONS: the bits the initializer gives CMS Owner. */
    private static final int OWNER_BITS = PermissionAPI.PERMISSION_USE | PermissionAPI.PERMISSION_EDIT
            | PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;

    /**
     * Method to test: {@link ContentTypeInitializer#init()} and saving a favorite page through
     * {@link WorkflowResource#fireActionDefaultSinglePart}, the call the Favorite Page dialog makes.
     * Given Scenario: The favorite page content type is in the state the starter ships: an
     * individual CMS Owner row and no row its contentlets inherit. A back-end user saves one
     * favorite before the initializer runs and one after, without a permission list.
     * ExpectedResult: After the initializer runs, both favorites can be read by their owner only:
     * not anonymously, not by a front-end user and not by another back-end user.
     */
    @Test
    public void test_favorite_pages_are_readable_only_by_their_owner() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.save(new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                contentType, APILocator.systemUser(), false);

        final String password = "favorite-" + System.currentTimeMillis();
        final User owner = new UserDataGen().active(true).password(password)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User otherBackEndUser = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User frontEndUser = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();
        final User anonymous = APILocator.getUserAPI().getAnonymousUser();
        final List<Contentlet> favorites = new ArrayList<>();
        try {

            final Contentlet savedBefore = saveFavoriteAs(owner, password);
            favorites.add(savedBefore);
            Assert.assertTrue("In the starter state a favorite inherits anonymous READ from the System Host,"
                            + " which is the state the initializer has to repair",
                    permissionAPI.doesUserHavePermission(savedBefore, PermissionAPI.PERMISSION_READ, anonymous, true));

            // The check above cached its answer; the initializer must drop it when it repairs.
            new ContentTypeInitializer().init();

            final Contentlet savedAfter = saveFavoriteAs(owner, password);
            favorites.add(savedAfter);
            for (final Contentlet favorite : favorites) {

                Assert.assertTrue("The owner must read the favorite",
                        permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ, owner, true));
                for (final User reader : List.of(anonymous, frontEndUser, otherBackEndUser)) {

                    if (permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ, reader, true)) {

                        Assert.fail(reader.getUserId() + " must not read a favorite owned by someone else: "
                                + describePermissions(favorite, favorite == savedBefore ? "saved before init" : "saved after init"));
                    }
                }
            }

            final String query = "+contentType:" + ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME
                    + " +identifier:" + savedAfter.getIdentifier();
            Assert.assertEquals("The owner must find the favorite in a content search", 1,
                    APILocator.getContentletAPI().search(query, 10, 0, null, owner, true).size());
            for (final User reader : List.of(anonymous, frontEndUser, otherBackEndUser)) {

                Assert.assertTrue(reader.getUserId() + " must not find a favorite owned by someone else",
                        APILocator.getContentletAPI().search(query, 10, 0, null, reader, true).isEmpty());
            }

            contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
            final String cmsOwnerId = cmsOwner.getId();
            Assert.assertTrue("The favorite page content type must pass CMS Owner to its contentlets",
                    permissionAPI.getInheritablePermissions(contentType, true).stream().anyMatch(permission ->
                            Contentlet.class.getCanonicalName().equals(permission.getType())
                                    && cmsOwnerId.equals(permission.getRoleId())
                                    && (permission.getPermission() & OWNER_BITS) == OWNER_BITS));
        } finally {

            favorites.forEach(ContentletDataGen::destroy);
            UserDataGen.remove(owner);
            UserDataGen.remove(otherBackEndUser);
            UserDataGen.remove(frontEndUser);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorite page content type already has both CMS Owner rows, plus a READ
     * row for CMS Anonymous that its contentlets inherit.
     * ExpectedResult: The initializer replaces the type's rows, so the anonymous row is gone and a
     * favorite saved afterwards cannot be read anonymously.
     */
    @Test
    public void test_init_removes_rows_of_other_roles_from_favorite_page_type() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        anonymousRole.getId(), PermissionAPI.PERMISSION_READ, true)),
                contentType, APILocator.systemUser(), false);

        new ContentTypeInitializer().init();

        final String anonymousRoleId = anonymousRole.getId();
        Assert.assertTrue("The favorite page content type must not pass CMS Anonymous to its contentlets",
                permissionAPI.getInheritablePermissions(contentType, true).stream()
                        .noneMatch(permission -> anonymousRoleId.equals(permission.getRoleId())));

        final String password = "favorite-" + System.currentTimeMillis();
        final User owner = new UserDataGen().active(true).password(password)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        Contentlet favorite = null;
        try {

            favorite = saveFavoriteAs(owner, password);
            Assert.assertFalse("A favorite must not be readable anonymously",
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ,
                            APILocator.getUserAPI().getAnonymousUser(), true));
        } finally {

            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            UserDataGen.remove(owner);
        }
    }

    /**
     * Describes where a favorite's permissions come from, for a failure message: the favorite, the
     * permissions it resolves to, its own rows and its permission reference in the database.
     */
    private static String describePermissions(final Contentlet favorite, final String label)
            throws DotDataException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        return label + " " + favorite.getIdentifier()
                + ", resolved=" + permissionAPI.getPermissions(favorite)
                + ", own rows=" + new DotConnect().setSQL("select roleid, permission, permission_type from permission"
                        + " where inode_id = ?").addParam(favorite.getPermissionId()).loadObjectResults()
                + ", reference=" + new DotConnect().setSQL("select reference_id, permission_type from"
                        + " permission_reference where asset_id = ?").addParam(favorite.getPermissionId())
                        .loadObjectResults()
                + ", parent=" + Try.of(() -> favorite.getParentPermissionable().getPermissionId()).getOrNull();
    }

    /**
     * Publishes a new favorite page through {@link WorkflowResource#fireActionDefaultSinglePart}
     * with Basic authentication for the given user and the fields the Favorite Page dialog sends,
     * without a permission list, and returns the saved contentlet.
     */
    @SuppressWarnings("unchecked")
    private static Contentlet saveFavoriteAs(final User user, final String password) throws Exception {

        final MockHeaderRequest request = new MockHeaderRequest(new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request()).request());
        request.setHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((user.getEmailAddress() + ":" + password).getBytes(StandardCharsets.UTF_8)));
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        try {

            final FireActionForm form = new FireActionForm(new FireActionForm.Builder().contentlet(Map.of(
                    "contentType", ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME,
                    "title", "favorite",
                    "url", "/favorite-" + System.nanoTime() + "?language_id=1",
                    "order", 1)));
            final Response response = new WorkflowResource().fireActionDefaultSinglePart(request,
                    new EmptyHttpResponse(), null, null, "WAIT_FOR",
                    String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId()),
                    "DEFAULT", SystemAction.PUBLISH, form);
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            final Map<String, Object> saved =
                    (Map<String, Object>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
            return APILocator.getContentletAPI().find(String.valueOf(saved.get("inode")),
                    APILocator.systemUser(), false);
        } finally {

            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

    private static void checkPermissionsOnContentlet(final ContentType contentType) throws DotDataException {
        final Role backendRole = APILocator.getRoleAPI().loadCMSOwnerRole();
        final List<Permission> permissions = APILocator.getPermissionAPI().getPermissions(contentType);
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_USE && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_EDIT  && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_PUBLISH  && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_EDIT_PERMISSIONS && p.getRoleId().equals(backendRole.getId())));
    }
}
