package com.dotcms.contenttype.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Unit tests for the rules of {@link FieldReorderHelper}, through both entry points: the user must
 * have EDIT_PERMISSIONS on one existing content type, every field must belong to it, every sort
 * order must be a whole number, and only then are the fields saved, as the user. The content
 * type, field and permission APIs are mocked: type A is editable by the user, type B is not.
 */
class FieldReorderHelperTest {

    private static final String TYPE_A = "type-a-inode";
    private static final String TYPE_B = "type-b-inode";
    private static final String FIELD_A1 = "field-a1-inode";
    private static final String FIELD_A2 = "field-a2-inode";
    private static final String FIELD_B1 = "field-b1-inode";
    private static final String UNKNOWN_FIELD = "unknown-field-inode";
    private static final Map<String, String> SORT_ORDERS = Map.of("field-inode", "1");

    private final User user = mock(User.class);
    private final ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);
    private final FieldAPI fieldAPI = mock(FieldAPI.class);
    private final PermissionAPI permissionAPI = mock(PermissionAPI.class);
    private final ContentType typeA = mock(ContentType.class);
    private final ContentType typeB = mock(ContentType.class);

    /** Names of the {@link SecurityLogger} methods called during a test. */
    private final List<String> securityLoggerCalls = new ArrayList<>();

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<SecurityLogger> securityLogger;

    @BeforeEach
    void setUp() {
        apiLocator = mockStatic(APILocator.class);
        securityLogger = mockStatic(SecurityLogger.class, invocation -> {
            securityLoggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }

    @AfterEach
    void tearDown() {
        securityLogger.close();
        apiLocator.close();
    }

    /**
     * Type A, with fields A1 and A2, on which the user has EDIT_PERMISSIONS, and type B, with field
     * B1, which the user can read but not edit. An unknown field inode is not found.
     */
    private void givenTypes() throws Exception {
        when(user.getUserId()).thenReturn("back-end-user");
        apiLocator.when(() -> APILocator.getContentTypeAPI(any(User.class))).thenReturn(contentTypeAPI);
        apiLocator.when(APILocator::getContentTypeFieldAPI).thenReturn(fieldAPI);
        apiLocator.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);

        final List<Field> fieldsOfA = List.of(field(FIELD_A1, TYPE_A, 1), field(FIELD_A2, TYPE_A, 2));
        final List<Field> fieldsOfB = List.of(field(FIELD_B1, TYPE_B, 1));
        when(contentTypeAPI.find(TYPE_A)).thenReturn(typeA);
        when(typeA.fields()).thenReturn(fieldsOfA);
        when(contentTypeAPI.find(TYPE_B)).thenReturn(typeB);
        when(typeB.fields()).thenReturn(fieldsOfB);
        doThrow(new DotSecurityException("denied")).when(permissionAPI)
                .checkPermission(typeB, PermissionLevel.EDIT_PERMISSIONS, user);
        when(fieldAPI.find(UNKNOWN_FIELD)).thenThrow(new NotFoundInDbException("not found"));
    }

    /** A text field of the given type, also returned by the field API for its inode. */
    private Field field(final String inode, final String typeInode, final int sortOrder) throws Exception {
        final Field field = ImmutableTextField.builder()
                .id(inode)
                .name("Field " + inode)
                .variable("var" + inode.hashCode())
                .contentTypeId(typeInode)
                .dataType(DataTypes.TEXT)
                .sortOrder(sortOrder)
                .build();
        when(fieldAPI.find(inode)).thenReturn(field);
        return field;
    }

    /** Sort orders in posting order, by field inode. */
    private static Map<String, String> posted(final String... inodeThenOrder) {
        final Map<String, String> posted = new LinkedHashMap<>();
        for (int i = 0; i < inodeThenOrder.length; i += 2) {
            posted.put(inodeThenOrder[i], inodeThenOrder[i + 1]);
        }
        return posted;
    }

    private void assertOneSecurityLogEntry() {
        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per rejection, got " + securityLoggerCalls);
    }

    private void assertNothingSaved() throws Exception {
        verify(fieldAPI, never()).saveFields(anyList(), any(User.class));
    }

    /** The sort order of each field passed to the one {@code saveFields} call, saved as the user. */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> savedSortOrders() throws Exception {
        final ArgumentCaptor<List<Field>> saved = ArgumentCaptor.forClass(List.class);
        verify(fieldAPI).saveFields(saved.capture(), eq(user));
        return saved.getValue().stream().collect(Collectors.toMap(Field::id, Field::sortOrder));
    }

    /**
     * Given: no user
     * When: reorder is called for a content type
     * Then: a DotSecurityException is thrown, the rejection is written once to the security log
     * and neither the content type nor its fields are looked up
     */
    @Test
    void reorder_rejectsMissingUser() {
        assertThrows(DotSecurityException.class,
                () -> FieldReorderHelper.reorder(null, "type-inode", SORT_ORDERS));

        assertOneSecurityLogEntry();
        apiLocator.verifyNoInteractions();
    }

    /**
     * Given: no user, as the Struts field action passes when nobody is logged in
     * When: reorderFieldsOfOneType is called
     * Then: a DotSecurityException is thrown, the rejection is written once to the security log
     * and no field is looked up
     */
    @Test
    void reorderFieldsOfOneType_rejectsMissingUser() {
        assertThrows(DotSecurityException.class,
                () -> FieldReorderHelper.reorderFieldsOfOneType(null, SORT_ORDERS));

        assertOneSecurityLogEntry();
        apiLocator.verifyNoInteractions();
    }

    /**
     * Given: the fields of type A, which the user may edit
     * When: reorder is called for type A
     * Then: the fields are saved once, as the user, with the posted sort orders
     */
    @Test
    void reorder_savesSortOrdersAsTheUser() throws Exception {
        givenTypes();

        FieldReorderHelper.reorder(user, TYPE_A, posted(FIELD_A1, "2", FIELD_A2, "1"));

        assertEquals(Map.of(FIELD_A1, 2, FIELD_A2, 1), savedSortOrders());
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: a field of type A and a field of type B
     * When: reorder is called for type A
     * Then: a DotSecurityException is thrown, nothing is saved, not even the field of A, and the
     * rejection is written once to the security log
     */
    @Test
    void reorder_rejectsFieldOfAnotherType() throws Exception {
        givenTypes();

        assertThrows(DotSecurityException.class, () -> FieldReorderHelper.reorder(user, TYPE_A,
                posted(FIELD_A1, "2", FIELD_B1, "1")));

        assertNothingSaved();
        assertOneSecurityLogEntry();
    }

    /**
     * Given: a field of type A and an inode that is no field
     * When: reorder is called for type A
     * Then: a DotSecurityException is thrown, nothing is saved and the rejection is written once
     * to the security log
     */
    @Test
    void reorder_rejectsUnknownField() throws Exception {
        givenTypes();

        assertThrows(DotSecurityException.class, () -> FieldReorderHelper.reorder(user, TYPE_A,
                posted(FIELD_A1, "2", UNKNOWN_FIELD, "1")));

        assertNothingSaved();
        assertOneSecurityLogEntry();
    }

    /**
     * Given: the fields of type A, the second one with a sort order that is not a whole number
     * When: reorder is called for type A
     * Then: a NumberFormatException is thrown, nothing is saved, not even the first field, and
     * nothing is written to the security log
     */
    @Test
    void reorder_nonNumericOrderSavesNothing() throws Exception {
        givenTypes();

        assertThrows(NumberFormatException.class, () -> FieldReorderHelper.reorder(user, TYPE_A,
                posted(FIELD_A1, "2", FIELD_A2, "first")));

        assertNothingSaved();
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: the fields of type A, which the user may edit, with no content type named
     * When: reorderFieldsOfOneType is called
     * Then: the type is taken from the first field and the fields are saved once, as the user,
     * with the posted sort orders
     */
    @Test
    void reorderFieldsOfOneType_savesSortOrdersOfTheFirstFieldsType() throws Exception {
        givenTypes();

        FieldReorderHelper.reorderFieldsOfOneType(user, posted(FIELD_A2, "1", FIELD_A1, "2"));

        assertEquals(Map.of(FIELD_A1, 2, FIELD_A2, 1), savedSortOrders());
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: a field of type A first, then a field of type B
     * When: reorderFieldsOfOneType is called
     * Then: a DotSecurityException is thrown, nothing is saved and the rejection is written once
     * to the security log
     */
    @Test
    void reorderFieldsOfOneType_rejectsFieldOfAnotherType() throws Exception {
        givenTypes();

        assertThrows(DotSecurityException.class, () -> FieldReorderHelper.reorderFieldsOfOneType(user,
                posted(FIELD_A1, "2", FIELD_B1, "1")));

        assertNothingSaved();
        assertOneSecurityLogEntry();
    }

    /**
     * Given: a field of type A first, then an inode that is no field
     * When: reorderFieldsOfOneType is called
     * Then: a DotSecurityException is thrown, nothing is saved and the rejection is written once
     * to the security log
     */
    @Test
    void reorderFieldsOfOneType_rejectsUnknownField() throws Exception {
        givenTypes();

        assertThrows(DotSecurityException.class, () -> FieldReorderHelper.reorderFieldsOfOneType(user,
                posted(FIELD_A1, "2", UNKNOWN_FIELD, "1")));

        assertNothingSaved();
        assertOneSecurityLogEntry();
    }

    /**
     * Given: the fields of type A, the second one with a sort order that is not a whole number
     * When: reorderFieldsOfOneType is called
     * Then: a NumberFormatException is thrown, nothing is saved and nothing is written to the
     * security log
     */
    @Test
    void reorderFieldsOfOneType_nonNumericOrderSavesNothing() throws Exception {
        givenTypes();

        assertThrows(NumberFormatException.class, () -> FieldReorderHelper.reorderFieldsOfOneType(user,
                posted(FIELD_A1, "2", FIELD_A2, "first")));

        assertNothingSaved();
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: first an inode that is no field, then a field of type B, which the user cannot edit
     * When: reorderFieldsOfOneType is called with each of them first
     * Then: both are rejected with the same message, so the answer does not tell whether the
     * first inode is a field
     */
    @Test
    void reorderFieldsOfOneType_unknownFirstFieldAnswersLikeTypeNotEditable() throws Exception {
        givenTypes();

        final DotSecurityException unknownFirst = assertThrows(DotSecurityException.class,
                () -> FieldReorderHelper.reorderFieldsOfOneType(user, posted(UNKNOWN_FIELD, "1")));
        final DotSecurityException notEditable = assertThrows(DotSecurityException.class,
                () -> FieldReorderHelper.reorderFieldsOfOneType(user, posted(FIELD_B1, "1")));

        assertEquals(notEditable.getMessage(), unknownFirst.getMessage());
        assertNothingSaved();
    }
}
