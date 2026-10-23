package com.dotcms.contenttype.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Unit tests for how {@link StoryBlockAPIImpl} resolves the user on whose behalf referenced
 * contentlets are looked up while refreshing Story Block values, and for what the refreshed value
 * looks like when a referenced contentlet cannot be read by that user.
 * <p>
 * {@code APILocator}, {@code WebAPILocator} and {@code ContentUtils} are static mocks; the current
 * request is set in {@code HttpServletRequestThreadLocal} and restored after each test.
 */
public class StoryBlockAPIImplReferenceAccessTest {

    private static final String PARENT_ID = "parent-id";
    private static final String REF_ID = "ref-id";
    private static final String REF_INODE = "ref-inode";
    private static final String STORED_TITLE = "stored title";

    /** A document with a paragraph and one content reference whose stored data must be preserved or cleared. */
    private static final String STORY_BLOCK_JSON = "{\"type\":\"doc\",\"content\":["
            + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]},"
            + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"" + REF_ID + "\","
            + "\"languageId\":1,\"inode\":\"old-inode\",\"title\":\"" + STORED_TITLE + "\"}}}]}";

    /** Same document, with the reference's data stored as a JSON string instead of an object. */
    private static final String STORY_BLOCK_JSON_WITH_STRING_DATA = "{\"type\":\"doc\",\"content\":["
            + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]},"
            + "{\"type\":\"dotContent\",\"attrs\":{\"data\":\"{\\\"identifier\\\":\\\"" + REF_ID + "\\\","
            + "\\\"languageId\\\":1,\\\"title\\\":\\\"" + STORED_TITLE + "\\\"}\"}}]}";

    private static final String REF_TYPE_ID = "ref-type-id";

    private final StoryBlockAPIImpl storyBlockAPI = new StoryBlockAPIImpl();

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<WebAPILocator> webApiLocator;
    private MockedStatic<ContentUtils> contentUtils;
    private HttpServletRequest previousRequest;

    private final Map<String, Object> parentProperties = new HashMap<>();

    private ContentletAPI contentletAPI;
    private VersionableAPI versionableAPI;
    private PermissionAPI permissionAPI;
    private UserWebAPI userWebAPI;
    private User systemUser;
    private User anonymousUser;
    private User backendUser;

    @BeforeEach
    public void setUp() throws DotDataException {
        previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        contentletAPI = mock(ContentletAPI.class);
        userWebAPI = mock(UserWebAPI.class);
        systemUser = mock(User.class);
        anonymousUser = mock(User.class);
        backendUser = mock(User.class);
        when(backendUser.isBackendUser()).thenReturn(true);

        final ContentletVersionInfo versionInfo = mock(ContentletVersionInfo.class);
        when(versionInfo.getLiveInode()).thenReturn(REF_INODE);
        when(versionInfo.getWorkingInode()).thenReturn(REF_INODE);
        versionableAPI = mock(VersionableAPI.class);
        when(versionableAPI.getContentletVersionInfo(REF_ID, 1L)).thenReturn(Optional.of(versionInfo));
        permissionAPI = mock(PermissionAPI.class);

        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.getAnonymousUserNoThrow()).thenReturn(anonymousUser);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        apiLocator.when(APILocator::getVersionableAPI).thenReturn(versionableAPI);
        apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);

        webApiLocator = mockStatic(WebAPILocator.class);
        webApiLocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);

        contentUtils = mockStatic(ContentUtils.class);
    }

    @AfterEach
    public void tearDown() {
        contentUtils.close();
        webApiLocator.close();
        apiLocator.close();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
    }

    // ------------------------------------------------------------------ refreshReferences(Contentlet)

    @Test
    public void refreshReferences_withoutRequest_leavesValueUntouched() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        final Contentlet parent = parentWithStoryBlock(STORY_BLOCK_JSON);

        final StoryBlockReferenceResult result = storyBlockAPI.refreshReferences(parent);

        assertFalse(result.isRefreshed());
        assertSame(parent, result.getValue());
        verifyNoInteractions(contentletAPI);
    }

    @Test
    public void refreshReferences_looksUpReferencesAsTheLoggedInUser() throws Exception {
        final HttpServletRequest request = requestFor(backendUser, PageMode.LIVE);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(backendUser);

        storyBlockAPI.refreshReferences(parentWithStoryBlock(STORY_BLOCK_JSON));

        verify(contentletAPI).find(REF_INODE, backendUser, true, true);
        verify(contentletAPI, never()).find(anyString(), eq(systemUser), anyBoolean(), anyBoolean());
    }

    @Test
    public void refreshReferences_withoutLoggedInUser_looksUpAsAnonymous() throws Exception {
        final HttpServletRequest request = requestFor(null, null);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);

        storyBlockAPI.refreshReferences(parentWithStoryBlock(STORY_BLOCK_JSON));

        verify(contentletAPI).find(REF_INODE, anonymousUser, true, true);
    }

    @Test
    public void refreshReferences_backendUserInEditMode_doesNotApplyAnonymousPermissions() throws Exception {
        final HttpServletRequest request = requestFor(backendUser, PageMode.EDIT_MODE);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(backendUser);

        storyBlockAPI.refreshReferences(parentWithStoryBlock(STORY_BLOCK_JSON));

        verify(contentletAPI).find(REF_INODE, backendUser, false, true);
    }

    @Test
    public void refreshReferences_withExplicitUser_usesThatUserForLookups() throws Exception {
        final HttpServletRequest request = requestFor(backendUser, PageMode.LIVE);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(backendUser);
        final User explicitUser = mock(User.class);

        storyBlockAPI.refreshReferences(parentWithStoryBlock(STORY_BLOCK_JSON), explicitUser, false);

        verify(contentletAPI).find(REF_INODE, explicitUser, false, true);
        verify(contentletAPI, never()).find(anyString(), eq(backendUser), anyBoolean(), anyBoolean());
    }

    // ------------------------------------------- refreshStoryBlockValueReferences(Object, String, User, boolean)

    @Test
    public void refreshStoryBlockValueReferences_unreadableReference_clearsItsData() throws Exception {
        requestFor(null, null);
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true))
                .thenThrow(new DotSecurityException("no read permission"));

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

        assertTrue(result.isRefreshed());
        final List<Map<String, Object>> content = contentOf(result);
        assertEquals(contentOf(STORY_BLOCK_JSON).get(0), content.get(0), "the paragraph node must be untouched");
        assertEquals(Map.of(StoryBlockAPI.IDENTIFIER_KEY, REF_ID, StoryBlockAPI.LANGUAGE_ID_KEY, 1),
                dataOf(content.get(1)), "the unreadable reference must keep only the keys that identify it");
    }

    @Test
    public void refreshStoryBlockValueReferences_missingReference_keepsOnlyReferenceKeys() throws Exception {
        requestFor(null, null);
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true)).thenReturn(null);

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

        assertTrue(result.isRefreshed());
        assertEquals(referenceKeysOf(REF_ID), dataOf(contentOf(result).get(1)));
    }

    @Test
    public void refreshStoryBlockValueReferences_dataError_keepsOnlyReferenceKeys() throws Exception {
        requestFor(null, null);
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true))
                .thenThrow(new DotDataException("lookup failed"));

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

        assertTrue(result.isRefreshed());
        assertEquals(referenceKeysOf(REF_ID), dataOf(contentOf(result).get(1)));
    }

    @Test
    public void refreshStoryBlockValueReferences_referenceWithoutVersionInfo_keepsOnlyReferenceKeys() throws Exception {
        requestFor(null, null);
        final String json = STORY_BLOCK_JSON.replace(REF_ID, "unknown-id");

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(json, PARENT_ID, anonymousUser, true);

        assertTrue(result.isRefreshed());
        assertEquals(referenceKeysOf("unknown-id"), dataOf(contentOf(result).get(1)));
        verifyNoInteractions(contentletAPI);
    }

    @Test
    public void refreshStoryBlockValueReferences_dataStoredAsJsonString_isReplacedByTheRefreshedMap() throws Exception {
        requestFor(null, null);
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true))
                .thenThrow(new DotSecurityException("no read permission"));

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON_WITH_STRING_DATA, PARENT_ID, anonymousUser, true);

        assertTrue(result.isRefreshed());
        final Object data = ((Map<?, ?>) contentOf(result).get(1).get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
        assertTrue(data instanceof Map, "the stored JSON string must be replaced by the refreshed map, was: " + data);
        assertEquals(referenceKeysOf(REF_ID), data);
    }

    @Test
    public void refreshStoryBlockValueReferences_unparseableDataString_isDropped() throws Exception {
        requestFor(null, null);
        final String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\","
                + "\"attrs\":{\"data\":\"not json " + STORED_TITLE + "\"}}]}";

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(json, PARENT_ID, anonymousUser, true);

        assertTrue(result.isRefreshed());
        assertEquals(Map.of(), dataOf(contentOf(result).get(0)));
    }

    @Test
    public void refreshStoryBlockValueReferences_loadsRelationshipsAsTheRequestingUserOnACopy() throws Exception {
        requestFor(null, null);
        final Contentlet referenced = readableContentlet(Map.of());
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true)).thenReturn(referenced);

        storyBlockAPI.refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

        contentUtils.verify(() -> ContentUtils.addRelationships(
                argThat((Contentlet c) -> c != referenced && REF_ID.equals(c.getIdentifier())),
                eq(anonymousUser), any(PageMode.class), eq(1L), anyInt()));
        contentUtils.verify(() -> ContentUtils.addRelationships(
                eq(referenced), any(User.class), any(PageMode.class), anyLong(), anyInt()), never());
        contentUtils.verify(() -> ContentUtils.addRelationships(
                any(Contentlet.class), eq(systemUser), any(PageMode.class), anyLong(), anyInt()), never());
    }

    @Test
    public void refreshStoryBlockValueReferences_fileLinksOfAReadableReference_areBuiltAsBefore() throws Exception {
        requestFor(null, null);
        final BinaryField binaryField = mock(BinaryField.class);
        when(binaryField.variable()).thenReturn("asset");
        final Contentlet referenced = readableContentlet(Map.of("asset", "stored-asset-value"));
        when(referenced.getContentType().fields(BinaryField.class)).thenReturn(List.of(binaryField));
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true)).thenReturn(referenced);

        try (MockedConstruction<ResourceLink.ResourceLinkBuilder> builders = mockConstruction(
                ResourceLink.ResourceLinkBuilder.class,
                (builder, context) -> when(builder.getFileLink(any(), any(), any(), anyString()))
                        .thenReturn("/dA/ref-id/asset"))) {

            final StoryBlockReferenceResult result = storyBlockAPI
                    .refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

            assertEquals(1, builders.constructed().size());
            verify(builders.constructed().get(0))
                    .getFileLink(any(HttpServletRequest.class), eq(systemUser), any(Contentlet.class), eq("asset"));
            assertEquals("/dA/ref-id/asset", dataOf(contentOf(result).get(1)).get("asset"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void refreshStoryBlockValueReferences_storyBlockInsideAReadableReference_keepsOnlyReferenceKeys()
            throws Exception {
        requestFor(null, null);
        final StoryBlockField nestedField = mock(StoryBlockField.class);
        when(nestedField.variable()).thenReturn("nested");
        final String nestedJson = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":"
                + "{\"identifier\":\"deep-id\",\"languageId\":1,\"title\":\"deep title\"}}}]}";
        final Contentlet referenced = readableContentlet(Map.of("nested", nestedJson, "nested_raw", nestedJson));
        when(referenced.getContentType().fields()).thenReturn(List.of(nestedField));
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true)).thenReturn(referenced);

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

        final Map<String, Object> nested = (Map<String, Object>) dataOf(contentOf(result).get(1)).get("nested");
        assertNotNull(nested, "the nested Story Block must still be returned");
        final List<Map<String, Object>> nestedContent = (List<Map<String, Object>>) nested.get(StoryBlockAPI.CONTENT_KEY);
        assertEquals(referenceKeysOf("deep-id"), dataOf(nestedContent.get(0)));
    }

    // ------------------------------------------------------------ references that are not refreshed

    @Test
    public void refreshStoryBlockValueReferences_selfReferenceTheReaderCanRead_keepsItsStoredData() throws Exception {
        requestFor(null, null);
        final String json = STORY_BLOCK_JSON.replace(REF_ID, PARENT_ID);
        final Contentlet stored = storedVersionOf(PARENT_ID, "parent-inode");
        when(permissionAPI.doesUserHavePermission(stored, PermissionAPI.PERMISSION_READ, anonymousUser, true))
                .thenReturn(true);

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(json, PARENT_ID, anonymousUser, true);

        assertEquals(STORED_TITLE, referenceDataIn(result.getValue()).get("title"));
        verify(contentletAPI, never()).find(anyString(), eq(anonymousUser), anyBoolean(), anyBoolean());
    }

    @Test
    public void refreshStoryBlockValueReferences_selfReferenceTheReaderCannotRead_keepsOnlyReferenceKeys()
            throws Exception {
        requestFor(null, null);
        final String json = STORY_BLOCK_JSON.replace(REF_ID, PARENT_ID);
        final Contentlet stored = storedVersionOf(PARENT_ID, "parent-inode");
        when(permissionAPI.doesUserHavePermission(stored, PermissionAPI.PERMISSION_READ, anonymousUser, true))
                .thenReturn(false);

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(json, PARENT_ID, anonymousUser, true);

        assertEquals(referenceKeysOf(PARENT_ID), referenceDataIn(result.getValue()));
    }

    @Test
    public void refreshStoryBlockValueReferences_unreadableAudioReference_keepsOnlyReferenceKeys() throws Exception {
        requestFor(null, null);
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true))
                .thenThrow(new DotSecurityException("no read permission"));
        final String json = STORY_BLOCK_JSON.replace("\"dotContent\"", "\"dotAudio\"");

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(json, PARENT_ID, anonymousUser, true);

        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(result.getValue()));
    }

    @Test
    public void refreshStoryBlockValueReferences_valueAsMap_isProcessedLikeJson() throws Exception {
        requestFor(null, null);
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true))
                .thenThrow(new DotSecurityException("no read permission"));

        final StoryBlockReferenceResult result = storyBlockAPI.refreshStoryBlockValueReferences(
                storyBlockAPI.toMap(STORY_BLOCK_JSON), PARENT_ID, anonymousUser, true);

        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(result.getValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void limitReferencesToReader_readableReference_keepsOnlyTheRelatedContentTheReaderCanRead()
            throws Exception {
        final HttpServletRequest request = requestFor(null, null);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);
        final Contentlet stored = storedVersionOf(REF_ID, REF_INODE);
        when(permissionAPI.doesUserHavePermission(stored, PermissionAPI.PERMISSION_READ, anonymousUser, true))
                .thenReturn(true);
        final Contentlet readableRelated = storedVersionOf("rel-ok", "rel-ok-inode");
        storedVersionOf("rel-no", "rel-no-inode");
        when(permissionAPI.filterCollection(anyList(), eq(PermissionAPI.PERMISSION_READ), eq(true), eq(anonymousUser)))
                .thenAnswer(inv -> ((List<Contentlet>) inv.getArgument(0)).stream()
                        .filter(c -> c == readableRelated).collect(java.util.stream.Collectors.toList()));
        final String json = STORY_BLOCK_JSON.replace("\"title\":\"" + STORED_TITLE + "\"",
                "\"title\":\"" + STORED_TITLE + "\",\"related\":["
                        + "{\"identifier\":\"rel-ok\",\"languageId\":1,\"title\":\"related ok\"},"
                        + "{\"identifier\":\"rel-no\",\"languageId\":1,\"title\":\"related no\"}]");

        storyBlockAPI.limitReferencesToReader(parentWithStoryBlock(json));

        final List<Map<String, Object>> related =
                (List<Map<String, Object>>) referenceDataIn(parentProperties.get("body")).get("related");
        assertEquals("related ok", related.get(0).get("title"));
        assertEquals(Map.of(StoryBlockAPI.IDENTIFIER_KEY, "rel-no", StoryBlockAPI.LANGUAGE_ID_KEY, 1), related.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void limitReferencesToReader_readableReference_keepsTheNestedReferencesTheReaderCanRead()
            throws Exception {
        final HttpServletRequest request = requestFor(null, null);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);
        final Contentlet stored = storedVersionOf(REF_ID, REF_INODE);
        final Contentlet deep = storedVersionOf("deep-id", "deep-inode");
        when(permissionAPI.doesUserHavePermission(any(Contentlet.class), eq(PermissionAPI.PERMISSION_READ),
                eq(anonymousUser), eq(true))).thenAnswer(inv -> inv.getArgument(0) == stored || inv.getArgument(0) == deep);
        final String nestedJson = "{\\\"type\\\":\\\"doc\\\",\\\"content\\\":[{\\\"type\\\":\\\"dotContent\\\","
                + "\\\"attrs\\\":{\\\"data\\\":{\\\"identifier\\\":\\\"deep-id\\\",\\\"languageId\\\":1,"
                + "\\\"title\\\":\\\"deep title\\\"}}}]}";
        final String json = STORY_BLOCK_JSON.replace("\"title\":\"" + STORED_TITLE + "\"",
                "\"title\":\"" + STORED_TITLE + "\",\"nested\":\"" + nestedJson + "\"");

        storyBlockAPI.limitReferencesToReader(parentWithStoryBlock(json));

        final Object nested = referenceDataIn(parentProperties.get("body")).get("nested");
        assertEquals("deep title", dataOf(contentOf(nested).get(0)).get("title"));
    }

    @Test
    public void refreshStoryBlockValueReferences_referenceWithoutIdentifier_dropsItsData() throws Exception {
        requestFor(null, null);
        final String json = STORY_BLOCK_JSON.replace("\"identifier\":\"" + REF_ID + "\",", "");

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(json, PARENT_ID, anonymousUser, true);

        assertEquals(Map.of(StoryBlockAPI.LANGUAGE_ID_KEY, 1), referenceDataIn(result.getValue()));
    }

    @Test
    public void refreshStoryBlockValueReferences_errorBeforeTheLookup_keepsOnlyReferenceKeys() throws Exception {
        requestFor(null, null);
        when(versionableAPI.getContentletVersionInfo(REF_ID, 1L)).thenThrow(new IllegalStateException("db down"));

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(result.getValue()));
    }

    @Test
    public void refreshStoryBlockValueReferences_relationshipsFailWithDataStoredAsString_keepsOnlyReferenceKeys()
            throws Exception {
        requestFor(null, null);
        final Contentlet referenced = readableContentlet(Map.of());
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true)).thenReturn(referenced);
        contentUtils.when(() -> ContentUtils.addRelationships(any(Contentlet.class), any(User.class),
                any(PageMode.class), anyLong(), anyInt())).thenThrow(new IllegalStateException("relationships"));

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON_WITH_STRING_DATA, PARENT_ID, anonymousUser, true);

        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(result.getValue()));
    }

    @Test
    public void refreshReferences_alsoAppliesToTheRawCompanionField() throws Exception {
        final HttpServletRequest request = requestFor(null, null);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true))
                .thenThrow(new DotSecurityException("no read permission"));

        storyBlockAPI.refreshReferences(parentWithStoryBlock(STORY_BLOCK_JSON));

        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(parentProperties.get("body")));
        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(parentProperties.get("body_raw")));
    }

    @Test
    public void refreshReferences_nestedCall_appliesReadPermissionsInsteadOfReturningStoredData() throws Exception {
        final HttpServletRequest request = requestFor(null, null);
        request.setAttribute("CURRENT_DEPTH", 1);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);
        final Contentlet stored = storedVersionOf(REF_ID, REF_INODE);
        when(permissionAPI.doesUserHavePermission(stored, PermissionAPI.PERMISSION_READ, anonymousUser, true))
                .thenReturn(false);

        storyBlockAPI.refreshReferences(parentWithStoryBlock(STORY_BLOCK_JSON));

        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(parentProperties.get("body")));
        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(parentProperties.get("body_raw")));
    }

    @Test
    public void limitReferencesToReader_readableReference_keepsItsStoredData() throws Exception {
        final HttpServletRequest request = requestFor(null, null);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);
        final Contentlet stored = storedVersionOf(REF_ID, REF_INODE);
        when(permissionAPI.doesUserHavePermission(stored, PermissionAPI.PERMISSION_READ, anonymousUser, true))
                .thenReturn(true);

        storyBlockAPI.limitReferencesToReader(parentWithStoryBlock(STORY_BLOCK_JSON));

        assertEquals(STORED_TITLE, referenceDataIn(parentProperties.get("body")).get("title"));
        verify(contentletAPI, never()).find(anyString(), eq(anonymousUser), anyBoolean(), anyBoolean());
    }

    @Test
    public void limitReferencesToReader_unreadableReference_keepsOnlyReferenceKeys() throws Exception {
        final HttpServletRequest request = requestFor(null, null);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(null);
        final Contentlet stored = storedVersionOf(REF_ID, REF_INODE);
        when(permissionAPI.doesUserHavePermission(stored, PermissionAPI.PERMISSION_READ, anonymousUser, true))
                .thenReturn(false);

        storyBlockAPI.limitReferencesToReader(parentWithStoryBlock(STORY_BLOCK_JSON));

        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(parentProperties.get("body")));
        assertEquals(referenceKeysOf(REF_ID), referenceDataIn(parentProperties.get("body_raw")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void refreshStoryBlockValueReferences_readableNestedReference_keepsItsStoredData() throws Exception {
        requestFor(null, null);
        final StoryBlockField nestedField = mock(StoryBlockField.class);
        when(nestedField.variable()).thenReturn("nested");
        final String nestedJson = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":"
                + "{\"identifier\":\"deep-id\",\"languageId\":1,\"title\":\"deep title\"}}}]}";
        final Contentlet referenced = readableContentlet(Map.of("nested", nestedJson, "nested_raw", nestedJson));
        when(referenced.getContentType().fields()).thenReturn(List.of(nestedField));
        when(contentletAPI.find(REF_INODE, anonymousUser, true, true)).thenReturn(referenced);
        final Contentlet deep = storedVersionOf("deep-id", "deep-inode");
        when(permissionAPI.doesUserHavePermission(deep, PermissionAPI.PERMISSION_READ, anonymousUser, true))
                .thenReturn(true);

        final StoryBlockReferenceResult result = storyBlockAPI
                .refreshStoryBlockValueReferences(STORY_BLOCK_JSON, PARENT_ID, anonymousUser, true);

        final Map<String, Object> nested = (Map<String, Object>) referenceDataIn(result.getValue()).get("nested");
        final List<Map<String, Object>> nestedContent = (List<Map<String, Object>>) nested.get(StoryBlockAPI.CONTENT_KEY);
        assertEquals("deep title", dataOf(nestedContent.get(0)).get("title"));
    }

    // ----------------------------------------------------------------------------------- helpers

    /**
     * Installs a request in the thread-local with map-backed attributes. {@code user} becomes the
     * portal user of the request (what {@code PageMode.get} reads) and {@code mode} its page mode
     * attribute; both may be null for an anonymous request in the default mode.
     */
    private HttpServletRequest requestFor(final User user, final PageMode mode) {
        final Map<String, Object> attributes = new HashMap<>();
        if (null != user) {
            attributes.put(com.liferay.portal.util.WebKeys.USER, user);
        }
        if (null != mode) {
            attributes.put(WebKeys.PAGE_MODE_PARAMETER, mode);
        }
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(inv -> attributes.get(inv.<String>getArgument(0)));
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(inv -> attributes.remove(inv.<String>getArgument(0)))
                .when(request).removeAttribute(anyString());
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        return request;
    }

    /**
     * A contentlet whose content type has one Story Block field, {@code body}, holding
     * {@code json}, plus its raw companion {@code body_raw} with the same value. Its properties
     * live in {@link #parentProperties}, so what the API writes back can be asserted.
     */
    private Contentlet parentWithStoryBlock(final String json) {
        final StoryBlockField field = mock(StoryBlockField.class);
        when(field.variable()).thenReturn("body");
        final ContentType type = mock(ContentType.class);
        when(type.hasStoryBlockFields()).thenReturn(true);
        when(type.fields(StoryBlockField.class)).thenReturn(List.of(field));
        parentProperties.put("body", json);
        parentProperties.put("body_raw", json);
        final Contentlet parent = mock(Contentlet.class);
        when(parent.getContentType()).thenReturn(type);
        when(parent.getIdentifier()).thenReturn(PARENT_ID);
        when(parent.get(anyString())).thenAnswer(inv -> parentProperties.get(inv.<String>getArgument(0)));
        doAnswer(inv -> parentProperties.put(inv.getArgument(0), inv.getArgument(1)))
                .when(parent).setProperty(anyString(), any());
        return parent;
    }

    /** Stubs a version, readable as stored by the system user, for {@code identifier} in language 1. */
    private Contentlet storedVersionOf(final String identifier, final String inode) throws Exception {
        final ContentletVersionInfo info = mock(ContentletVersionInfo.class);
        when(info.getLiveInode()).thenReturn(inode);
        when(info.getWorkingInode()).thenReturn(inode);
        when(versionableAPI.getContentletVersionInfo(identifier, 1L)).thenReturn(Optional.of(info));
        final Contentlet stored = mock(Contentlet.class);
        when(stored.getIdentifier()).thenReturn(identifier);
        when(contentletAPI.find(inode, systemUser, false, true)).thenReturn(stored);
        return stored;
    }

    /** The {@code attrs.data} of the reference node (index 1) of a Story Block value. */
    private Map<String, Object> referenceDataIn(final Object storyBlockValue) throws Exception {
        return dataOf(contentOf(storyBlockValue).get(1));
    }

    /**
     * The referenced contentlet as returned by a successful lookup. It is a mock whose properties
     * live in a real map, so that a copy of it ({@code new Contentlet(contentlet)}) is a real
     * contentlet with the same values; both resolve to the same mocked content type, which has no
     * fields unless the test adds some.
     *
     * @param extraProperties Field values to add on top of the common properties.
     */
    private Contentlet readableContentlet(final Map<String, Object> extraProperties) throws Exception {
        final ContentType type = mock(ContentType.class);
        when(type.baseType()).thenReturn(BaseContentType.CONTENT);
        when(type.id()).thenReturn(REF_TYPE_ID);
        final ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);
        when(contentTypeAPI.find(REF_TYPE_ID)).thenReturn(type);
        apiLocator.when(() -> APILocator.getContentTypeAPI(any(User.class))).thenReturn(contentTypeAPI);

        final Map<String, Object> properties = new HashMap<>(extraProperties);
        properties.put(Contentlet.IDENTIFIER_KEY, REF_ID);
        properties.put(Contentlet.INODE_KEY, REF_INODE);
        properties.put(Contentlet.LANGUAGEID_KEY, 1L);
        properties.put(Contentlet.STRUCTURE_INODE_KEY, REF_TYPE_ID);
        properties.put(Contentlet.TITTLE_KEY, "readable title");
        properties.put(Contentlet.TITLE_IMAGE_KEY, Contentlet.TITLE_IMAGE_NOT_FOUND);

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getMap()).thenAnswer(inv -> new HashMap<>(properties));
        when(contentlet.get(anyString())).thenAnswer(inv -> properties.get(inv.<String>getArgument(0)));
        when(contentlet.getContentType()).thenReturn(type);
        when(contentlet.getIdentifier()).thenReturn(REF_ID);
        when(contentlet.getInode()).thenReturn(REF_INODE);
        when(contentlet.getLanguageId()).thenReturn(1L);
        when(contentlet.getTitleImage()).thenReturn(Optional.empty());
        return contentlet;
    }

    /** The data map a reference is reduced to when its contentlet is not served to the reader. */
    private static Map<String, Object> referenceKeysOf(final String identifier) {
        return Map.of(StoryBlockAPI.IDENTIFIER_KEY, identifier, StoryBlockAPI.LANGUAGE_ID_KEY, 1);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> contentOf(final StoryBlockReferenceResult result) throws Exception {
        return contentOf(result.getValue());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> contentOf(final Object value) throws Exception {
        return (List<Map<String, Object>>) storyBlockAPI.toMap(value).get(StoryBlockAPI.CONTENT_KEY);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(final Map<String, Object> node) {
        return (Map<String, Object>) ((Map<String, Object>) node.get(StoryBlockAPI.ATTRS_KEY))
                .get(StoryBlockAPI.DATA_KEY);
    }
}
