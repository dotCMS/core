package com.dotmarketing.portlets.structure.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.ajax.DwrWebContextRule;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Integration tests for {@link StructureAjax} against stored permission rows: reordering the
 * fields of a content type requires {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS} on it and
 * only accepts fields of that type, and the type reads answer like an empty type for a user who
 * cannot read it.
 */
public class StructureAjaxIntegrationTest {

    private static final String SELECT_OPTIONS = "Red|red\r\nBlue|blue";
    /** The answer reorderfields gives when the fields could not be reordered. */
    private static final String REORDER_FAILED = "Fields-could-not-be-reorderd";

    @ClassRule
    public static final DwrWebContextRule dwrContext = new DwrWebContextRule();

    private static final List<ContentType> createdTypes = new ArrayList<>();

    private static PermissionAPI permissionAPI;
    private static User systemUser;
    private static Role ownerRole;
    private static User editor;
    private static User reader;
    private static User stranger;

    private static ContentType readType;
    private static Field searchableField;
    private static Field selectField;
    private static Container containerUsingReadType;
    private static Category category;
    private static ContentType categoryType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        systemUser = APILocator.systemUser();

        final String suffix = String.valueOf(System.nanoTime());
        ownerRole = new RoleDataGen().name("structureAjaxOwner" + suffix)
                .key("structureAjaxOwner" + suffix).nextPersisted();
        editor = backendUser("structure-editor");
        reader = backendUser("structure-reader");
        stranger = backendUser("structure-stranger");

        readType = newContentType();
        searchableField = new FieldDataGen().type(TextField.class)
                .name("searchable" + suffix).velocityVarName("searchable" + suffix)
                .searchable(true).indexed(true)
                .contentTypeId(readType.id()).nextPersisted();
        selectField = new FieldDataGen().type(SelectField.class)
                .name("color" + suffix).velocityVarName("color" + suffix)
                .values(SELECT_OPTIONS)
                .contentTypeId(readType.id()).nextPersisted();
        containerUsingReadType = new ContainerDataGen()
                .withContentType(readType, "$!{" + searchableField.variable() + "}")
                .nextPersisted();

        // Both the reader and the stranger can read the category, so only READ on the content
        // type decides whether getCategoriesTree returns it.
        category = new CategoryDataGen()
                .setCategoryName("structureAjaxCategory" + suffix)
                .setKey("structureAjaxCategory" + suffix)
                .setCategoryVelocityVarName("structureAjaxCategory" + suffix)
                .nextPersisted();
        for (final User user : new User[]{reader, stranger}) {
            permissionAPI.save(new Permission(category.getPermissionId(),
                            APILocator.getRoleAPI().getUserRole(user).getId(), PermissionAPI.PERMISSION_READ, true),
                    category, systemUser, false);
            assertTrue(permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_READ, user));
        }
        categoryType = newContentType();
        new FieldDataGen().type(CategoryField.class)
                .name("category" + suffix).velocityVarName("category" + suffix)
                .values(category.getInode())
                .contentTypeId(categoryType.id()).nextPersisted();
    }

    /**
     * Removes the container, the content types and the category, then deletes the test users,
     * handing whatever they own to the admin user, and finally the owner role.
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        if (containerUsingReadType != null) {
            ContainerDataGen.remove(containerUsingReadType);
        }
        createdTypes.forEach(ContentTypeDataGen::remove);
        if (category != null) {
            CategoryDataGen.delete(category);
        }
        final User replacement = TestUserUtils.getAdminUser();
        for (final User user : new User[]{editor, reader, stranger}) {
            if (user != null) {
                APILocator.getUserAPI().delete(user, replacement, systemUser, false);
            }
        }
        if (ownerRole != null) {
            RoleDataGen.remove(ownerRole);
        }
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a back-end user who can read a content type but has no EDIT_PERMISSIONS on it
     * Expected: DotSecurityException; the stored sort orders and the type's modification date stay
     * as they were
     */
    @Test
    public void reorderfields_requiresEditPermissionOnType() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 3);
        final Map<String, Integer> before = storedSortOrders(fields);
        final Date modDate = backdateModDate(type);

        dwrContext.loginAs(reader);
        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(type.inode(), orderList(reversed(fields))));

        assertEquals(before, storedSortOrders(fields));
        assertEquals(modDate.getTime(), storedModDate(type).getTime());
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a back-end user with no permission at all on a content type and no portlets
     * Expected: DotSecurityException; the stored sort orders and the type's modification date stay
     * as they were
     */
    @Test
    public void reorderfields_userWithoutPermissions_keepsSortOrder() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 3);
        final Map<String, Integer> before = storedSortOrders(fields);
        final Date modDate = backdateModDate(type);

        dwrContext.loginAs(stranger);
        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(type.inode(), orderList(reversed(fields))));

        assertEquals(before, storedSortOrders(fields));
        assertEquals(modDate.getTime(), storedModDate(type).getTime());
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a user with EDIT_PERMISSIONS on two content types posts the fields of the first type
     * plus one field of the second type, under the first type's inode
     * Expected: DotSecurityException and no field of either type is saved, not even the ones that
     * belong to the first type
     */
    @Test
    public void reorderfields_rejectsFieldOfAnotherType() throws Exception {
        final ContentType typeA = newContentType();
        final ContentType typeB = newContentType();
        final List<Field> fieldsA = textFields(typeA, 2);
        final List<Field> fieldsB = textFields(typeB, 1);
        final List<Field> posted = new ArrayList<>(reversed(fieldsA));
        posted.addAll(fieldsB);
        final Map<String, Integer> before = storedSortOrders(posted);

        dwrContext.loginAs(editor);
        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(typeA.inode(), orderList(posted)));

        assertEquals(before, storedSortOrders(posted));
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a user with EDIT_PERMISSIONS on a content type posts its fields plus an inode that is
     * not a field
     * Expected: DotSecurityException and none of the type's fields is saved
     */
    @Test
    public void reorderfields_rejectsUnknownField() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 2);
        final Map<String, Integer> before = storedSortOrders(fields);
        final String list = orderList(reversed(fields)) + ",00000000-0000-0000-0000-000000000000 @ 99";

        dwrContext.loginAs(editor);
        assertThrows(DotSecurityException.class,
                () -> new StructureAjax().reorderfields(type.inode(), list));

        assertEquals(before, storedSortOrders(fields));
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a user with EDIT_PERMISSIONS on a content type posts a valid order for the first field
     * and a non-numeric order for the second one
     * Expected: the "could not be reordered" answer, not a rejection of the user, and no field is
     * saved, not even the first one
     */
    @Test
    public void reorderfields_nonNumericOrder_savesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 2);
        final Map<String, Integer> before = storedSortOrders(fields);
        final String list = "," + fields.get(0).id() + " @ 50," + fields.get(1).id() + " @ first";

        dwrContext.loginAs(editor);
        final String answer = new StructureAjax().reorderfields(type.inode(), list);

        assertEquals(REORDER_FAILED, answer);
        assertEquals(before, storedSortOrders(fields));
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a back-end user with READ, WRITE and PUBLISH on a content type, but not
     * EDIT_PERMISSIONS
     * Expected: DotSecurityException; the stored sort orders and the type's modification date stay
     * as they were
     */
    @Test
    public void reorderfields_withWriteAndPublishButNotEditPermissions_isDenied() throws Exception {
        final ContentType type = newContentType(PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_WRITE, reader, true));
        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_PUBLISH, reader, true));
        final List<Field> fields = textFields(type, 3);
        final Map<String, Integer> before = storedSortOrders(fields);
        final Date modDate = backdateModDate(type);

        dwrContext.loginAs(reader);
        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(type.inode(), orderList(reversed(fields))));

        assertEquals(before, storedSortOrders(fields));
        assertEquals(modDate.getTime(), storedModDate(type).getTime());
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a user with READ and EDIT_PERMISSIONS on a content type that has a text field and a
     * relationship field to a content type the user cannot read
     * Expected: the success message, both fields stored with their new sort order, and the
     * relationship left as it was
     */
    @Test
    public void reorderfields_typeWithRelationshipField_savesNewOrder() throws Exception {
        final ContentType relatedType = newContentTypeOnlyOwnerCanRead();
        assertFalse(permissionAPI.doesUserHavePermission(relatedType, PermissionAPI.PERMISSION_READ, editor, true));
        final ContentType type = newContentType();
        final List<Field> fields = new ArrayList<>(textFields(type, 1));
        final String variable = "related" + System.nanoTime();
        fields.add(new FieldDataGen().type(RelationshipField.class)
                .name(variable).velocityVarName(variable).sortOrder(2)
                .relationType(relatedType.variable())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                .contentTypeId(type.id()).nextPersisted());
        final Relationship relationship = APILocator.getRelationshipAPI()
                .getRelationshipFromField(fields.get(1), systemUser);
        assertNotNull(relationship);
        final List<Field> newOrder = reversed(fields);

        dwrContext.loginAs(editor);
        final String answer = new StructureAjax().reorderfields(type.inode(), orderList(newOrder));

        assertEquals(successMessage(), answer);
        final Map<String, Integer> expected = new LinkedHashMap<>();
        for (int i = 0; i < newOrder.size(); i++) {
            expected.put(newOrder.get(i).id(), sortOrderAt(i));
        }
        assertEquals(expected, storedSortOrders(newOrder));
        final Relationship after = APILocator.getRelationshipAPI()
                .getRelationshipFromField(fields.get(1), systemUser);
        assertNotNull(after);
        assertEquals(relationship.getInode(), after.getInode());
        assertEquals(relationship.getCardinality(), after.getCardinality());
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a user with EDIT_PERMISSIONS on a content type posts its fields under an inode that is
     * not a content type
     * Expected: DotSecurityException and no field is saved
     */
    @Test
    public void reorderfields_unknownType_savesNothing() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 2);
        final Map<String, Integer> before = storedSortOrders(fields);

        dwrContext.loginAs(editor);
        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields("00000000-0000-0000-0000-000000000000", orderList(reversed(fields))));

        assertEquals(before, storedSortOrders(fields));
    }

    /**
     * Method to test: {@link StructureAjax#reorderfields(String, String)}
     * Given: a user with READ and EDIT_PERMISSIONS on a content type posts a new order for its
     * fields, in the format edit_structure.jsp sends (leading comma)
     * Expected: the success message, every field stored with its new sort order, and the type's
     * modification date moved forward
     */
    @Test
    public void reorderfields_withEditPermissions_savesNewOrder() throws Exception {
        final ContentType type = newContentType();
        final List<Field> fields = textFields(type, 3);
        final Date modDate = backdateModDate(type);
        final List<Field> newOrder = reversed(fields);

        dwrContext.loginAs(editor);
        final String answer = new StructureAjax().reorderfields(type.inode(), orderList(newOrder));

        assertEquals(successMessage(), answer);
        final Map<String, Integer> expected = new LinkedHashMap<>();
        for (int i = 0; i < newOrder.size(); i++) {
            expected.put(newOrder.get(i).id(), sortOrderAt(i));
        }
        assertEquals(expected, storedSortOrders(newOrder));
        assertTrue("the content type's modification date must move forward after a reorder",
                storedModDate(type).getTime() > modDate.getTime());
    }

    /**
     * Method to test: {@link StructureAjax#fetchByIdentity(String)}
     * Given: a back-end user with no permission on the content type
     * Expected: null, the same answer an unknown id gets
     */
    @Test
    public void fetchByIdentity_withoutRead_returnsNull() throws Exception {
        dwrContext.loginAs(stranger);

        assertNull(new StructureAjax().fetchByIdentity(readType.inode()));
    }

    /**
     * Method to test: {@link StructureAjax#fetchByIdentity(String)}
     * Given: a back-end user with READ on the content type
     * Expected: the type's properties
     */
    @Test
    public void fetchByIdentity_withRead_returnsType() throws Exception {
        dwrContext.loginAs(reader);

        final Map<String, Object> type = new StructureAjax().fetchByIdentity(readType.inode());

        assertNotNull(type);
        assertEquals(readType.inode(), type.get("inode"));
    }

    /**
     * Method to test: {@link StructureAjax#fetchByIdentity(String)}
     * Given: a back-end user with READ on other types and an id that is not a content type
     * Expected: null
     */
    @Test
    public void fetchByIdentity_unknownType_returnsNull() throws Exception {
        dwrContext.loginAs(reader);

        assertNull(new StructureAjax().fetchByIdentity("00000000-0000-0000-0000-000000000000"));
    }

    /**
     * Method to test: {@link StructureAjax#getSearchableStructureFields(String)}
     * Given: a content type with a searchable and indexed field, read by a user with no permission
     * on it
     * Expected: an empty list
     */
    @Test
    public void getSearchableStructureFields_withoutRead_returnsEmptyList() throws Exception {
        dwrContext.loginAs(stranger);

        final List<Map> fields = new StructureAjax().getSearchableStructureFields(readType.inode());

        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    /**
     * Method to test: {@link StructureAjax#getSearchableStructureFields(String)}
     * Given: a content type with a searchable and indexed field, read by a user with READ on it
     * Expected: the searchable field is listed
     */
    @Test
    public void getSearchableStructureFields_withRead_listsSearchableField() throws Exception {
        dwrContext.loginAs(reader);

        final List<Map> fields = new StructureAjax().getSearchableStructureFields(readType.inode());

        assertTrue(fields.stream().anyMatch(field -> field.containsValue(searchableField.variable())));
    }

    /**
     * Method to test: {@link StructureAjax#checkDependencies(String)}
     * Given: a content type used by a container, checked by a user with no permission on the type
     * Expected: no containers and size 0
     */
    @Test
    public void checkDependencies_withoutRead_reportsNoContainers() throws Exception {
        dwrContext.loginAs(stranger);

        final Map<Object, Object> dependencies =
                new StructureAjax().checkDependencies(readType.inode());

        assertEquals(0, dependencies.get("size"));
        assertTrue(((List<?>) dependencies.get("containers")).isEmpty());
    }

    /**
     * Method to test: {@link StructureAjax#checkDependencies(String)}
     * Given: a content type used by a container, checked by a user with READ on the type
     * Expected: the container is listed
     */
    @Test
    public void checkDependencies_withRead_listsContainer() throws Exception {
        dwrContext.loginAs(reader);

        final Map<Object, Object> dependencies =
                new StructureAjax().checkDependencies(readType.inode());

        final List<Map<String, String>> containers =
                (List<Map<String, String>>) dependencies.get("containers");
        assertTrue(containers.stream().anyMatch(container ->
                containerUsingReadType.getIdentifier().equals(container.get("identifier"))));
        assertEquals(containers.size(), dependencies.get("size"));
    }

    /**
     * Method to test: {@link StructureAjax#getDropDownList(String)}
     * Given: a select field of a content type, rendered for a user with no permission on the type
     * Expected: an empty string
     */
    @Test
    public void getDropDownList_withoutReadOnOwningType_returnsEmpty() throws Exception {
        dwrContext.loginAs(stranger);

        assertEquals("", new StructureAjax().getDropDownList(selectField.id()));
    }

    /**
     * Method to test: {@link StructureAjax#getDropDownList(String)}
     * Given: a select field of a content type, rendered for a user with READ on the type
     * Expected: the select with the field's options
     */
    @Test
    public void getDropDownList_withReadOnOwningType_rendersOptions() throws Exception {
        dwrContext.loginAs(reader);

        final String html = new StructureAjax().getDropDownList(selectField.id());

        assertTrue(html.contains("value=\"red\""));
        assertTrue(html.contains("value=\"blue\""));
    }

    /**
     * Method to test: {@link StructureAjax#getDropDownList(String)}
     * Given: an id that is not a field
     * Expected: an empty string, the same answer as a field of a type the user cannot read
     */
    @Test
    public void getDropDownList_unknownField_returnsEmpty() throws Exception {
        dwrContext.loginAs(reader);

        assertEquals("", new StructureAjax().getDropDownList("00000000-0000-0000-0000-000000000000"));
    }

    /**
     * Method to test: {@link StructureAjax#getCategoriesTree(String, String)}
     * Given: a content type with a category field, asked for by a back-end user who can read the
     * category but has no permission on the type
     * Expected: an empty list
     */
    @Test
    public void getCategoriesTree_withoutReadOnType_returnsEmptyList() throws Exception {
        dwrContext.loginAs(stranger);

        final List<Map<String, Object>> tree =
                new StructureAjax().getCategoriesTree(categoryType.variable(), ".*");

        assertTrue("no category must be returned, got " + tree, tree.isEmpty());
    }

    /**
     * Method to test: {@link StructureAjax#getCategoriesTree(String, String)}
     * Given: a content type with a category field, asked for by a back-end user with READ on the
     * type and on the category
     * Expected: the field's category is returned
     */
    @Test
    public void getCategoriesTree_withReadOnType_returnsCategory() throws Exception {
        dwrContext.loginAs(reader);

        final List<Map<String, Object>> tree =
                new StructureAjax().getCategoriesTree(categoryType.variable(), ".*");

        assertTrue("the field's category must be returned, got " + tree, tree.stream()
                .anyMatch(entry -> category.getInode().equals(entry.get("inode"))));
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    /**
     * A new content type with its own permissions: everything for a separate role, READ and
     * EDIT_PERMISSIONS for the editor, READ for the reader, nothing for the stranger.
     */
    private static ContentType newContentType() throws DotDataException, DotSecurityException {
        return newContentType(PermissionAPI.PERMISSION_READ);
    }

    /**
     * A new content type with its own permissions: everything for a separate role, READ and
     * EDIT_PERMISSIONS for the editor, the given permission for the reader (which must include
     * READ and not EDIT_PERMISSIONS), nothing for the stranger.
     */
    private static ContentType newContentType(final int readerPermission)
            throws DotDataException, DotSecurityException {
        final ContentType type = newContentTypeOnlyOwnerCanRead();
        savePermission(type, APILocator.getRoleAPI().getUserRole(editor),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        savePermission(type, APILocator.getRoleAPI().getUserRole(reader), readerPermission);

        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, editor, true));
        assertTrue(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, reader, true));
        assertFalse(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, reader, true));
        assertFalse(permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, stranger, true));
        return type;
    }

    /**
     * A new content type whose only permissions are everything for a separate role, so none of
     * the test users can read it.
     */
    private static ContentType newContentTypeOnlyOwnerCanRead() throws DotDataException, DotSecurityException {
        final String suffix = String.valueOf(System.nanoTime());
        final ContentType type = new ContentTypeDataGen()
                .name("StructureAjaxType" + suffix)
                .velocityVarName("structureAjaxType" + suffix)
                .nextPersisted();
        createdTypes.add(type);

        final Host site = APILocator.getHostAPI().find(type.host(), systemUser, false);
        permissionAPI.permissionIndividually(site, type, systemUser);
        permissionAPI.removePermissions(type);
        savePermission(type, ownerRole, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                | PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
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

    /** New sort order for the field at the given position of a posted list. */
    private static int sortOrderAt(final int position) {
        return (position + 1) * 10;
    }

    /** The list in the format edit_structure.jsp posts: {@code ,<inode> @ <order>,...}. */
    private static String orderList(final List<Field> fields) {
        final StringBuilder list = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            list.append(',').append(fields.get(i).id()).append(" @ ").append(sortOrderAt(i));
        }
        return list.toString();
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

    /** Stores a modification date one hour in the past for the type and returns it. */
    private static Date backdateModDate(final ContentType type) throws DotDataException {
        final Date past = new Date(System.currentTimeMillis() - 3_600_000L);
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("UPDATE structure SET mod_date = ? WHERE inode = ?");
        dotConnect.addParam(past);
        dotConnect.addParam(type.inode());
        dotConnect.loadResult();
        return storedModDate(type);
    }

    private static Date storedModDate(final ContentType type) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("SELECT mod_date FROM structure WHERE inode = ?");
        dotConnect.addParam(type.inode());
        return (Date) dotConnect.loadObjectResults().get(0).get("mod_date");
    }

    private static String successMessage() throws Exception {
        final Company company = PublicCompanyFactory.getDefaultCompany();
        return LanguageUtil.get(company.getCompanyId(), company.getLocale(), "message.structure.reorderfield");
    }
}
