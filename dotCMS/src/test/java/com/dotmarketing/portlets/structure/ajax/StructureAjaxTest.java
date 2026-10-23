package com.dotmarketing.portlets.structure.ajax;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_EDIT_PERMISSIONS;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedStatic;
import org.mockito.invocation.Invocation;

/**
 * Unit tests for the access rules of {@link StructureAjax}: reordering fields requires
 * EDIT_PERMISSIONS on the content type, only accepts that type's fields and saves them as the
 * calling user; the type reads return data only to users who can read the type. The logged-in user
 * comes from a mocked DWR request and every collaborator is mocked.
 */
class StructureAjaxTest {

    private static final String TYPE_A = "type-a-inode";
    private static final String TYPE_B = "type-b-inode";
    private static final String UNKNOWN_TYPE = "unknown-type-inode";
    private static final String FIELD_A1 = "field-a1-inode";
    private static final String FIELD_A2 = "field-a2-inode";
    private static final String FIELD_B1 = "field-b1-inode";
    private static final String SELECT_FIELD = "select-field-inode";
    private static final String RELATIONSHIP_FIELD = "relationship-field-inode";
    private static final String UNKNOWN_FIELD = "unknown-field-inode";
    /** The text getStructureFields returns for a type without fields. */
    private static final String NO_FIELDS = "[  ]";
    /** The answer reorderfields gives when the fields could not be reordered. */
    private static final String REORDER_FAILED = "Fields-could-not-be-reorderd";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final UserWebAPI userWebAPI = mock(UserWebAPI.class);
    private final PermissionAPI permissionAPI = mock(PermissionAPI.class);
    private final ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);
    private final FieldAPI fieldAPI = mock(FieldAPI.class);
    private final ContainerAPI containerAPI = mock(ContainerAPI.class);
    private final ContentTypeCache contentTypeCache = mock(ContentTypeCache.class);
    private final CategoryAPI categoryAPI = mock(CategoryAPI.class);
    private final User user = mock(User.class);
    private final User systemUser = mock(User.class);
    private final Structure typeA = mock(Structure.class);
    private final ContentType contentTypeA = mock(ContentType.class);

    /** Names of the {@link SecurityLogger} methods called during a test. */
    private final List<String> securityLoggerCalls = new ArrayList<>();

    private MockedStatic<WebContextFactory> webContextFactory;
    private MockedStatic<WebAPILocator> webAPILocator;
    private MockedStatic<PortalUtil> portalUtil;
    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<CacheLocator> cacheLocator;
    private MockedStatic<StructureFactory> structureFactory;
    private MockedStatic<FieldFactory> fieldFactory;
    private MockedStatic<FieldsCache> fieldsCache;
    private MockedStatic<PublicCompanyFactory> publicCompanyFactory;
    private MockedStatic<LanguageUtil> languageUtil;
    private MockedStatic<Logger> logger;
    private MockedStatic<SecurityLogger> securityLogger;

    @BeforeEach
    void setUp() throws Exception {
        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        webContextFactory = mockStatic(WebContextFactory.class);
        webContextFactory.when(WebContextFactory::get).thenReturn(webContext);

        when(user.getUserId()).thenReturn("back-end-user");
        when(user.isBackendUser()).thenReturn(true);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(true);
        webAPILocator = mockStatic(WebAPILocator.class);
        webAPILocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);
        portalUtil = mockStatic(PortalUtil.class);
        portalUtil.when(() -> PortalUtil.getUser(request)).thenReturn(user);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);
        apiLocator.when(() -> APILocator.getContentTypeAPI(any(User.class))).thenReturn(contentTypeAPI);
        apiLocator.when(() -> APILocator.getContentTypeAPI(any(User.class), anyBoolean())).thenReturn(contentTypeAPI);
        apiLocator.when(APILocator::getContentTypeFieldAPI).thenReturn(fieldAPI);
        apiLocator.when(APILocator::getContainerAPI).thenReturn(containerAPI);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getCategoryAPI).thenReturn(categoryAPI);

        cacheLocator = mockStatic(CacheLocator.class);
        cacheLocator.when(CacheLocator::getContentTypeCache).thenReturn(contentTypeCache);
        structureFactory = mockStatic(StructureFactory.class);
        fieldFactory = mockStatic(FieldFactory.class);
        fieldsCache = mockStatic(FieldsCache.class);
        publicCompanyFactory = mockStatic(PublicCompanyFactory.class);
        publicCompanyFactory.when(PublicCompanyFactory::getDefaultCompany).thenReturn(mock(Company.class));
        languageUtil = mockStatic(LanguageUtil.class);
        logger = mockStatic(Logger.class);
        securityLogger = mockStatic(SecurityLogger.class, invocation -> {
            securityLoggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });

        givenTypeA();
        givenUnknownType();
        givenSelectField();
        givenUnknownField();
    }

    @AfterEach
    void tearDown() {
        securityLogger.close();
        logger.close();
        languageUtil.close();
        publicCompanyFactory.close();
        fieldsCache.close();
        fieldFactory.close();
        structureFactory.close();
        cacheLocator.close();
        apiLocator.close();
        portalUtil.close();
        webAPILocator.close();
        webContextFactory.close();
    }

    /** Content type A, with two fields, reachable through the legacy and the current lookups. */
    private void givenTypeA() throws Exception {
        when(typeA.getInode()).thenReturn(TYPE_A);
        when(typeA.getPermissionId()).thenReturn(TYPE_A);
        when(typeA.getVelocityVarName()).thenReturn("typeA");
        when(typeA.getName()).thenReturn("Type A");
        final Map<String, Object> typeMap = new HashMap<>();
        typeMap.put("inode", TYPE_A);
        typeMap.put("name", "Type A");
        when(typeA.getMap()).thenReturn(typeMap);
        when(typeA.getFields()).thenReturn(List.of(
                legacyField(FIELD_A1, TYPE_A, 1), legacyField(FIELD_A2, TYPE_A, 2)));
        structureFactory.when(() -> StructureFactory.getStructureByInode(TYPE_A)).thenReturn(typeA);
        when(contentTypeCache.getStructureByInode(TYPE_A)).thenReturn(typeA);

        when(contentTypeA.id()).thenReturn(TYPE_A);
        when(contentTypeA.inode()).thenReturn(TYPE_A);
        when(contentTypeA.getPermissionId()).thenReturn(TYPE_A);
        when(contentTypeA.variable()).thenReturn("typeA");
        when(contentTypeAPI.find(TYPE_A)).thenReturn(contentTypeA);

        final List<com.dotcms.contenttype.model.field.Field> fieldsOfTypeA = List.of(
                givenField(FIELD_A1, TYPE_A, 1), givenField(FIELD_A2, TYPE_A, 2));
        when(contentTypeA.fields()).thenReturn(fieldsOfTypeA);
        when(fieldAPI.byContentTypeId(TYPE_A)).thenReturn(fieldsOfTypeA);
        givenField(FIELD_B1, TYPE_B, 1);

        final Container container = mock(Container.class);
        when(container.getIdentifier()).thenReturn("container-id");
        when(container.getInode()).thenReturn("container-inode");
        when(container.getTitle()).thenReturn("Container using type A");
        when(containerAPI.findContainersForStructure(TYPE_A, true)).thenReturn(List.of(container));
        when(containerAPI.findContainersForStructure(TYPE_A)).thenReturn(List.of(container));
    }

    /** An inode that is no content type: the lookups answer as they do for a missing type. */
    private void givenUnknownType() throws Exception {
        structureFactory.when(() -> StructureFactory.getStructureByInode(UNKNOWN_TYPE))
                .thenReturn(new Structure());
        when(contentTypeCache.getStructureByInode(UNKNOWN_TYPE)).thenReturn(null);
        when(contentTypeAPI.find(UNKNOWN_TYPE)).thenThrow(new NotFoundInDbException("not found"));
    }

    /** A select field of type A whose options are "Red|red" and "Blue|blue". */
    private void givenSelectField() throws Exception {
        final Field select = legacyField(SELECT_FIELD, TYPE_A, 3);
        select.setFieldType(Field.FieldType.SELECT.toString());
        select.setValues("Red|red\nBlue|blue");
        fieldFactory.when(() -> FieldFactory.getFieldByInode(SELECT_FIELD)).thenReturn(select);
        when(fieldAPI.find(SELECT_FIELD)).thenReturn(ImmutableTextField.builder()
                .id(SELECT_FIELD)
                .name("Colour")
                .variable("colour")
                .contentTypeId(TYPE_A)
                .dataType(DataTypes.TEXT)
                .build());
    }

    /** A field inode that does not exist: the lookups answer as they do for a missing field. */
    private void givenUnknownField() throws Exception {
        fieldFactory.when(() -> FieldFactory.getFieldByInode(UNKNOWN_FIELD)).thenReturn(new Field());
        when(fieldAPI.find(UNKNOWN_FIELD)).thenThrow(new NotFoundInDbException("not found"));
    }

    private Field legacyField(final String inode, final String typeInode, final int sortOrder) {
        final Field field = new Field();
        field.setInode(inode);
        field.setStructureInode(typeInode);
        field.setFieldName("Field " + inode);
        field.setVelocityVarName("var" + sortOrder);
        field.setFieldContentlet("text" + sortOrder);
        field.setFieldType(Field.FieldType.TEXT.toString());
        field.setSortOrder(sortOrder);
        field.setSearchable(true);
        field.setIndexed(true);
        return field;
    }

    /**
     * A field of the given type, as both the legacy and the current field lookups return it.
     *
     * @return the field as the current field API returns it
     */
    private com.dotcms.contenttype.model.field.Field givenField(final String inode,
            final String typeInode, final int sortOrder) throws Exception {
        fieldFactory.when(() -> FieldFactory.getFieldByInode(inode))
                .thenReturn(legacyField(inode, typeInode, sortOrder));
        final com.dotcms.contenttype.model.field.Field field = ImmutableTextField.builder()
                .id(inode)
                .name("Field " + inode)
                .variable("var" + sortOrder)
                .contentTypeId(typeInode)
                .dataType(DataTypes.TEXT)
                .sortOrder(sortOrder)
                .build();
        when(fieldAPI.find(inode)).thenReturn(field);
        return field;
    }

    /** A relationship field to type B, added to the fields of type A. */
    private void givenRelationshipFieldOnTypeA() {
        final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(contentTypeA.fields());
        fields.add(ImmutableRelationshipField.builder()
                .id(RELATIONSHIP_FIELD)
                .name("Related")
                .variable("related")
                .contentTypeId(TYPE_A)
                .relationType("typeB")
                .values("0")
                .sortOrder(3)
                .build());
        when(contentTypeA.fields()).thenReturn(fields);
    }

    /** A category field on type A, pointing to the category {@code category-inode}. */
    private void givenCategoryFieldOnTypeA() {
        final Field categoryField = legacyField("category-field-inode", TYPE_A, 4);
        categoryField.setFieldType(Field.FieldType.CATEGORY.toString());
        categoryField.setValues("category-inode");
        when(typeA.getFields()).thenReturn(List.of(categoryField));
        when(contentTypeCache.getStructureByVelocityVarName("typeA")).thenReturn(typeA);
    }

    private void assertOneSecurityLogEntry() {
        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per denial, got " + securityLoggerCalls);
    }

    /** The caller holds, or not, the given permission on type A, whichever API checks it. */
    private void givenPermissionOnTypeA(final int permission, final boolean granted) throws Exception {
        when(permissionAPI.doesUserHavePermission(any(Permissionable.class), eq(permission), eq(user)))
                .thenReturn(granted);
        when(permissionAPI.doesUserHavePermission(any(Permissionable.class), eq(permission), eq(user), anyBoolean()))
                .thenReturn(granted);
        when(permissionAPI.doesUserHavePermission(any(ContentType.class), eq(permission), eq(user)))
                .thenReturn(granted);
        when(permissionAPI.doesUserHavePermission(any(ContentType.class), eq(permission), eq(user), anyBoolean()))
                .thenReturn(granted);
        if (!granted) {
            final PermissionLevel level = PermissionLevel.getPermissionLevel(permission);
            doThrow(new DotSecurityException("denied")).when(permissionAPI)
                    .checkPermission(any(Permissionable.class), eq(level), eq(user));
            if (permission == PERMISSION_READ) {
                when(contentTypeAPI.find(TYPE_A)).thenThrow(new DotSecurityException("denied"));
            }
        }
    }

    /** Calls on the field API that save fields, whichever save method was used. */
    private List<Invocation> fieldSaves() {
        return mockingDetails(fieldAPI).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().startsWith("save"))
                .collect(Collectors.toList());
    }

    /** The sort order saved for each field, from the field API save calls. */
    private Map<String, Integer> savedSortOrders() {
        final Map<String, Integer> sortOrders = new HashMap<>();
        for (final Invocation save : fieldSaves()) {
            final Object fields = save.getArgument(0);
            final List<?> saved = fields instanceof List ? (List<?>) fields : List.of(fields);
            for (final Object field : saved) {
                final com.dotcms.contenttype.model.field.Field current =
                        (com.dotcms.contenttype.model.field.Field) field;
                sortOrders.put(current.id(), current.sortOrder());
            }
        }
        return sortOrders;
    }

    /** The field with the given id as it was passed to the field API to be saved. */
    private com.dotcms.contenttype.model.field.Field savedField(final String fieldId) {
        for (final Invocation save : fieldSaves()) {
            final Object fields = save.getArgument(0);
            final List<?> saved = fields instanceof List ? (List<?>) fields : List.of(fields);
            for (final Object field : saved) {
                final com.dotcms.contenttype.model.field.Field current =
                        (com.dotcms.contenttype.model.field.Field) field;
                if (fieldId.equals(current.id())) {
                    return current;
                }
            }
        }
        throw new AssertionError("the field " + fieldId + " was not saved, got " + fieldSaves());
    }

    private void assertNothingSaved() {
        assertTrue(fieldSaves().isEmpty(), "no field must be saved, got " + fieldSaves());
        fieldFactory.verify(() -> FieldFactory.saveField(any(Field.class)), never());
        structureFactory.verify(() -> StructureFactory.saveStructure(any(Structure.class)), never());
    }

    /**
     * Given: a user without EDIT_PERMISSIONS on the content type
     * When: reorderfields is called with fields of that type
     * Then: a DotSecurityException is thrown, no field or type is saved and the denial is
     * recorded once in the security log
     */
    @Test
    void reorderfields_requiresEditPermissionOnType() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, false);

        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(TYPE_A, FIELD_A1 + " @ 2," + FIELD_A2 + " @ 1"));
        assertNothingSaved();
        assertOneSecurityLogEntry();
    }

    /**
     * Given: a user without EDIT_PERMISSIONS on the content type and a list whose second sort
     * order is not a whole number
     * When: reorderfields is called
     * Then: the request is denied like any other request from that user (DotSecurityException,
     * one security log entry), whatever the sort orders hold, and nothing is saved
     */
    @Test
    void reorderfields_nonNumericOrderFromUserWithoutEditPermissionIsDenied() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, false);

        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(TYPE_A, FIELD_A1 + " @ 2," + FIELD_A2 + " @ second"));
        assertNothingSaved();
        assertOneSecurityLogEntry();
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on the content type, and a content type lookup that
     * fails with a data error (not a missing type)
     * When: reorderfields is called
     * Then: the data error is not reported as a denial: no exception reaches the caller, the
     * answer is the "could not be reordered" text, nothing goes to the security log and nothing
     * is saved
     */
    @Test
    void reorderfields_typeLookupDataErrorIsNotReportedAsDenial() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);
        when(contentTypeAPI.find(TYPE_A)).thenThrow(new DotDataException("content type could not be loaded"));

        final String answer = assertDoesNotThrow(() -> new StructureAjax()
                        .reorderfields(TYPE_A, FIELD_A1 + " @ 2," + FIELD_A2 + " @ 1"),
                "a data error while loading the content type must not reach the caller as an exception");

        assertEquals(REORDER_FAILED, answer);
        assertTrue(securityLoggerCalls.isEmpty(),
                "a data error must not write to the security log, got " + securityLoggerCalls);
        assertNothingSaved();
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on type A and a list with one field of A and one field of type B
     * When: reorderfields is called for type A
     * Then: a DotSecurityException is thrown and no field is saved, including the field of A
     */
    @Test
    void reorderfields_rejectsFieldOfAnotherType() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(TYPE_A, FIELD_A1 + " @ 2," + FIELD_B1 + " @ 9"));
        assertNothingSaved();
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on type A and fields of A
     * When: reorderfields is called
     * Then: each field is saved with the sort order from the list and the calling user
     */
    @Test
    void reorderfields_savesOrderAsCallingUser() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        new StructureAjax().reorderfields(TYPE_A, FIELD_A1 + " @ 2," + FIELD_A2 + " @ 1");

        assertFalse(fieldSaves().isEmpty(), "the fields must be saved through the field API");
        for (final Invocation save : fieldSaves()) {
            assertEquals(user, save.getArgument(1), "fields must be saved as the calling user");
        }
        assertEquals(Map.of(FIELD_A1, 2, FIELD_A2, 1), savedSortOrders());
        fieldFactory.verify(() -> FieldFactory.saveField(any(Field.class)), never());
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on type A, which has a relationship field next to its
     * text fields
     * When: reorderfields is called with a text field and the relationship field
     * Then: both are saved with their new sort order, and the relationship field is saved so that
     * its relationship is neither created nor updated, as the content type field layout does when
     * it moves fields
     */
    @Test
    void reorderfields_savesRelationshipFieldWithoutRecreatingRelationship() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);
        givenRelationshipFieldOnTypeA();

        new StructureAjax().reorderfields(TYPE_A, FIELD_A1 + " @ 2," + RELATIONSHIP_FIELD + " @ 1");

        assertEquals(Map.of(FIELD_A1, 2, RELATIONSHIP_FIELD, 1), savedSortOrders());
        final com.dotcms.contenttype.model.field.Field saved = savedField(RELATIONSHIP_FIELD);
        assertTrue(saved instanceof RelationshipField, "got " + saved);
        assertTrue(((RelationshipField) saved).skipRelationshipCreation(),
                "a reorder must not create or update the relationship of the field");
    }

    /**
     * Given: a content type inode that resolves to no type
     * When: reorderfields is called
     * Then: a DotSecurityException is thrown and nothing is saved
     */
    @Test
    void reorderfields_rejectsUnknownType() {
        assertThrows(DotSecurityException.class, () -> new StructureAjax()
                .reorderfields(UNKNOWN_TYPE, FIELD_A1 + " @ 2"));
        assertNothingSaved();
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on the content type and an empty or missing list
     * When: reorderfields is called
     * Then: the request is not denied (no exception, no security log entry) and nothing is saved
     */
    @ParameterizedTest(name = "list: [{0}]")
    @NullAndEmptySource
    void reorderfields_emptyListSavesNothing(final String inodeList) throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        assertDoesNotThrow(() -> new StructureAjax().reorderfields(TYPE_A, inodeList));

        assertNothingSaved();
        assertTrue(securityLoggerCalls.isEmpty(),
                "an empty list is not a denial, got " + securityLoggerCalls);
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on the content type and a list where one entry has no
     * {@code " @ "} separator
     * When: reorderfields is called
     * Then: that entry is skipped and the other entries are saved with their sort order
     */
    @Test
    void reorderfields_skipsEntryWithoutSeparator() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        new StructureAjax().reorderfields(TYPE_A, FIELD_A1 + "@2," + FIELD_A2 + " @ 1");

        assertEquals(Map.of(FIELD_A2, 1), savedSortOrders());
        assertTrue(securityLoggerCalls.isEmpty(),
                "a skipped entry is not a denial, got " + securityLoggerCalls);
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on the content type and a list where one entry has the
     * separator but no sort order ({@code "<inode> @ "})
     * When: reorderfields is called
     * Then: that entry is skipped, no sort order is saved for its field, and the other entries are
     * saved
     */
    @Test
    void reorderfields_skipsEntryWithBlankSortOrder() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        new StructureAjax().reorderfields(TYPE_A, FIELD_A1 + " @ ," + FIELD_A2 + " @ 3");

        assertEquals(Map.of(FIELD_A2, 3), savedSortOrders());
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on the content type and a negative sort order
     * When: reorderfields is called
     * Then: the negative sort order is saved as sent
     */
    @Test
    void reorderfields_savesNegativeSortOrderAsSent() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        new StructureAjax().reorderfields(TYPE_A, FIELD_A1 + " @ -3," + FIELD_A2 + " @ 1");

        assertEquals(Map.of(FIELD_A1, -3, FIELD_A2, 1), savedSortOrders());
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on the content type and a sort order too large for an
     * int
     * When: reorderfields is called
     * Then: the "could not be reordered" text is returned, nothing is saved, not even the valid
     * entry, and nothing is written to the security log
     */
    @Test
    void reorderfields_sortOrderOutOfIntRangeSavesNothing() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        final String answer = new StructureAjax()
                .reorderfields(TYPE_A, FIELD_A1 + " @ 2," + FIELD_A2 + " @ 99999999999");

        assertEquals(REORDER_FAILED, answer);
        assertNothingSaved();
        assertTrue(securityLoggerCalls.isEmpty(),
                "a sort order out of range is not a denial, got " + securityLoggerCalls);
    }

    /**
     * Given: a user with EDIT_PERMISSIONS on type A, and no content type inode (the content type
     * API answers {@code null} for it)
     * When: reorderfields is called with fields of type A
     * Then: a DotSecurityException is thrown, nothing is saved and the denial is recorded once in
     * the security log
     */
    @Test
    void reorderfields_rejectsMissingTypeInode() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenPermissionOnTypeA(PERMISSION_EDIT_PERMISSIONS, true);

        assertThrows(DotSecurityException.class,
                () -> new StructureAjax().reorderfields(null, FIELD_A1 + " @ 2"));
        assertNothingSaved();
        assertOneSecurityLogEntry();
    }

    /**
     * Given: an inode that resolves to no content type
     * When: checkDependencies is called
     * Then: size is 0, containers is empty, the containers are not looked up and nothing is
     * written to the security log
     */
    @Test
    void checkDependencies_unknownTypeListsNoContainers() throws Exception {
        final Map<Object, Object> result = new StructureAjax().checkDependencies(UNKNOWN_TYPE);

        assertEquals(0, result.get("size"));
        assertEquals(List.of(), result.get("containers"));
        verify(containerAPI, never()).findContainersForStructure(anyString(), anyBoolean());
        verify(containerAPI, never()).findContainersForStructure(anyString());
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: an inode that resolves to no content type
     * When: getStructureFields is called
     * Then: the text of a type without fields, {@code "[  ]"}, is returned
     */
    @Test
    void getStructureFields_unknownTypeReturnsNoFields() {
        assertEquals(NO_FIELDS, new StructureAjax().getStructureFields(UNKNOWN_TYPE));
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: an inode that resolves to no content type
     * When: getSearchableStructureFields is called
     * Then: an empty list is returned
     */
    @Test
    void getSearchableStructureFields_unknownTypeReturnsEmptyList() {
        final List<Map> fields = new StructureAjax().getSearchableStructureFields(UNKNOWN_TYPE);

        assertNotNull(fields);
        assertTrue(fields.isEmpty(), "no field must be returned, got " + fields);
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: a variable name that is no content type
     * When: getCategoriesTree is called
     * Then: an empty list is returned and no category is looked up
     */
    @Test
    void getCategoriesTree_unknownVariableNameReturnsEmptyList() throws Exception {
        final List<Map<String, Object>> tree = new StructureAjax().getCategoriesTree("unknownType", ".*");

        assertTrue(tree.isEmpty(), "no category must be returned, got " + tree);
        verify(categoryAPI, never()).find(anyString(), any(User.class), anyBoolean());
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: a user without READ on the content type
     * When: fetchByIdentity is called
     * Then: null is returned
     */
    @Test
    void fetchByIdentity_requiresReadOnType() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, false);

        assertNull(new StructureAjax().fetchByIdentity(TYPE_A));
    }

    /**
     * Given: a user without READ on the content type
     * When: checkDependencies is called
     * Then: size is 0, containers is empty and the containers are not looked up
     */
    @Test
    void checkDependencies_requiresReadOnType() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, false);

        final Map<Object, Object> result = new StructureAjax().checkDependencies(TYPE_A);

        assertEquals(0, result.get("size"));
        assertEquals(List.of(), result.get("containers"));
        verify(containerAPI, never()).findContainersForStructure(anyString(), anyBoolean());
        verify(containerAPI, never()).findContainersForStructure(anyString());
    }

    /**
     * Given: a user with READ on the content type, and a content type lookup that fails with a
     * data error (not a missing type)
     * When: checkDependencies is called
     * Then: the data error reaches the caller instead of a size of zero, which the delete dialog
     * would read as "no container uses this type", with a fixed message instead of the database
     * text; the containers are not looked up and nothing is written to the security log
     */
    @Test
    void checkDependencies_typeLookupDataErrorIsReportedToTheCaller() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        final String databaseText = "org.postgresql.util.PSQLException: ERROR: relation \"structure\" does not exist";
        when(contentTypeAPI.find(TYPE_A)).thenThrow(new DotDataException(databaseText));

        final DotDataException thrown = assertThrows(DotDataException.class,
                () -> new StructureAjax().checkDependencies(TYPE_A));
        assertFalse(thrown.getMessage().contains(databaseText),
                "the database text must not reach the caller, got: " + thrown.getMessage());

        verify(containerAPI, never()).findContainersForStructure(anyString(), anyBoolean());
        verify(containerAPI, never()).findContainersForStructure(anyString());
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: nobody is logged in
     * When: fetchByIdentity, checkDependencies, getStructureFields and getSearchableStructureFields
     * are called for a content type
     * Then: each answers like a type the caller cannot read, the content type is not loaded and
     * nothing is written to the security log
     */
    @Test
    void typeReads_withoutLoggedInUserReturnNothing() throws Exception {
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);
        final StructureAjax structureAjax = new StructureAjax();

        assertNull(structureAjax.fetchByIdentity(TYPE_A));
        assertEquals(0, structureAjax.checkDependencies(TYPE_A).get("size"));
        assertEquals(NO_FIELDS, structureAjax.getStructureFields(TYPE_A));
        final List<Map> searchable = structureAjax.getSearchableStructureFields(TYPE_A);
        assertNotNull(searchable);
        assertTrue(searchable.isEmpty(), "no field must be returned, got " + searchable);

        verify(contentTypeAPI, never()).find(anyString());
        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: a logged-in user and no content type inode
     * When: fetchByIdentity, checkDependencies, getStructureFields and getSearchableStructureFields
     * are called
     * Then: each answers like an unknown type and the content type is not loaded
     */
    @ParameterizedTest(name = "inode: [{0}]")
    @NullAndEmptySource
    void typeReads_blankInodeReturnNothing(final String inode) throws Exception {
        final StructureAjax structureAjax = new StructureAjax();

        assertNull(structureAjax.fetchByIdentity(inode));
        assertEquals(0, structureAjax.checkDependencies(inode).get("size"));
        assertEquals(NO_FIELDS, structureAjax.getStructureFields(inode));
        final List<Map> searchable = structureAjax.getSearchableStructureFields(inode);
        assertNotNull(searchable);
        assertTrue(searchable.isEmpty(), "no field must be returned, got " + searchable);

        verify(contentTypeAPI, never()).find(any());
    }

    /**
     * Given: a logged-in user and a content type lookup that fails with a data error
     * When: fetchByIdentity, getStructureFields and getSearchableStructureFields are called
     * Then: each answers like a type with nothing to show, and the failure is not written to the
     * security log as a denial
     */
    @Test
    void typeReads_typeLookupDataErrorReturnNothingWithoutDenial() throws Exception {
        when(contentTypeAPI.find(TYPE_A)).thenThrow(new DotDataException("content type could not be loaded"));
        final StructureAjax structureAjax = new StructureAjax();

        assertNull(structureAjax.fetchByIdentity(TYPE_A));
        assertEquals(NO_FIELDS, structureAjax.getStructureFields(TYPE_A));
        final List<Map> searchable = structureAjax.getSearchableStructureFields(TYPE_A);
        assertNotNull(searchable);
        assertTrue(searchable.isEmpty(), "no field must be returned, got " + searchable);

        assertTrue(securityLoggerCalls.isEmpty(), "got " + securityLoggerCalls);
    }

    /**
     * Given: a user without READ on the content type
     * When: getStructureFields is called
     * Then: the text of a type without fields, {@code "[  ]"}, is returned
     */
    @Test
    void getStructureFields_requiresReadOnType() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, false);

        assertEquals(NO_FIELDS, new StructureAjax().getStructureFields(TYPE_A));
    }

    /**
     * Given: a user without READ on the content type
     * When: getSearchableStructureFields is called
     * Then: an empty list is returned
     */
    @Test
    void getSearchableStructureFields_requiresReadOnType() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, false);

        final List<Map> fields = new StructureAjax().getSearchableStructureFields(TYPE_A);

        assertNotNull(fields);
        assertTrue(fields.isEmpty(), "no field must be returned, got " + fields);
    }

    /**
     * Given: a user with READ on the content type
     * When: fetchByIdentity, checkDependencies, getStructureFields and getSearchableStructureFields are called
     * Then: each returns the type's data
     */
    @Test
    void typeReads_returnDataWithReadPermission() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        final StructureAjax structureAjax = new StructureAjax();

        final Map<String, Object> type = structureAjax.fetchByIdentity(TYPE_A);
        assertNotNull(type);
        assertEquals(TYPE_A, type.get("inode"));

        final Map<Object, Object> dependencies = structureAjax.checkDependencies(TYPE_A);
        assertEquals(1, dependencies.get("size"));

        assertTrue(structureAjax.getStructureFields(TYPE_A).contains(FIELD_A1));

        final List<Map> searchable = structureAjax.getSearchableStructureFields(TYPE_A);
        assertEquals(2, searchable.size());
        assertEquals(FIELD_A1, searchable.get(0).get("inode"));
    }

    /**
     * Given: an id that resolves to no content type
     * When: fetchByIdentity is called
     * Then: null is returned and nothing fails
     */
    @Test
    void fetchByIdentity_unknownTypeReturnsNull() throws Exception {
        assertNull(new StructureAjax().fetchByIdentity(UNKNOWN_TYPE));
    }

    /** A call to one of the readers that render a field's options. */
    @FunctionalInterface
    interface FieldOptionsReader {
        String render(StructureAjax ajax, String fieldInode) throws Exception;
    }

    static Stream<Arguments> fieldOptionReaders() {
        return Stream.of(
                Arguments.of("getDropDownList", (FieldOptionsReader) StructureAjax::getDropDownList),
                Arguments.of("getDropDownOptions", (FieldOptionsReader) StructureAjax::getDropDownOptions),
                Arguments.of("getRadioButtons", (FieldOptionsReader) StructureAjax::getRadioButtons),
                Arguments.of("getCheckboxes", (FieldOptionsReader) StructureAjax::getCheckboxes));
    }

    /**
     * Given: a user without READ on the content type that owns a select field
     * When: each reader of the field's options is called
     * Then: an empty string is returned
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldOptionReaders")
    void fieldOptionReaders_requireReadOnOwningType(final String method,
            final FieldOptionsReader reader) throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, false);

        assertEquals("", reader.render(new StructureAjax(), SELECT_FIELD),
                method + " must not render the options of a field the caller cannot read");
    }

    /**
     * Given: a user with READ on the content type that owns a select field
     * When: each reader of the field's options is called
     * Then: the field's options are rendered
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldOptionReaders")
    void fieldOptionReaders_renderOptionsWithReadPermission(final String method,
            final FieldOptionsReader reader) throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);

        final String html = reader.render(new StructureAjax(), SELECT_FIELD);

        assertTrue(html.contains("red") && html.contains("blue"),
                method + " must render the options, got " + html);
    }

    /**
     * Given: a field inode that does not exist
     * When: each reader of the field's options is called
     * Then: an empty string is returned and nothing fails
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldOptionReaders")
    void fieldOptionReaders_unknownFieldReturnsEmpty(final String method,
            final FieldOptionsReader reader) {
        final String html = assertDoesNotThrow(() -> reader.render(new StructureAjax(), UNKNOWN_FIELD),
                method + " must answer an unknown field like a field the caller cannot read");
        assertEquals("", html);
    }

    /**
     * Given: a user without READ on a content type that has a category field
     * When: getCategoriesTree is called with the type's variable name
     * Then: an empty list is returned and no category is looked up
     */
    @Test
    void getCategoriesTree_requiresReadOnType() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, false);
        givenCategoryFieldOnTypeA();

        final List<Map<String, Object>> tree = new StructureAjax().getCategoriesTree("typeA", ".*");

        assertTrue(tree.isEmpty(), "no category must be returned, got " + tree);
        verify(categoryAPI, never()).find(anyString(), any(User.class), anyBoolean());
    }

    /**
     * Given: a user with READ on a content type that has a category field, and READ on that
     * category
     * When: getCategoriesTree is called with the type's variable name
     * Then: the category of the field is returned
     */
    @Test
    void getCategoriesTree_returnsCategoriesWithReadPermission() throws Exception {
        givenPermissionOnTypeA(PERMISSION_READ, true);
        givenCategoryFieldOnTypeA();
        final Category category = new Category();
        category.setInode("category-inode");
        category.setCategoryName("Colours");
        when(categoryAPI.find(eq("category-inode"), eq(user), anyBoolean())).thenReturn(category);

        final List<Map<String, Object>> tree = new StructureAjax().getCategoriesTree("typeA", ".*");

        assertEquals(1, tree.size(), "the field's category must be returned, got " + tree);
        assertEquals("category-inode", tree.get(0).get("inode"));
        assertEquals("Colours", tree.get(0).get("categoryName"));
    }
}
