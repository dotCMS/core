package com.dotmarketing.portlets.structure.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the {@code cmd=reorder} branch of {@link EditFieldAction}, driven through
 * {@link EditFieldAction#processAction} with stored permission rows: the posted
 * {@code order_<field inode>} values are only saved when every field belongs to one content type
 * and the user has {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS} on it, whatever portlets the
 * user has.
 */
public class EditFieldActionReorderTest {

    private static final String CONTENT_TYPES_PORTLET = "content-types";
    private static final String ERROR_MESSAGE = "error";
    private static final String SUCCESS_MESSAGE = "message";
    private static final String REFERER = "/c/portal/layout?p_l_id=content-types";

    private static final List<ContentType> createdTypes = new ArrayList<>();

    private static PermissionAPI permissionAPI;
    private static User systemUser;
    private static Role ownerRole;
    private static Layout contentTypesLayout;
    private static User userWithoutPortlets;
    private static User contentTypesPortletUser;
    private static User editor;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        systemUser = APILocator.systemUser();
        final RoleAPI roleAPI = APILocator.getRoleAPI();

        final String suffix = String.valueOf(System.nanoTime());
        ownerRole = new RoleDataGen().name("editFieldOwner" + suffix)
                .key("editFieldOwner" + suffix).nextPersisted();
        contentTypesLayout = new LayoutDataGen()
                .name("content-types-" + suffix)
                .portletIds(CONTENT_TYPES_PORTLET)
                .nextPersisted();

        userWithoutPortlets = backendUser("reorder-no-portlets");
        contentTypesPortletUser = backendUser("reorder-content-types");
        roleAPI.addLayoutToRole(contentTypesLayout, roleAPI.getUserRole(contentTypesPortletUser));
        editor = backendUser("reorder-editor");
        roleAPI.addLayoutToRole(contentTypesLayout, roleAPI.getUserRole(editor));

        assertTrue(APILocator.getLayoutAPI().loadLayoutsForUser(userWithoutPortlets).isEmpty());
        assertTrue(APILocator.getLayoutAPI()
                .doesUserHaveAccessToPortlet(CONTENT_TYPES_PORTLET, contentTypesPortletUser));
    }

    /** One step of the class cleanup. */
    @FunctionalInterface
    private interface CleanupStep {
        void run() throws Exception;
    }

    /**
     * Removes the content types, deletes the test users (handing whatever they own to the admin
     * user), the owner role and the test layout, reloaded first because the persisted instance is
     * detached from the current session. Every step runs even when an earlier one fails; the first
     * failure is rethrown at the end with the others attached.
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        final List<Exception> failures = new ArrayList<>();
        runCleanupStep(failures, () -> createdTypes.forEach(ContentTypeDataGen::remove));
        for (final User user : new User[]{userWithoutPortlets, contentTypesPortletUser, editor}) {
            if (null != user) {
                runCleanupStep(failures, () -> APILocator.getUserAPI()
                        .delete(user, TestUserUtils.getAdminUser(), systemUser, false));
            }
        }
        if (null != ownerRole) {
            runCleanupStep(failures, () -> RoleDataGen.remove(ownerRole));
        }
        if (null != contentTypesLayout) {
            runCleanupStep(failures, () -> {
                final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
                layoutAPI.removeLayout(layoutAPI.findLayout(contentTypesLayout.getId()));
            });
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
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a back-end user with no portlets and no permission on the content type posts a new
     * order for its fields, with a referer to return to
     * Expected: no sort order changes, an error message, no success message and no redirect to
     * the referer
     */
    @Test
    public void reorder_userWithoutPortletsOrPermissions_changesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 3);
        final Map<String, Integer> before = storedSortOrders(fields);

        final ActionRequestImpl request = reorderRequest(userWithoutPortlets, newOrder(reversed(fields)));
        when(request.getParameter("referer")).thenReturn(REFERER);
        final ActionResponse response = mock(ActionResponse.class);
        new EditFieldAction().processAction(null, null, null, request, response);

        assertEquals(before, storedSortOrders(fields));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a back-end user with the Content Types portlet who can read the content type but has
     * no EDIT_PERMISSIONS on it posts a new order for its fields, with a referer to return to
     * Expected: no sort order changes, an error message, no success message and no redirect to
     * the referer
     */
    @Test
    public void reorder_contentTypesPortletUserWithoutEditPermissions_changesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 3);
        final Map<String, Integer> before = storedSortOrders(fields);

        final ActionRequestImpl request = reorderRequest(contentTypesPortletUser, newOrder(reversed(fields)));
        when(request.getParameter("referer")).thenReturn(REFERER);
        final ActionResponse response = mock(ActionResponse.class);
        new EditFieldAction().processAction(null, null, null, request, response);

        assertEquals(before, storedSortOrders(fields));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user with READ, WRITE and PUBLISH on the content type, but not EDIT_PERMISSIONS,
     * posts a new order for its fields, with a referer to return to
     * Expected: no sort order changes, an error message, no success message and no redirect to
     * the referer
     */
    @Test
    public void reorder_withWriteAndPublishButNotEditPermissions_changesNothing() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_WRITE, editor, true));
        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_PUBLISH, editor, true));
        final List<Field> fields = textFields(type, 3);
        final Map<String, Integer> before = storedSortOrders(fields);

        final ActionRequestImpl request = reorderRequest(editor, newOrder(reversed(fields)));
        when(request.getParameter("referer")).thenReturn(REFERER);
        final ActionResponse response = mock(ActionResponse.class);
        new EditFieldAction().processAction(null, null, null, request, response);

        assertEquals(before, storedSortOrders(fields));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user with EDIT_PERMISSIONS on two content types posts fields of both types in one
     * request
     * Expected: no field of either type changes, an error message and no success message
     */
    @Test
    public void reorder_fieldsOfTwoTypes_changesNothing() throws Exception {
        final ContentType typeA = newContentType();
        final ContentType typeB = newContentType();
        final List<Field> posted = new ArrayList<>(reversed(textFields(typeA, 2)));
        posted.addAll(textFields(typeB, 1));
        final Map<String, Integer> before = storedSortOrders(posted);

        final ActionRequestImpl request = reorderRequest(editor, newOrder(posted));
        new EditFieldAction().processAction(null, null, null, request, mock(ActionResponse.class));

        assertEquals(before, storedSortOrders(posted));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user posts first the fields of a content type on which they have EDIT_PERMISSIONS,
     * then a field of a content type they can only read
     * Expected: no field of either type changes, an error message and no success message
     */
    @Test
    public void reorder_editableTypeFirstThenFieldOfReadOnlyType_changesNothing() throws Exception {
        final ContentType editableType = newContentType();
        final ContentType readOnlyType = newContentTypeEditorCanOnlyRead();
        final List<Field> posted = new ArrayList<>(reversed(textFields(editableType, 2)));
        posted.addAll(textFields(readOnlyType, 1));
        final Map<String, Integer> before = storedSortOrders(posted);

        final ActionRequestImpl request = reorderRequest(editor, newOrder(posted));
        new EditFieldAction().processAction(null, null, null, request, mock(ActionResponse.class));

        assertEquals(before, storedSortOrders(posted));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user posts first a field of a content type they can only read, then the fields of
     * a content type on which they have EDIT_PERMISSIONS
     * Expected: the request is denied on the first field's type: no field of either type changes,
     * an error message and no success message
     */
    @Test
    public void reorder_readOnlyTypeFirstThenFieldsOfEditableType_changesNothing() throws Exception {
        final ContentType readOnlyType = newContentTypeEditorCanOnlyRead();
        final ContentType editableType = newContentType();
        final List<Field> posted = new ArrayList<>(textFields(readOnlyType, 1));
        posted.addAll(reversed(textFields(editableType, 2)));
        final Map<String, Integer> before = storedSortOrders(posted);

        final ActionRequestImpl request = reorderRequest(editor, newOrder(posted));
        new EditFieldAction().processAction(null, null, null, request, mock(ActionResponse.class));

        assertEquals(before, storedSortOrders(posted));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user with EDIT_PERMISSIONS on the content type posts a valid order for the first
     * field and a non-numeric order for the second one
     * Expected: no field changes, an error message and no success message
     */
    @Test
    public void reorder_nonNumericOrder_changesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 2);
        final Map<String, Integer> before = storedSortOrders(fields);
        final Map<String, String> posted = new LinkedHashMap<>();
        posted.put(fields.get(0).id(), "50");
        posted.put(fields.get(1).id(), "first");

        final ActionRequestImpl request = reorderRequest(editor, posted);
        new EditFieldAction().processAction(null, null, null, request, mock(ActionResponse.class));

        assertEquals(before, storedSortOrders(fields));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user with EDIT_PERMISSIONS on the content type posts a new order for its fields
     * plus an order for an inode that is no field, with a referer to return to
     * Expected: no field changes, an error message, no success message and no redirect to the
     * referer
     */
    @Test
    public void reorder_unknownFieldInode_changesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 2);
        final Map<String, Integer> before = storedSortOrders(fields);
        final Map<String, String> posted = newOrder(reversed(fields));
        posted.put(UUID.randomUUID().toString(), "30");

        final ActionRequestImpl request = reorderRequest(editor, posted);
        when(request.getParameter("referer")).thenReturn(REFERER);
        final ActionResponse response = mock(ActionResponse.class);
        new EditFieldAction().processAction(null, null, null, request, response);

        assertEquals(before, storedSortOrders(fields));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user with EDIT_PERMISSIONS on the content type posts first an order for an inode
     * that is no field, then a new order for the type's fields, with a referer to return to (the
     * content type is taken from the first posted field)
     * Expected: no field changes, an error message, no success message and no redirect to the
     * referer
     */
    @Test
    public void reorder_unknownFieldInodeFirst_changesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 2);
        final Map<String, Integer> before = storedSortOrders(fields);
        final Map<String, String> posted = new LinkedHashMap<>();
        posted.put(UUID.randomUUID().toString(), "30");
        posted.putAll(newOrder(reversed(fields)));

        final ActionRequestImpl request = reorderRequest(editor, posted);
        when(request.getParameter("referer")).thenReturn(REFERER);
        final ActionResponse response = mock(ActionResponse.class);
        new EditFieldAction().processAction(null, null, null, request, response);

        assertEquals(before, storedSortOrders(fields));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user with EDIT_PERMISSIONS on a content type posts no sort order at all, with a
     * referer to return to
     * Expected: no field changes, an error message, no success message and no redirect to the
     * referer
     */
    @Test
    public void reorder_emptyPost_changesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 2);
        final Map<String, Integer> before = storedSortOrders(fields);

        final ActionRequestImpl request = reorderRequest(editor, Collections.emptyMap());
        when(request.getParameter("referer")).thenReturn(REFERER);
        final ActionResponse response = mock(ActionResponse.class);
        new EditFieldAction().processAction(null, null, null, request, response);

        assertEquals(before, storedSortOrders(fields));
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Method to test: {@link EditFieldAction#processAction} with {@code cmd=reorder}
     * Given: a user with the Content Types portlet and EDIT_PERMISSIONS on the content type posts a
     * new order for its fields
     * Expected: every field stored with its new sort order, the success message and no error
     */
    @Test
    public void reorder_withEditPermissions_savesNewOrder() throws Exception {
        final ContentType type = newContentType();
        final List<Field> newOrder = reversed(textFields(type, 3));
        final Map<String, String> posted = newOrder(newOrder);

        final ActionRequestImpl request = reorderRequest(editor, posted);
        new EditFieldAction().processAction(null, null, null, request, mock(ActionResponse.class));

        final Map<String, Integer> expected = new LinkedHashMap<>();
        posted.forEach((fieldId, sortOrder) -> expected.put(fieldId, Integer.parseInt(sortOrder)));
        assertEquals(expected, storedSortOrders(newOrder));
        assertTrue(SessionMessages.contains(request, SUCCESS_MESSAGE));
        assertFalse(SessionMessages.contains(request, ERROR_MESSAGE));
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    /**
     * A new content type with its own permissions: everything for a separate role, READ and
     * EDIT_PERMISSIONS for the editor, READ for the Content Types portlet user, nothing for the
     * user without portlets.
     */
    private static ContentType newContentType() throws DotDataException, DotSecurityException {
        return newContentType(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
    }

    /** Like {@link #newContentType()}, but the editor can only read the type. */
    private static ContentType newContentTypeEditorCanOnlyRead() throws DotDataException, DotSecurityException {
        return newContentType(PermissionAPI.PERMISSION_READ);
    }

    /**
     * A new content type with its own permissions: everything for a separate role, the given
     * permission for the editor (which must include READ), READ for the Content Types portlet
     * user, nothing for the user without portlets.
     */
    private static ContentType newContentType(final int editorPermission)
            throws DotDataException, DotSecurityException {
        final String suffix = String.valueOf(System.nanoTime());
        final ContentType type = new ContentTypeDataGen()
                .name("EditFieldReorderType" + suffix)
                .velocityVarName("editFieldReorderType" + suffix)
                .nextPersisted();
        createdTypes.add(type);

        final Host site = APILocator.getHostAPI().find(type.host(), systemUser, false);
        permissionAPI.permissionIndividually(site, type, systemUser);
        permissionAPI.removePermissions(type);
        savePermission(type, ownerRole, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                | PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        savePermission(type, APILocator.getRoleAPI().getUserRole(editor), editorPermission);
        savePermission(type, APILocator.getRoleAPI().getUserRole(contentTypesPortletUser),
                PermissionAPI.PERMISSION_READ);

        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, editor, true));
        assertEquals((editorPermission & PermissionAPI.PERMISSION_EDIT_PERMISSIONS) != 0,
                permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, editor, true));
        assertFalse(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
                contentTypesPortletUser, true));
        assertFalse(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, userWithoutPortlets, true));
        return type;
    }

    private static void savePermission(final ContentType type, final Role role, final int permission)
            throws DotDataException, DotSecurityException {
        permissionAPI.save(new Permission(type.getPermissionId(), role.getId(), permission, true),
                type, systemUser, false);
    }

    /** Text fields of the type, stored with the sort orders 1..count. */
    private static List<Field> textFields(final ContentType type, final int count) {
        final List<Field> fields = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            final String variable = "field" + i + "x" + System.nanoTime();
            fields.add(new FieldDataGen().type(TextField.class)
                    .name(variable).velocityVarName(variable).sortOrder(i)
                    .contentTypeId(type.id()).nextPersisted());
        }
        return fields;
    }

    private static List<Field> reversed(final List<Field> fields) {
        final List<Field> reversed = new ArrayList<>(fields);
        Collections.reverse(reversed);
        return reversed;
    }

    /** Posted sort order per field id: 10, 20, 30... in the given order. */
    private static Map<String, String> newOrder(final List<Field> fields) {
        final Map<String, String> posted = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            posted.put(fields.get(i).id(), String.valueOf((i + 1) * 10));
        }
        return posted;
    }

    /**
     * A {@code cmd=reorder} request of the given user with one {@code order_<field id>} parameter
     * per entry, and a portlet session that keeps the messages the action adds.
     */
    private static ActionRequestImpl reorderRequest(final User user, final Map<String, String> sortOrders) {
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

        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getParameter("cmd")).thenReturn("reorder");
        final List<String> parameterNames = new ArrayList<>();
        sortOrders.forEach((fieldId, sortOrder) -> {
            parameterNames.add("order_" + fieldId);
            when(request.getParameter("order_" + fieldId)).thenReturn(sortOrder);
        });
        when(request.getParameterNames()).thenReturn(Collections.enumeration(parameterNames));
        return request;
    }

    /** Sort orders as stored in the database, by field id, in the given order. */
    private static Map<String, Integer> storedSortOrders(final List<Field> fields) throws DotDataException {
        final Map<String, Integer> sortOrders = new LinkedHashMap<>();
        for (final Field field : fields) {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("SELECT sort_order FROM field WHERE inode = ?");
            dotConnect.addParam(field.id());
            final List<Map<String, Object>> rows = dotConnect.loadObjectResults();
            sortOrders.put(field.id(), ((Number) rows.get(0).get("sort_order")).intValue());
        }
        return sortOrders;
    }
}
