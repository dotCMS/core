package com.dotmarketing.portlets.structure.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.struts.StructureForm;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for saving a content type through {@link EditStructureAction#processAction}
 * with {@code cmd=add}, with stored permission rows: an existing type is only saved, moved or given
 * other workflow schemes by a user with EDIT_PERMISSIONS on it, and a type is only created on, or
 * moved to, a site where the user can add children and edit content type permissions.
 */
public class EditStructureActionSaveTest {

    /** Session key of the error message the action shows. */
    private static final String ERROR_KEY = "error";
    /** Session key of the action's other messages: the success message or a warning. */
    private static final String MESSAGE_KEY = "message";
    /** The message shown when the content type was saved. */
    private static final String SAVED_MESSAGE = "message.structure.savestructure";
    /** The message shown when the content type was made the default one. */
    private static final String DEFAULT_SET_MESSAGE = "message.structure.defaultstructure";
    private static final String NOT_ALLOWED_MESSAGE = "message.insufficient.permissions.to.save";
    /** The message shown when the user may not make the type the default one. */
    private static final String DEFAULT_NOT_ALLOWED_MESSAGE = "dont-have-permissions-msg";
    private static final String REFERER = "/c/portal/layout?p_l_id=content-types";

    private static final List<ContentType> createdTypes = new ArrayList<>();

    private static PermissionAPI permissionAPI;
    private static User systemUser;
    private static Role ownerRole;
    private static Role siteRole;
    private static Host homeSite;
    private static Host otherSite;
    private static WorkflowScheme customScheme;
    private static WorkflowScheme systemScheme;
    private static User editor;
    private static User writer;
    private static User reader;
    private static User typeEditorWithoutSites;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        systemUser = APILocator.systemUser();
        final RoleAPI roleAPI = APILocator.getRoleAPI();

        final String suffix = String.valueOf(System.nanoTime());
        ownerRole = new RoleDataGen().name("editStructureOwner" + suffix)
                .key("editStructureOwner" + suffix).nextPersisted();
        siteRole = new RoleDataGen().name("editStructureSite" + suffix)
                .key("editStructureSite" + suffix).nextPersisted();

        homeSite = siteWithOwnPermissions("edit-structure-home-" + suffix);
        otherSite = siteWithOwnPermissions("edit-structure-other-" + suffix);

        customScheme = new WorkflowDataGen().name("EditStructureScheme" + suffix).nextPersisted();
        systemScheme = APILocator.getWorkflowAPI().findSystemWorkflowScheme();

        editor = backendUser("edit-structure-editor");
        writer = backendUser("edit-structure-writer");
        reader = backendUser("edit-structure-reader");
        typeEditorWithoutSites = backendUser("edit-structure-no-sites");
        for (final User user : new User[]{editor, writer, reader}) {
            roleAPI.addRoleToUser(siteRole, user);
        }

        assertTrue(permissionAPI.doesUserHavePermissions(otherSite,
                "PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:"
                        + PermissionAPI.PERMISSION_EDIT_PERMISSIONS, editor));
        assertFalse(permissionAPI.doesUserHavePermission(otherSite,
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, typeEditorWithoutSites));
    }

    /** One step of the class cleanup. */
    @FunctionalInterface
    private interface CleanupStep {
        void run() throws Exception;
    }

    /**
     * Removes the content types, including the ones the tests created through the action, the
     * workflow scheme (archived first, as deleting requires), the test users (handing whatever
     * they own to the admin user), the roles and the sites. Every step runs even when an earlier
     * one fails; the first failure is rethrown at the end with the others attached.
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        final List<Exception> failures = new ArrayList<>();
        runCleanupStep(failures, () -> createdTypes.forEach(ContentTypeDataGen::remove));
        for (final Host site : new Host[]{homeSite, otherSite}) {
            if (null != site) {
                runCleanupStep(failures, () -> APILocator.getContentTypeAPI(systemUser)
                        .search("structure.host = '" + site.getIdentifier() + "'")
                        .forEach(ContentTypeDataGen::remove));
            }
        }
        if (null != customScheme) {
            runCleanupStep(failures, () -> {
                customScheme.setArchived(true);
                APILocator.getWorkflowAPI().saveScheme(customScheme, systemUser);
                WorkflowDataGen.remove(customScheme, false);
            });
        }
        for (final User user : new User[]{editor, writer, reader, typeEditorWithoutSites}) {
            if (null != user) {
                runCleanupStep(failures, () -> APILocator.getUserAPI()
                        .delete(user, TestUserUtils.getAdminUser(), systemUser, false));
            }
        }
        for (final Role role : new Role[]{ownerRole, siteRole}) {
            if (null != role) {
                runCleanupStep(failures, () -> RoleDataGen.remove(role));
            }
        }
        for (final Host site : new Host[]{homeSite, otherSite}) {
            if (null != site) {
                runCleanupStep(failures, () -> {
                    APILocator.getHostAPI().archive(site, systemUser, false);
                    APILocator.getHostAPI().delete(site, systemUser, false);
                });
            }
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
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a user who can only read a content type, and who can add content types on both sites,
     * renames it and moves it to the other site
     * Expected: the stored type keeps its name and site, the not-allowed message and no success
     * message
     */
    @Test
    public void save_readOnlyUser_changesNothing() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ);

        final ActionRequestImpl request = saveRequest(reader, type.id());
        final StructureForm form = postedForm(type.id(), type.name() + "Renamed", otherSite);
        new EditStructureAction().processAction(mock(ActionMapping.class), form, null, request,
                mock(ActionResponse.class));

        assertStored(type.id(), type.name(), homeSite);
        assertEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertNotEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a user who can only read a content type posts it unchanged with other workflow
     * schemes
     * Expected: the type keeps its workflow schemes
     */
    @Test
    public void save_readOnlyUser_keepsWorkflowSchemes() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ);

        final ActionRequestImpl request = saveRequest(reader, type.id(), systemScheme.getId());
        new EditStructureAction().processAction(mock(ActionMapping.class),
                postedForm(type.id(), type.name(), homeSite), null, request,
                mock(ActionResponse.class));

        assertEquals(Set.of(customScheme.getId()), schemeIds(type));
        assertNotEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a user with READ, WRITE and PUBLISH on a content type, but not EDIT_PERMISSIONS,
     * renames it on its own site
     * Expected: the stored type keeps its name, the not-allowed message and no success message
     */
    @Test
    public void save_withoutEditPermissions_changesNothing() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);

        final ActionRequestImpl request = saveRequest(writer, type.id());
        new EditStructureAction().processAction(mock(ActionMapping.class),
                postedForm(type.id(), type.name() + "Renamed", homeSite), null, request,
                mock(ActionResponse.class));

        assertStored(type.id(), type.name(), homeSite);
        assertEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertNotEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a user who cannot add children on a site posts a new content type for it
     * Expected: no content type with that name exists, the not-allowed message and no success
     * message
     */
    @Test
    public void create_withoutAddChildrenOnSite_createsNothing() throws Exception {
        final String name = "EditStructureNew" + System.nanoTime();

        final ActionRequestImpl request = saveRequest(typeEditorWithoutSites, null);
        new EditStructureAction().processAction(mock(ActionMapping.class),
                postedForm(null, name, otherSite), null, request, mock(ActionResponse.class));

        assertEquals(0, storedTypesNamed(name).size());
        assertEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertNotEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a user with EDIT_PERMISSIONS on a content type, who cannot add children on the other
     * site, moves the type there
     * Expected: the stored type stays on its site, the not-allowed message and no success message
     */
    @Test
    public void move_withoutAddChildrenOnDestination_changesNothing() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);

        final ActionRequestImpl request = saveRequest(typeEditorWithoutSites, type.id());
        new EditStructureAction().processAction(mock(ActionMapping.class),
                postedForm(type.id(), type.name(), otherSite), null, request,
                mock(ActionResponse.class));

        assertStored(type.id(), type.name(), homeSite);
        assertEquals(NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
        assertNotEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a user with EDIT_PERMISSIONS on a content type, who can add content types on both
     * sites, renames it, moves it to the other site and gives it the system workflow only
     * Expected: the stored type has the new name, the new site and the new workflow scheme, the
     * success message and no error
     */
    @Test
    public void save_withRequiredPermissions_savesChanges() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        final String newName = type.name() + "Renamed";

        final ActionRequestImpl request = saveRequest(editor, type.id(), systemScheme.getId());
        new EditStructureAction().processAction(mock(ActionMapping.class),
                postedForm(type.id(), newName, otherSite), null, request,
                mock(ActionResponse.class));

        assertStored(type.id(), newName, otherSite);
        assertEquals(Set.of(systemScheme.getId()), schemeIds(type));
        assertEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
        assertFalse(SessionMessages.contains(request, ERROR_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=add}
     * Given: a user who can add content types on a site posts a new content type for it with a
     * workflow scheme
     * Expected: one content type with that name exists on the site, with that workflow scheme, the
     * success message and no error
     */
    @Test
    public void create_withRequiredPermissions_createsTypeWithSchemes() throws Exception {
        final String name = "EditStructureNew" + System.nanoTime();

        final ActionRequestImpl request = saveRequest(editor, null, customScheme.getId());
        new EditStructureAction().processAction(mock(ActionMapping.class),
                postedForm(null, name, otherSite), null, request, mock(ActionResponse.class));

        final List<ContentType> created = storedTypesNamed(name);
        createdTypes.addAll(created);
        assertEquals(1, created.size());
        assertEquals(otherSite.getIdentifier(), created.get(0).host());
        assertEquals(Set.of(customScheme.getId()), schemeIds(created.get(0)));
        assertEquals(SAVED_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
        assertFalse(SessionMessages.contains(request, ERROR_KEY));
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=SET_DEFAULT}
     * Given: a user who can only read a content type asks to make it the default one
     * Expected: the default content type does not change, the not-allowed message and no success
     * message
     */
    @Test
    public void setDefault_readOnlyUser_keepsDefault() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ);
        final ContentType originalDefault = APILocator.getContentTypeAPI(systemUser).findDefault();
        try {
            final ActionRequestImpl request = setDefaultRequest(reader, type.id());
            new EditStructureAction().processAction(mock(ActionMapping.class),
                    postedForm(type.id(), type.name(), homeSite), null, request,
                    mock(ActionResponse.class));

            assertEquals(originalDefault.id(),
                    APILocator.getContentTypeAPI(systemUser).findDefault().id());
            assertEquals(DEFAULT_NOT_ALLOWED_MESSAGE, SessionMessages.get(request, ERROR_KEY));
            assertNotEquals(DEFAULT_SET_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
        } finally {
            APILocator.getContentTypeAPI(systemUser).setAsDefault(originalDefault);
        }
    }

    /**
     * Method to test: {@link EditStructureAction#processAction} with {@code cmd=SET_DEFAULT}
     * Given: a user with EDIT_PERMISSIONS on a content type asks to make it the default one
     * Expected: the content type is the default one and the success message is shown; the original
     * default is restored afterwards
     */
    @Test
    public void setDefault_withEditPermissions_changesDefault() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        final ContentType originalDefault = APILocator.getContentTypeAPI(systemUser).findDefault();
        try {
            final ActionRequestImpl request = setDefaultRequest(editor, type.id());
            new EditStructureAction().processAction(mock(ActionMapping.class),
                    postedForm(type.id(), type.name(), homeSite), null, request,
                    mock(ActionResponse.class));

            assertEquals(type.id(), APILocator.getContentTypeAPI(systemUser).findDefault().id());
            assertEquals(DEFAULT_SET_MESSAGE, SessionMessages.get(request, MESSAGE_KEY));
            assertFalse(SessionMessages.contains(request, ERROR_KEY));
        } finally {
            APILocator.getContentTypeAPI(systemUser).setAsDefault(originalDefault);
        }
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    /**
     * A new site with its own permissions: READ and CAN_ADD_CHILDREN, plus EDIT_PERMISSIONS on the
     * content types it holds, for the site role; nothing for anyone else.
     */
    private static Host siteWithOwnPermissions(final String name)
            throws DotDataException, DotSecurityException {
        final Host site = new SiteDataGen().name(name + ".dotcms.com").nextPersisted();
        permissionAPI.permissionIndividually(APILocator.systemHost(), site, systemUser);
        permissionAPI.removePermissions(site);
        savePermission(site, new Permission(site.getPermissionId(), siteRole.getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, true));
        savePermission(site, new Permission(Structure.class.getCanonicalName(),
                site.getPermissionId(), siteRole.getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true));
        return site;
    }

    /**
     * A new content type on the home site, using the custom workflow scheme, with its own
     * permissions: everything for a separate role, and the given permission (which must include
     * READ) for the editor, the writer, the reader and the user without sites alike; each test
     * picks the user whose site permissions it needs.
     */
    private static ContentType newContentType(final int userPermission)
            throws DotDataException, DotSecurityException {
        final String suffix = String.valueOf(System.nanoTime());
        final ContentType type = new ContentTypeDataGen()
                .name("EditStructureType" + suffix)
                .velocityVarName("editStructureType" + suffix)
                .host(homeSite)
                .nextPersisted();
        createdTypes.add(type);
        APILocator.getWorkflowAPI().saveSchemeIdsForContentType(type, Set.of(customScheme.getId()));

        permissionAPI.permissionIndividually(homeSite, type, systemUser);
        permissionAPI.removePermissions(type);
        savePermission(type, new Permission(type.getPermissionId(), ownerRole.getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                        | PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
                true));
        for (final User user : new User[]{editor, writer, reader, typeEditorWithoutSites}) {
            savePermission(type, new Permission(type.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(user).getId(), userPermission, true));
        }

        assertEquals(Set.of(customScheme.getId()), schemeIds(type));
        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, reader));
        assertEquals((userPermission & PermissionAPI.PERMISSION_EDIT_PERMISSIONS) != 0,
                permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, reader));
        return type;
    }

    private static void savePermission(final Permissionable permissionable, final Permission permission)
            throws DotDataException, DotSecurityException {
        permissionAPI.save(permission, permissionable, systemUser, false);
    }

    /**
     * The posted form for an existing content type ({@code inode} set) or a new one ({@code inode}
     * {@code null}), on the given site. The Struts validation of the form is not under test here
     * and always passes.
     */
    private static StructureForm postedForm(final String inode, final String name, final Host site) {
        final StructureForm form = new PostedStructureForm();
        form.setInode(inode);
        form.setName(name);
        form.setHost(site.getIdentifier());
        form.setFolder(Folder.SYSTEM_FOLDER);
        form.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        return form;
    }

    /** A {@link StructureForm} whose Struts validation always passes. */
    public static class PostedStructureForm extends StructureForm {

        private static final long serialVersionUID = 1L;

        @Override
        public ActionErrors validate(final ActionMapping mapping, final HttpServletRequest request) {
            return new ActionErrors();
        }
    }

    /**
     * A {@code cmd=add} request of the given user for the given content type ({@code null} for a
     * new one) with the given workflow schemes and a referer to return to, and a portlet session
     * that keeps the messages the action adds.
     */
    private static ActionRequestImpl saveRequest(final User user, final String inode,
            final String... schemeIds) {
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
        when(request.getParameter("cmd")).thenReturn("add");
        when(request.getParameter("inode")).thenReturn(inode);
        when(request.getParameter("referer")).thenReturn(REFERER);
        when(request.getParameterValues("workflowScheme")).thenReturn(schemeIds);
        final Map<String, Object> requestAttributes = new HashMap<>();
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> requestAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return request;
    }

    /**
     * A {@code cmd=SET_DEFAULT} request of the given user for the given content type, with a
     * referer to return to.
     */
    private static ActionRequestImpl setDefaultRequest(final User user, final String inode) {
        final ActionRequestImpl request = saveRequest(user, inode);
        when(request.getParameter("cmd")).thenReturn(
                com.dotmarketing.util.WebKeys.Structure.SET_DEFAULT);
        return request;
    }

    /** Asserts the stored name and site of the content type, read from the database. */
    private static void assertStored(final String inode, final String name, final Host site)
            throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("SELECT name, host FROM structure WHERE inode = ?");
        dotConnect.addParam(inode);
        final List<Map<String, Object>> rows = dotConnect.loadObjectResults();
        assertEquals(1, rows.size());
        assertEquals(name, rows.get(0).get("name"));
        assertEquals(site.getIdentifier(), rows.get(0).get("host"));
    }

    /** The content types stored with the given name, read without permission checks. */
    private static List<ContentType> storedTypesNamed(final String name)
            throws DotDataException, DotSecurityException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("SELECT inode FROM structure WHERE name = ?");
        dotConnect.addParam(name);
        final List<ContentType> types = new ArrayList<>();
        for (final Map<String, Object> row : dotConnect.loadObjectResults()) {
            types.add(APILocator.getContentTypeAPI(systemUser).find((String) row.get("inode")));
        }
        return types;
    }

    /** The ids of the workflow schemes stored for the content type. */
    private static Set<String> schemeIds(final ContentType type) throws DotDataException {
        return APILocator.getWorkflowAPI().findSchemesForContentType(type).stream()
                .map(WorkflowScheme::getId).collect(Collectors.toSet());
    }
}
