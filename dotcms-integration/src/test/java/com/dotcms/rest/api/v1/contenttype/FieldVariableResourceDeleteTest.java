package com.dotcms.rest.api.v1.contenttype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for deleting a field variable as a given user: the content type
 * {@link FieldAPI#delete(FieldVariable, User)} and the two DELETE endpoints of
 * {@link FieldVariableResource}. Deleting needs the same rights on the content type as saving a
 * variable through {@link FieldAPI#save(FieldVariable, User)}. Every test works on its own site,
 * content type, field and users.
 */
public class FieldVariableResourceDeleteTest {

    private static User systemUser;
    private static Host site;
    private static Role ownerRole;
    private static final List<User> usersToClean = new ArrayList<>();
    private static final List<ContentType> typesToClean = new ArrayList<>();

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        ownerRole = new RoleDataGen().nextPersisted();
    }

    /** Deletes the content types, users, role and site the tests created. */
    @AfterAll
    static void cleanUp() throws Exception {
        typesToClean.forEach(type -> ContentTypeDataGen.remove(type, true));
        usersToClean.forEach(user -> UserDataGen.remove(user, true));
        RoleDataGen.remove(ownerRole, true);
        APILocator.getHostAPI().archive(site, systemUser, false);
        APILocator.getHostAPI().delete(site, systemUser, false);
    }

    /**
     * Method to test: {@link FieldAPI#delete(FieldVariable, User)}
     * Given Scenario: A back-end user with no rights on the content type
     * ExpectedResult: The call throws {@link DotSecurityException} and the variable stays
     */
    @Test
    void delete_userWithoutRightsOnType_throwsAndKeepsIt() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        final FieldVariable stored = storeAsSystem(field);

        assertThrows(DotSecurityException.class,
                () -> APILocator.getContentTypeFieldAPI().delete(stored, user));

        assertTrue(isStored(stored));
    }

    /**
     * Method to test: {@link FieldAPI#delete(FieldVariable, User)}
     * Given Scenario: A back-end user with READ, EDIT and EDIT_PERMISSIONS on the content type
     * ExpectedResult: The variable is deleted
     */
    @Test
    void delete_userWithEditRightsOnType_deletesIt() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        grantOnType(field, user, editRights());
        final FieldVariable stored = storeAsSystem(field);

        APILocator.getContentTypeFieldAPI().delete(stored, user);

        assertFalse(isStored(stored));
    }

    /**
     * Method to test: {@link FieldVariableResource#deleteFieldVariableByFieldId}
     * Given Scenario: A back-end user with no rights on the content type calls the endpoint
     * ExpectedResult: The call throws {@link DotSecurityException} and the variable stays
     */
    @Test
    void deleteFieldVariableByFieldId_userWithoutRightsOnType_throwsAndKeepsIt()
            throws Exception {
        final Field field = newField();
        final User user = backendUser();
        final FieldVariable stored = storeAsSystem(field);

        assertThrows(DotSecurityException.class, () -> resource().deleteFieldVariableByFieldId(
                field.contentTypeId(), field.id(), stored.id(), requestAs(user),
                new EmptyHttpResponse()));

        assertTrue(isStored(stored));
    }

    /**
     * Method to test: {@link FieldVariableResource#deleteFieldVariableByFieldVar}
     * Given Scenario: A back-end user with no rights on the content type calls the endpoint
     * ExpectedResult: The call throws {@link DotSecurityException} and the variable stays
     */
    @Test
    void deleteFieldVariableByFieldVar_userWithoutRightsOnType_throwsAndKeepsIt()
            throws Exception {
        final Field field = newField();
        final User user = backendUser();
        final FieldVariable stored = storeAsSystem(field);

        assertThrows(DotSecurityException.class, () -> resource().deleteFieldVariableByFieldVar(
                field.contentTypeId(), field.variable(), stored.id(), requestAs(user),
                new EmptyHttpResponse()));

        assertTrue(isStored(stored));
    }

    /**
     * Method to test: {@link FieldVariableResource#deleteFieldVariableByFieldId}
     * Given Scenario: A back-end user with READ, EDIT and EDIT_PERMISSIONS on the content type
     * calls the endpoint
     * ExpectedResult: The response is 200 and the variable is deleted
     */
    @Test
    void deleteFieldVariableByFieldId_userWithEditRightsOnType_deletesIt() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        grantOnType(field, user, editRights());
        final FieldVariable stored = storeAsSystem(field);

        final Response response = resource().deleteFieldVariableByFieldId(field.contentTypeId(),
                field.id(), stored.id(), requestAs(user), new EmptyHttpResponse());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertFalse(isStored(stored));
    }

    private static FieldVariableResource resource() {
        return new FieldVariableResource(new WebResource(), APILocator.getContentTypeFieldAPI());
    }

    private static HttpServletRequest requestAs(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/contenttype").request())
                .request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    /**
     * A text field on a new content type that lives on the test site. The type gets its own
     * permissions (READ for a role of the test that no test user holds), so what a test user can
     * do on it comes from the test and not from what the type would inherit.
     */
    private static Field newField() throws DotDataException, DotSecurityException {
        final ContentType type = new ContentTypeDataGen().host(site).nextPersisted();
        typesToClean.add(type);
        APILocator.getPermissionAPI().save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                type.getPermissionId(), ownerRole.getId(),
                PermissionAPI.PERMISSION_READ, true), type, systemUser, false);
        return new FieldDataGen().contentTypeId(type.id()).nextPersisted();
    }

    /** A back-end user that holds nothing on the test site or its content types. */
    private static User backendUser() throws DotDataException {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        usersToClean.add(user);
        return user;
    }

    private static int editRights() {
        return PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
    }

    /** Gives the user's own role the permission bits on the content type of the field. */
    private static void grantOnType(final Field field, final User user, final int permission)
            throws DotDataException, DotSecurityException {
        final ContentType type = APILocator.getContentTypeAPI(systemUser).find(field.contentTypeId());
        APILocator.getPermissionAPI().save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                type.getPermissionId(), APILocator.getRoleAPI().getUserRole(user).getId(),
                permission, true), type, systemUser, false);
    }

    /** Stores a variable with a unique key on the field, as the system user. */
    private static FieldVariable storeAsSystem(final Field field)
            throws DotDataException, DotSecurityException {
        final String key = "k" + UUIDGenerator.shorty();
        return APILocator.getContentTypeFieldAPI().save(ImmutableFieldVariable.builder()
                .fieldId(field.id()).name(key).key(key).value("one")
                .userId(systemUser.getUserId()).build(), systemUser);
    }

    /** Whether the variable's row is still in the database. */
    private static boolean isStored(final FieldVariable variable) throws DotDataException {
        return !new DotConnect()
                .setSQL("select id from field_variable where id = ?")
                .addParam(variable.id())
                .loadObjectResults()
                .isEmpty();
    }
}
