package com.dotmarketing.portlets.categories.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.struts.CategoryForm;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for a {@code cmd=delete} request to {@link EditCategoryAction}, driven through
 * {@link EditCategoryAction#processAction} with stored categories, permissions and content: the
 * action has no delete command, so the request leaves the category, its links to its parent and to
 * content, and its permissions as they were, whatever permissions the user holds on it.
 */
public class EditCategoryActionDeleteTest {

    private static final List<Contentlet> createdContentlets = new ArrayList<>();
    private static final List<ContentType> createdTypes = new ArrayList<>();
    private static final List<Category> createdCategories = new ArrayList<>();

    private static PermissionAPI permissionAPI;
    private static User systemUser;
    private static Role ownerRole;
    private static User readOnlyUser;
    private static User editor;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        systemUser = APILocator.systemUser();

        final String suffix = String.valueOf(System.nanoTime());
        ownerRole = new RoleDataGen().name("categoryOwner" + suffix)
                .key("categoryOwner" + suffix).nextPersisted();
        readOnlyUser = backendUser("category-reader");
        editor = backendUser("category-editor");
    }

    /** One step of the class cleanup. */
    @FunctionalInterface
    private interface CleanupStep {
        void run() throws Exception;
    }

    /**
     * Removes the contentlets, the content types, the categories (children first), the test users
     * (handing whatever they own to the admin user) and the owner role. Objects that are already
     * gone are skipped. Every step runs even when an earlier one fails; the first failure is
     * rethrown at the end with the others attached.
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        final List<Exception> failures = new ArrayList<>();
        for (final Contentlet contentlet : createdContentlets) {
            runCleanupStep(failures, () -> {
                if (null != APILocator.getContentletAPI().find(contentlet.getInode(), systemUser, false)) {
                    APILocator.getContentletAPI().destroy(contentlet, systemUser, false);
                }
            });
        }
        runCleanupStep(failures, () -> createdTypes.forEach(ContentTypeDataGen::remove));
        final List<Category> categories = new ArrayList<>(createdCategories);
        Collections.reverse(categories);
        for (final Category category : categories) {
            runCleanupStep(failures, () -> CategoryDataGen.delete(category, true));
        }
        for (final User user : new User[]{readOnlyUser, editor}) {
            if (null != user) {
                runCleanupStep(failures, () -> {
                    if (APILocator.getUserAPI().userExistsWithEmail(user.getEmailAddress())) {
                        APILocator.getUserAPI()
                                .delete(user, TestUserUtils.getAdminUser(), systemUser, false);
                    }
                });
            }
        }
        if (null != ownerRole) {
            runCleanupStep(failures, () -> RoleDataGen.remove(ownerRole));
        }
        if (!failures.isEmpty()) {
            final Exception first = failures.get(0);
            failures.stream().skip(1).forEach(first::addSuppressed);
            throw first;
        }
    }

    private static void runCleanupStep(final List<Exception> failures, final CleanupStep step) {
        try {
            step.run();
        } catch (final Exception e) {
            failures.add(e);
        }
    }

    /**
     * Method to test: {@link EditCategoryAction#processAction} with {@code cmd=delete}
     * Given: a back-end user with no portlets who can read a category but not edit it; the
     * category has a parent and is assigned to a contentlet
     * Expected: the category row, its links to its parent and to the contentlet, and its
     * permission rows are unchanged, and the contentlet still lists the category
     */
    @Test
    public void delete_userWithReadPermissionOnly_keepsCategoryLinksAndPermissions() throws Exception {
        final CategoryInUse categoryInUse = categoryInUse(readOnlyUser, PermissionAPI.PERMISSION_READ);
        assertFalse(permissionAPI.doesUserHavePermission(categoryInUse.category,
                PermissionAPI.PERMISSION_EDIT, readOnlyUser, false));

        assertDeleteCommandChangesNothing(readOnlyUser, categoryInUse);
    }

    /**
     * Method to test: {@link EditCategoryAction#processAction} with {@code cmd=delete}
     * Given: a back-end user with no portlets who can read and edit a category; the category has
     * a parent and is assigned to a contentlet
     * Expected: the category row, its links to its parent and to the contentlet, and its
     * permission rows are unchanged, and the contentlet still lists the category
     */
    @Test
    public void delete_userWithEditPermission_keepsCategoryLinksAndPermissions() throws Exception {
        final CategoryInUse categoryInUse = categoryInUse(editor,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT);
        assertTrue(permissionAPI.doesUserHavePermission(categoryInUse.category,
                PermissionAPI.PERMISSION_EDIT, editor, false));

        assertDeleteCommandChangesNothing(editor, categoryInUse);
    }

    /**
     * Sends a {@code cmd=delete} request of the given user for the category and checks that its
     * stored rows and its assignment to the contentlet are the same afterwards.
     */
    private static void assertDeleteCommandChangesNothing(final User user,
            final CategoryInUse categoryInUse) throws Exception {
        final String inode = categoryInUse.category.getInode();
        final int treeRows = treeCount(inode);
        final int permissionRows = permissionCount(inode);
        assertTrue("the category is linked to its parent and to the contentlet", treeRows >= 2);
        assertTrue("the category has its own permission rows", permissionRows > 0);
        assertEquals(1, categoryCount(inode));

        new EditCategoryAction().processAction(mock(ActionMapping.class), new CategoryForm(), null,
                deleteRequest(user, inode), mock(ActionResponse.class));

        assertEquals(treeRows, treeCount(inode));
        assertEquals(permissionRows, permissionCount(inode));
        assertEquals(1, categoryCount(inode));
        assertTrue(APILocator.getCategoryAPI()
                .getParents(categoryInUse.contentlet, systemUser, false).stream()
                .anyMatch(parent -> inode.equals(parent.getInode())));
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    /** A child category and the contentlet it is assigned to. */
    private static final class CategoryInUse {
        private final Category category;
        private final Contentlet contentlet;

        private CategoryInUse(final Category category, final Contentlet contentlet) {
            this.category = category;
            this.contentlet = contentlet;
        }
    }

    /**
     * A new category under a new parent, assigned to a new contentlet through a category field,
     * with its own permissions: everything for the owner role and the given permission (which must
     * include READ) for the user.
     */
    private static CategoryInUse categoryInUse(final User user, final int userPermission)
            throws DotDataException, DotSecurityException {
        final Category parent = newCategory("parent", null);
        final Category child = newCategory("child", parent);

        final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, false);
        final String suffix = String.valueOf(System.nanoTime());
        ContentType type = APILocator.getContentTypeAPI(systemUser).save(
                ContentTypeBuilder.builder(SimpleContentType.class)
                        .name("EditCategoryDeleteType" + suffix)
                        .variable("EditCategoryDeleteType" + suffix)
                        .host(site.getIdentifier())
                        .build());
        createdTypes.add(type);
        APILocator.getContentTypeFieldAPI().save(FieldBuilder.builder(CategoryField.class)
                .name("categories" + suffix)
                .variable("categories" + suffix)
                .values(parent.getInode())
                .contentTypeId(type.id())
                .build(), systemUser);
        type = APILocator.getContentTypeAPI(systemUser).find(type.inode());

        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(type.id());
        contentlet.setHost(type.host());
        contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet = APILocator.getContentletAPI().checkin(contentlet,
                Collections.singletonList(child),
                permissionAPI.getPermissions(contentlet, false, true), systemUser, false);
        createdContentlets.add(contentlet);

        savePermission(child, ownerRole, PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        savePermission(child, APILocator.getRoleAPI().loadRoleByKey(user.getUserId()), userPermission);
        assertTrue(permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_READ, user, false));
        return new CategoryInUse(child, contentlet);
    }

    private static Category newCategory(final String prefix, final Category parent) {
        final String name = prefix + System.nanoTime();
        final Category category = new CategoryDataGen()
                .setCategoryName(name)
                .setKey(name)
                .setCategoryVelocityVarName(name)
                .parent(parent)
                .nextPersisted();
        createdCategories.add(category);
        return category;
    }

    private static void savePermission(final Category category, final Role role, final int permission)
            throws DotDataException, DotSecurityException {
        permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                category.getPermissionId(), role.getId(), permission, true), category, systemUser, false);
    }

    /**
     * A {@code cmd=delete} request of the given user for the category, with an attribute store
     * the action reads back and a portlet session that keeps the messages the action adds.
     */
    private static ActionRequestImpl deleteRequest(final User user, final String inode) {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(WebKeys.USER)).thenReturn(user);

        final Map<String, Object> sessionAttributes = new HashMap<>();
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(invocation -> sessionAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(httpSession).setAttribute(anyString(), any());
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);
        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final Map<String, Object> requestAttributes = new HashMap<>();
        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getParameter(Constants.CMD)).thenReturn(Constants.DELETE);
        when(request.getParameter("inode")).thenReturn(inode);
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> requestAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return request;
    }

    private static int treeCount(final String inode) throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as test from tree where parent = ? or child = ?")
                .addParam(inode)
                .addParam(inode)
                .getInt("test");
    }

    private static int permissionCount(final String inode) throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as test from permission where inode_id = ?")
                .addParam(inode)
                .getInt("test");
    }

    private static int categoryCount(final String inode) throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as test from category where inode = ?")
                .addParam(inode)
                .getInt("test");
    }
}
