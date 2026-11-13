package com.dotmarketing.portlets.structure.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.FieldVariableTransformer;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the field variable methods of the legacy
 * {@link com.dotmarketing.portlets.structure.business.FieldAPI}: they act as the user they are
 * given. Saving and deleting a variable needs the same rights on the content type as the content
 * type {@link com.dotcms.contenttype.business.FieldAPI}, and reading needs READ on the type.
 * Every test works on its own site, content type, field and users.
 */
public class LegacyFieldAPIFieldVariableTest {

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
     * Method to test: {@link FieldAPI#saveFieldVariable(FieldVariable, User, boolean)}
     * Given Scenario: A back-end user with no rights on the content type
     * ExpectedResult: The call throws {@link DotSecurityException} and no variable is stored
     */
    @Test
    void saveFieldVariable_userWithoutRightsOnType_throwsAndStoresNothing() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        final String key = uniqueKey();

        assertThrows(DotSecurityException.class, () -> legacyFieldAPI()
                .saveFieldVariable(variable(field, key, "one"), user, false));

        assertTrue(storedVariable(field, key).isEmpty());
    }

    /**
     * Method to test: {@link FieldAPI#saveFieldVariable(FieldVariable, User, boolean)}
     * Given Scenario: A back-end user with READ, EDIT and EDIT_PERMISSIONS on the content type
     * ExpectedResult: The variable is stored, with that user as its last modifier
     */
    @Test
    void saveFieldVariable_userWithEditRightsOnType_storesItAsThatUser() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        grantOnType(field, user, editRights());
        final String key = uniqueKey();

        legacyFieldAPI().saveFieldVariable(variable(field, key, "one"), user, false);

        final Map<String, Object> row = storedVariable(field, key).orElseThrow();
        assertEquals("one", row.get("variable_value"));
        assertEquals(user.getUserId(), row.get("user_id"));
    }

    /**
     * Method to test: {@link FieldAPI#deleteFieldVariable(FieldVariable, User, boolean)}
     * Given Scenario: A back-end user with no rights on the content type
     * ExpectedResult: The call throws {@link DotSecurityException} and the variable stays
     */
    @Test
    void deleteFieldVariable_userWithoutRightsOnType_throwsAndKeepsIt() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        final FieldVariable stored = storeAsSystem(field, uniqueKey(), "one");

        assertThrows(DotSecurityException.class,
                () -> legacyFieldAPI().deleteFieldVariable(stored, user, false));

        assertFalse(storedVariable(field, stored.getKey()).isEmpty());
    }

    /**
     * Method to test: {@link FieldAPI#saveFieldVariable(FieldVariable, User, boolean)}
     * Given Scenario: A user with edit rights on one content type saves a variable of that type's
     * field, carrying the id of a variable that belongs to another content type
     * ExpectedResult: The call throws {@link DotSecurityException} and the other type's variable
     * is unchanged
     */
    @Test
    void saveFieldVariable_idOfAnotherTypesVariable_throwsAndKeepsIt() throws Exception {
        final Field ownField = newField();
        final Field otherField = newField();
        final User user = backendUser();
        grantOnType(ownField, user, editRights());
        final FieldVariable otherVariable = storeAsSystem(otherField, uniqueKey(), "one");

        final FieldVariable variable = variable(ownField, otherVariable.getKey(), "two");
        variable.setId(otherVariable.getId());

        assertThrows(DotSecurityException.class,
                () -> legacyFieldAPI().saveFieldVariable(variable, user, false));

        final Map<String, Object> row = storedVariable(otherField, otherVariable.getKey())
                .orElseThrow();
        assertEquals("one", row.get("variable_value"));
    }

    /**
     * Method to test: {@link FieldAPI#deleteFieldVariable(FieldVariable, User, boolean)}
     * Given Scenario: A user with edit rights on one content type deletes by the id of a variable
     * of another content type, passing a field of the type they can edit
     * ExpectedResult: The call throws {@link DotSecurityException} and the variable stays
     */
    @Test
    void deleteFieldVariable_idOfAnotherTypesVariable_throwsAndKeepsIt() throws Exception {
        final Field ownField = newField();
        final Field otherField = newField();
        final User user = backendUser();
        grantOnType(ownField, user, editRights());
        final FieldVariable otherVariable = storeAsSystem(otherField, uniqueKey(), "one");

        final FieldVariable variable = variable(ownField, otherVariable.getKey(), "one");
        variable.setId(otherVariable.getId());

        assertThrows(DotSecurityException.class,
                () -> legacyFieldAPI().deleteFieldVariable(variable, user, false));

        assertFalse(storedVariable(otherField, otherVariable.getKey()).isEmpty());
    }

    /**
     * Method to test: {@link FieldAPI#deleteFieldVariable(FieldVariable, User, boolean)}
     * Given Scenario: A back-end user with READ, EDIT and EDIT_PERMISSIONS on the content type
     * ExpectedResult: The variable is deleted
     */
    @Test
    void deleteFieldVariable_userWithEditRightsOnType_deletesIt() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        grantOnType(field, user, editRights());
        final FieldVariable stored = storeAsSystem(field, uniqueKey(), "one");

        legacyFieldAPI().deleteFieldVariable(stored, user, false);

        assertTrue(storedVariable(field, stored.getKey()).isEmpty());
    }

    /**
     * Method to test: {@link FieldAPI#getFieldVariablesForField(String, User, boolean)}
     * Given Scenario: A back-end user without READ on the content type
     * ExpectedResult: The call throws {@link DotSecurityException}
     */
    @Test
    void getFieldVariablesForField_userWithoutReadOnType_throws() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        storeAsSystem(field, uniqueKey(), "one");

        assertThrows(DotSecurityException.class,
                () -> legacyFieldAPI().getFieldVariablesForField(field.id(), user, false));
    }

    /**
     * Method to test: {@link FieldAPI#findFieldVariable(String, User, boolean)}
     * Given Scenario: A back-end user without READ on the content type
     * ExpectedResult: The call throws {@link DotSecurityException}
     */
    @Test
    void findFieldVariable_userWithoutReadOnType_throws() throws Exception {
        final Field field = newField();
        final User user = backendUser();
        final FieldVariable stored = storeAsSystem(field, uniqueKey(), "one");

        assertThrows(DotSecurityException.class,
                () -> legacyFieldAPI().findFieldVariable(stored.getId(), user, false));
    }

    /**
     * Method to test: {@link FieldAPI#getFieldVariablesForField(String, User, boolean)}
     * Given Scenario: The system user reads the variables of a field
     * ExpectedResult: The stored variable is returned
     */
    @Test
    void getFieldVariablesForField_systemUser_returnsTheVariables() throws Exception {
        final Field field = newField();
        final FieldVariable stored = storeAsSystem(field, uniqueKey(), "one");

        final List<FieldVariable> variables =
                legacyFieldAPI().getFieldVariablesForField(field.id(), systemUser, false);

        assertTrue(variables.stream().anyMatch(variable -> stored.getKey().equals(variable.getKey())
                && "one".equals(variable.getValue())));
    }

    private static FieldAPI legacyFieldAPI() {
        return APILocator.getFieldAPI();
    }

    /**
     * A text field on a new content type that lives on the test site. The type gets its own
     * permissions (READ for a role of the test that no test user holds), so what a test user can do on it
     * comes from the test and not from what the type would inherit.
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

    private static String uniqueKey() {
        return "k" + UUIDGenerator.shorty();
    }

    private static FieldVariable variable(final Field field, final String key, final String value) {
        final FieldVariable variable = new FieldVariable();
        variable.setFieldId(field.id());
        variable.setName(key);
        variable.setKey(key);
        variable.setValue(value);
        variable.setLastModifierId(systemUser.getUserId());
        variable.setLastModDate(new Date());
        return variable;
    }

    /** Stores a variable through the content type field API, as the system user. */
    private static FieldVariable storeAsSystem(final Field field, final String key, final String value)
            throws DotDataException, DotSecurityException {
        final com.dotcms.contenttype.model.field.FieldVariable saved = APILocator
                .getContentTypeFieldAPI().save(ImmutableFieldVariable.builder().fieldId(field.id())
                        .name(key).key(key).value(value).userId(systemUser.getUserId()).build(),
                        systemUser);
        return new FieldVariableTransformer(saved).oldField();
    }

    /** The stored row of the field's variable with that key, read from the database. */
    private static Optional<Map<String, Object>> storedVariable(final Field field,
            final String key) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select variable_value, user_id from field_variable"
                        + " where field_id = ? and variable_key = ?")
                .addParam(field.id())
                .addParam(key)
                .loadObjectResults();
        return rows.stream().findFirst();
    }
}
