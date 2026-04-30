package com.dotcms.rest.api.v1.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.CountView;
import com.dotcms.rest.ResponseEntityCountView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ContentResource} (v1 endpoint: /api/v1/content).
 */
public class ContentResourceIntegrationTest {

    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * Method to test: {@link ContentResource#getContent}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>Parent content type with a relationship field to a child content type</li>
     *     <li>Child content exists ONLY in default language (EN)</li>
     *     <li>Parent content exists ONLY in default language (EN), related to the child</li>
     *     <li>DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is enabled (fallback active)</li>
     *     <li>Request the parent with a non-default language (Spanish) and depth=1</li>
     * </ul>
     * <p>
     * Expected result: The parent should resolve via fallback to the default language,
     * and the relationship should be hydrated using the contentlet's actual language (EN),
     * returning the related child content correctly.
     * <p>
     * Before the fix, the relationship was hydrated using the request's language (Spanish),
     * which could fail to find the related content in that language.
     */
    @Test
    public void test_getContent_withFallbackLanguage_shouldHydrateRelationshipsWithContentletLanguage()
            throws Exception {

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        // Save original config and enable fallback
        final boolean originalConfig = Config.getBooleanProperty(
                "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);

        try {
            // Create child content type (simple: title)
            final ContentType childContentType = new ContentTypeDataGen()
                    .fields(List.of(
                            new FieldDataGen().name("Title")
                                    .velocityVarName("title").next()
                    ))
                    .nextPersisted();

            // Create parent content type with relationship field to child
            final ContentType parentContentType = new ContentTypeDataGen()
                    .fields(List.of(
                            new FieldDataGen().name("Title")
                                    .velocityVarName("title").next(),
                            new FieldDataGen().type(RelationshipField.class)
                                    .name("Children")
                                    .velocityVarName("children")
                                    .relationType(childContentType.variable())
                                    .values(String.valueOf(
                                            RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                                    .next()
                    ))
                    .nextPersisted();

            // Get the relationship object
            final com.dotcms.contenttype.model.field.Field relationshipField =
                    parentContentType.fields(RelationshipField.class).get(0);
            final Relationship relationship = APILocator.getRelationshipAPI()
                    .getRelationshipFromField(relationshipField, systemUser);

            // Create child content in default language (EN) only
            final Contentlet childEN = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty("title", "Child EN Title")
                    .nextPersisted();

            // Create parent content in default language (EN) with relationship to child
            Contentlet parentEN = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty("title", "Parent EN Title")
                    .next();
            parentEN.setIndexPolicy(IndexPolicy.FORCE);
            parentEN = APILocator.getContentletAPI().checkin(parentEN,
                    Map.of(relationship, List.of(childEN)), systemUser, false);

            // Create new ContentResource instance (gets fresh Lazy config evaluation)
            final ContentResource contentResource = new ContentResource();

            // Build request with admin auth
            final HttpServletRequest request = createAuthenticatedRequest();
            final HttpServletResponse response = mock(HttpServletResponse.class);

            // Request the parent with SPANISH language and depth=1
            // Parent doesn't exist in Spanish -> fallback to EN
            // Fix ensures relationships are hydrated with EN (contentlet's language),
            // not Spanish (request's language)
            final Response endpointResponse = contentResource.getContent(
                    request, response,
                    parentEN.getIdentifier(),
                    String.valueOf(spanishLanguage.getId()),
                    "DEFAULT",
                    1);

            // Verify the response is successful
            assertEquals(Status.OK.getStatusCode(), endpointResponse.getStatus());

            // Extract the contentlet map from the response
            @SuppressWarnings("unchecked")
            final ResponseEntityView<Map<String, Object>> entityView =
                    (ResponseEntityView<Map<String, Object>>) endpointResponse.getEntity();
            final Map<String, Object> contentletMap = entityView.getEntity();

            assertNotNull("Response map should not be null", contentletMap);

            // Verify the contentlet resolved in default language (EN)
            assertEquals("Contentlet should be in default language",
                    defaultLanguage.getId(),
                    Long.parseLong(contentletMap.get("languageId").toString()));

            // Verify the relationship field is present and contains the child
            final Object relationshipValue = contentletMap.get(relationshipField.variable());
            assertNotNull("Relationship field should be present in response",
                    relationshipValue);

            assertTrue("Relationship value should be a list",
                    relationshipValue instanceof List);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> relatedItems =
                    (List<Map<String, Object>>) relationshipValue;
            assertEquals("Should have one related child", 1, relatedItems.size());

            final Map<String, Object> relatedChild = relatedItems.get(0);
            assertEquals("Related child should have the correct identifier",
                    childEN.getIdentifier(), relatedChild.get("identifier"));
            assertEquals("Related child should be in default language",
                    defaultLanguage.getId(),
                    Long.parseLong(relatedChild.get("languageId").toString()));

        } finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", originalConfig);
        }
    }

    /**
     * Method to test: {@link ContentResource#getContent}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>Parent content type with a relationship field to a child content type</li>
     *     <li>Child content exists in both EN and ES</li>
     *     <li>Parent content exists in both EN and ES, related to the child</li>
     *     <li>Request the parent with Spanish language and depth=1</li>
     * </ul>
     * <p>
     * Expected result: The parent should resolve in Spanish, and the relationship
     * should return the Spanish version of the related child.
     */
    @Test
    public void test_getContent_withMultiLanguageContent_shouldReturnRelationshipsInRequestedLanguage()
            throws Exception {

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        // Create child content type
        final ContentType childContentType = new ContentTypeDataGen()
                .fields(List.of(
                        new FieldDataGen().name("Title")
                                .velocityVarName("title").next()
                ))
                .nextPersisted();

        // Create parent content type with relationship field to child
        final ContentType parentContentType = new ContentTypeDataGen()
                .fields(List.of(
                        new FieldDataGen().name("Title")
                                .velocityVarName("title").next(),
                        new FieldDataGen().type(RelationshipField.class)
                                .name("Children")
                                .velocityVarName("children")
                                .relationType(childContentType.variable())
                                .values(String.valueOf(
                                        RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                                .next()
                ))
                .nextPersisted();

        // Get the relationship object
        final com.dotcms.contenttype.model.field.Field relationshipField =
                parentContentType.fields(RelationshipField.class).get(0);
        final Relationship relationship = APILocator.getRelationshipAPI()
                .getRelationshipFromField(relationshipField, systemUser);

        // Create child content in EN
        final Contentlet childEN = new ContentletDataGen(childContentType.id())
                .languageId(defaultLanguage.getId())
                .setProperty("title", "Child EN Title")
                .nextPersisted();

        // Create child content in ES (same identifier, different language)
        final Contentlet childESCheckout = ContentletDataGen.checkout(childEN);
        childESCheckout.setLanguageId(spanishLanguage.getId());
        childESCheckout.setProperty("title", "Child ES Title");
        final Contentlet childES = ContentletDataGen.checkin(childESCheckout);

        // Create parent content in EN with relationship to child
        Contentlet parentEN = new ContentletDataGen(parentContentType.id())
                .languageId(defaultLanguage.getId())
                .setProperty("title", "Parent EN Title")
                .next();
        parentEN.setIndexPolicy(IndexPolicy.FORCE);
        parentEN = APILocator.getContentletAPI().checkin(parentEN,
                Map.of(relationship, List.of(childEN)), systemUser, false);

        // Create parent content in ES with relationship to child ES
        Contentlet parentESCheckout = ContentletDataGen.checkout(parentEN);
        parentESCheckout.setLanguageId(spanishLanguage.getId());
        parentESCheckout.setProperty("title", "Parent ES Title");
        parentESCheckout.setIndexPolicy(IndexPolicy.FORCE);
        final Contentlet parentES = APILocator.getContentletAPI().checkin(parentESCheckout,
                Map.of(relationship, List.of(childES)), systemUser, false);

        final ContentResource contentResource = new ContentResource();
        final HttpServletRequest request = createAuthenticatedRequest();
        final HttpServletResponse response = mock(HttpServletResponse.class);

        // Request the parent with Spanish language and depth=1
        final Response endpointResponse = contentResource.getContent(
                request, response,
                parentEN.getIdentifier(),
                String.valueOf(spanishLanguage.getId()),
                "DEFAULT",
                1);

        assertEquals(Status.OK.getStatusCode(), endpointResponse.getStatus());

        @SuppressWarnings("unchecked")
        final ResponseEntityView<Map<String, Object>> entityView =
                (ResponseEntityView<Map<String, Object>>) endpointResponse.getEntity();
        final Map<String, Object> contentletMap = entityView.getEntity();

        assertNotNull("Response map should not be null", contentletMap);

        // Verify the contentlet resolved in Spanish
        assertEquals("Contentlet should be in Spanish",
                spanishLanguage.getId(),
                Long.parseLong(contentletMap.get("languageId").toString()));

        // Verify the relationship returns the Spanish child
        final Object relationshipValue = contentletMap.get(relationshipField.variable());
        assertNotNull("Relationship field should be present", relationshipValue);
        assertTrue("Relationship value should be a list",
                relationshipValue instanceof List);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> relatedItems =
                (List<Map<String, Object>>) relationshipValue;
        assertEquals("Should have one related child", 1, relatedItems.size());

        final Map<String, Object> relatedChild = relatedItems.get(0);
        assertEquals("Related child should have correct identifier",
                childEN.getIdentifier(), relatedChild.get("identifier"));
        assertEquals("Related child should be in Spanish",
                spanishLanguage.getId(),
                Long.parseLong(relatedChild.get("languageId").toString()));
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A contentlet in Spanish is placed on two different pages inside the same
     *     container</li>
     *     <li>The first placement has no explicit personalization (default visitor)</li>
     *     <li>The second placement is tied to a specific Persona</li>
     * </ul>
     * <p>
     * Expected result: Both {@link ContentReferenceView} entries are returned with the correct
     * page identifier, container inode, and personaName. The default-visitor entry carries a
     * non-null "Default Visitor" label, and the persona entry carries the persona's display
     * name. This guards against the key-mismatch bug where the API layer stored the value under
     * {@code "persona"} while the resource layer read {@code "personaName"}, causing
     * {@code personaName} to always be {@code null}.
     */
    @Test
    public void test_getContentletReferences_allFieldsPopulated() throws Exception {
        final Language spanish = TestDataUtils.getSpanishLanguage();

        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();

        final HTMLPageAsset pageDefault = new HTMLPageDataGen(folder, template)
                .languageId(spanish.getId()).nextPersisted();
        final HTMLPageAsset pagePersona = new HTMLPageDataGen(folder, template)
                .languageId(spanish.getId()).nextPersisted();

        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(spanish.getId()).nextPersisted();

        final Persona persona = new PersonaDataGen()
                .keyTag(UUIDGenerator.shorty()).nextPersisted();
        final String personalization = Persona.DOT_PERSONA_PREFIX_SCHEME
                + StringPool.COLON + persona.getKeyTag();

        // default-visitor reference (no explicit personalization)
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                pageDefault.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 0));

        // persona-specific reference
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                pagePersona.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 0, personalization));

        final ContentResource contentResource = new ContentResource();
        final HttpServletRequest request = createAuthenticatedRequest();
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final ResponseEntityContentReferenceListView entityView =
                contentResource.getContentletReferences(
                        request, response,
                        content.getInode(),
                        String.valueOf(spanish.getId()));

        final List<ContentReferenceView> views = entityView.getEntity();

        assertNotNull(views);
        assertEquals("Expected two references (default + persona)", 2, views.size());

        final ContentReferenceView defaultView = views.stream()
                .filter(v -> pageDefault.getIdentifier().equals(v.getPage().getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Default-visitor reference not found"));

        // page
        assertEquals(pageDefault.getIdentifier(), defaultView.getPage().getIdentifier());
        // container
        assertEquals(container.getInode(),
                defaultView.getContainer().getContainer().getInode());
        // personaName — must be non-null (was always null before the key-mismatch fix)
        assertNotNull("personaName must not be null for default-visitor reference",
                defaultView.getPersonaName());
        assertFalse("personaName must not be blank for default-visitor reference",
                defaultView.getPersonaName().isBlank());

        final ContentReferenceView personaView = views.stream()
                .filter(v -> pagePersona.getIdentifier().equals(v.getPage().getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Persona reference not found"));

        // page
        assertEquals(pagePersona.getIdentifier(), personaView.getPage().getIdentifier());
        // container
        assertEquals(container.getInode(),
                personaView.getContainer().getContainer().getInode());
        // personaName — must equal the persisted persona's display name
        assertEquals("personaName must match persona display name",
                persona.getName(), personaView.getPersonaName());
    }

    // -------------------------------------------------------------------------
    // Tests for GET /{inodeOrIdentifier}/references
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A contentlet is created in Spanish</li>
     *     <li>The contentlet is not placed on any page (no MultiTree entries)</li>
     * </ul>
     * <p>
     * Expected result: The endpoint returns a non-null empty list.
     */
    @Test
    public void test_getContentletReferences_noReferences_returnsEmptyList() throws Exception {
        final Language spanish = TestDataUtils.getSpanishLanguage();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(spanish.getId()).nextPersisted();

        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        content.getInode(), String.valueOf(spanish.getId()))
                .getEntity();

        assertNotNull("Result must not be null even when empty", views);
        assertTrue("Expected empty list when content has no references", views.isEmpty());
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>English contentlet is placed on an English page</li>
     *     <li>The endpoint is called with {@code ?language=<englishId>}</li>
     * </ul>
     * <p>
     * Expected result: The reference is returned and the page identifier matches the placed page.
     * This verifies that the {@code ?language} query parameter is wired into the language filter.
     */
    @Test
    public void test_getContentletReferences_englishContentAndPage_withLanguageParam_returnsReference()
            throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(english.getId()).nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 0));

        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        content.getInode(), String.valueOf(english.getId()))
                .getEntity();

        assertFalse("Expected at least one reference", views.isEmpty());
        assertEquals(page.getIdentifier(), views.get(0).getPage().getIdentifier());
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>Spanish contentlet is placed on a Spanish page</li>
     *     <li>The endpoint is called with {@code ?language=<spanishId>}</li>
     * </ul>
     * <p>
     * Expected result: The reference is returned and the page identifier matches the placed page.
     * This verifies the {@code ?language} query parameter works for non-default languages.
     */
    @Test
    public void test_getContentletReferences_spanishContentAndPage_withLanguageParam_returnsReference()
            throws Exception {
        final Language spanish = TestDataUtils.getSpanishLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(spanish.getId()).nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(spanish.getId()).nextPersisted();

        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 0));

        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        content.getInode(), String.valueOf(spanish.getId()))
                .getEntity();

        assertFalse("Expected at least one reference", views.isEmpty());
        assertEquals(page.getIdentifier(), views.get(0).getPage().getIdentifier());
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A page exists only in Spanish</li>
     *     <li>An English contentlet is placed on that Spanish-only page via MultiTree</li>
     *     <li>The endpoint is called for the English contentlet with
     *     {@code ?language=<englishId>}</li>
     * </ul>
     * <p>
     * Expected result: The endpoint returns an empty list because the page language does not
     * match the contentlet language and there is no English fallback page.
     */
    @Test
    public void test_getContentletReferences_languageMismatch_returnsEmptyList() throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language spanish = TestDataUtils.getSpanishLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();

        // The page exists only in Spanish
        final HTMLPageAsset spanishPage = new HTMLPageDataGen(folder, template)
                .languageId(spanish.getId()).nextPersisted();
        // The content is in English
        final Contentlet englishContent = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        // Place the English content on the Spanish page
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                spanishPage.getIdentifier(), container.getIdentifier(),
                englishContent.getIdentifier(), uuid, 0));

        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        englishContent.getInode(), String.valueOf(english.getId()))
                .getEntity();

        assertTrue("References should be empty when page language doesn't match content language",
                views.isEmpty());
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A page exists only in English (monolingual)</li>
     *     <li>A Spanish contentlet is placed on that English-only page</li>
     *     <li>The endpoint is called for the Spanish contentlet with
     *     {@code ?language=<spanishId>}</li>
     * </ul>
     * <p>
     * Expected result: The endpoint falls back to the English page and returns it in the
     * reference list — the contentlet's language does not have a page of its own but the
     * fallback page exists.
     */
    @Test
    public void test_getContentletReferences_monolingualPage_fallsBackToEnglishPage()
            throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language spanish = TestDataUtils.getSpanishLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();

        // Page exists only in English (monolingual)
        final HTMLPageAsset englishPage = new HTMLPageDataGen(folder, template)
                .languageId(english.getId()).nextPersisted();
        final Contentlet spanishContent = new ContentletDataGen(structure.getInode())
                .languageId(spanish.getId()).nextPersisted();

        // Spanish content is placed on the English page
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                englishPage.getIdentifier(), container.getIdentifier(),
                spanishContent.getIdentifier(), uuid, 0));

        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        spanishContent.getInode(), String.valueOf(spanish.getId()))
                .getEntity();

        assertFalse("Expected the English fallback page to be returned", views.isEmpty());
        assertEquals(englishPage.getIdentifier(), views.get(0).getPage().getIdentifier());
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>English contentlet is placed on an English page</li>
     *     <li>The endpoint is called with an empty {@code ?language} parameter (the
     *     {@code @DefaultValue("")} behavior)</li>
     * </ul>
     * <p>
     * Expected result: When the language parameter is empty the endpoint resolves the language
     * from the session (defaulting to English in the test environment) and returns the
     * reference.
     */
    @Test
    public void test_getContentletReferences_noLanguageParam_resolvesFromSession() throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(english.getId()).nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 0));

        // Pass the @DefaultValue("") — session language (default = English) is used instead
        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        content.getInode(), "")
                .getEntity();

        assertFalse("Expected a reference when language resolves from session", views.isEmpty());
        assertEquals(page.getIdentifier(), views.get(0).getPage().getIdentifier());
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A Spanish contentlet is placed on the same page and container twice</li>
     *     <li>The first placement uses no explicit personalization (default visitor)</li>
     *     <li>The second placement uses a specific Persona personalization</li>
     * </ul>
     * <p>
     * Expected result: Both references are returned — one with the default-visitor persona name
     * and one with the named persona's display name.
     */
    @Test
    public void test_getContentletReferences_samePageAndContainer_differentPersonas_returnsBoth()
            throws Exception {
        final Language spanish = TestDataUtils.getSpanishLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(spanish.getId()).nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(spanish.getId()).nextPersisted();

        final Persona persona = new PersonaDataGen()
                .keyTag(UUIDGenerator.shorty()).nextPersisted();
        final String personalization = Persona.DOT_PERSONA_PREFIX_SCHEME
                + StringPool.COLON + persona.getKeyTag();

        // Default-visitor entry on the same page+container
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 0));

        // Named-persona entry on the same page+container
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 1, personalization));

        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        content.getInode(), String.valueOf(spanish.getId()))
                .getEntity();

        assertEquals("Both default-visitor and named-persona entries must be returned", 2,
                views.size());
        assertTrue("One entry must carry the named persona's display name",
                views.stream().anyMatch(v -> persona.getName().equals(v.getPersonaName())));
        assertTrue("One entry must carry a non-null default-visitor name",
                views.stream().anyMatch(v -> v.getPersonaName() != null
                        && !persona.getName().equals(v.getPersonaName())));
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>English contentlet is placed on an English page</li>
     *     <li>The endpoint is called using the contentlet's identifier (not its inode) as the
     *     path parameter</li>
     * </ul>
     * <p>
     * Expected result: The reference is returned. Confirms the path parameter accepts both
     * identifier and inode.
     */
    @Test
    public void test_getContentletReferences_byIdentifier_returnsReference() throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(english.getId()).nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), uuid, 0));

        // Use identifier instead of inode
        final List<ContentReferenceView> views = new ContentResource()
                .getContentletReferences(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        content.getIdentifier(), String.valueOf(english.getId()))
                .getEntity();

        assertFalse("Expected a reference when looking up by identifier", views.isEmpty());
        assertEquals(page.getIdentifier(), views.get(0).getPage().getIdentifier());
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A random UUID is used as the path parameter — no contentlet with that
     *     inode/identifier exists</li>
     * </ul>
     * <p>
     * Expected result: The endpoint throws {@link DoesNotExistException}, which the JAX-RS
     * exception mapper translates into an HTTP 404 response.
     */
    @Test(expected = DoesNotExistException.class)
    public void test_getContentletReferences_nonExistentId_throwsDoesNotExist() throws Exception {
        new ContentResource().getContentletReferences(
                createAuthenticatedRequest(), mock(HttpServletResponse.class),
                UUIDGenerator.generateUuid(), // random UUID — never persisted
                String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId()));
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A limited user is created with only backend and frontend roles (no admin)</li>
     *     <li>A contentlet is created and its individual permissions are overridden to grant
     *     READ only to the system user's role</li>
     *     <li>The endpoint is called authenticated as the limited user</li>
     * </ul>
     * <p>
     * Expected result: The endpoint throws {@link DotSecurityException}, which the JAX-RS
     * exception mapper translates into an HTTP 403 response.
     */
    @Test(expected = DotSecurityException.class)
    public void test_getContentletReferences_noReadPermission_throwsSecurityException()
            throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final String password = "TestPass" + System.currentTimeMillis() + "!";
        final User limitedUser = new UserDataGen()
                .password(password)
                .roles(TestUserUtils.getBackendRole(), TestUserUtils.getFrontendRole())
                .nextPersisted();

        final Structure structure = new StructureDataGen().nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        // Set individual permissions: only admin role gets READ, overriding inherited permissions
        final Permission adminOnlyRead = new Permission(
                content.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(
                        APILocator.getUserAPI().getSystemUser().getUserId()).getId(),
                PermissionAPI.PERMISSION_READ,
                true);
        APILocator.getPermissionAPI().save(adminOnlyRead, content,
                APILocator.systemUser(), false);

        new ContentResource().getContentletReferences(
                createRequestForUser(limitedUser.getEmailAddress(), password),
                mock(HttpServletResponse.class),
                content.getInode(),
                String.valueOf(english.getId()));
    }

    // -------------------------------------------------------------------------
    // Tests for GET /{identifier}/references/count
    // -------------------------------------------------------------------------

    /**
     * Method to test: {@link ContentResource#getAllContentletReferencesCount}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A contentlet is placed on two different pages (one English, one Spanish) in the
     *     same container</li>
     *     <li>The count endpoint is called with the contentlet's identifier</li>
     * </ul>
     * <p>
     * Expected result: The count is 2 — the endpoint counts every MultiTree entry regardless of
     * language or persona.
     */
    @Test
    public void test_getAllContentletReferencesCount_withReferences_returnsCorrectCount()
            throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language spanish = TestDataUtils.getSpanishLanguage();
        final String uuid = UUIDGenerator.generateUuid();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Container container = new ContainerDataGen()
                .withStructure(structure, "").nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();

        final HTMLPageAsset pageEN = new HTMLPageDataGen(folder, template)
                .languageId(english.getId()).nextPersisted();
        final HTMLPageAsset pageES = new HTMLPageDataGen(folder, template)
                .languageId(spanish.getId()).nextPersisted();

        // Create content with two language versions using the same identifier
        final Contentlet contentEN = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                pageEN.getIdentifier(), container.getIdentifier(),
                contentEN.getIdentifier(), uuid, 0));
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(
                pageES.getIdentifier(), container.getIdentifier(),
                contentEN.getIdentifier(), uuid, 0));

        final CountView countView = new ContentResource()
                .getAllContentletReferencesCount(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        contentEN.getIdentifier())
                .getEntity();

        assertEquals("Count must equal the number of MultiTree entries", 2, countView.getCount());
    }

    /**
     * Method to test: {@link ContentResource#getAllContentletReferencesCount}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A contentlet is created but never placed on any page</li>
     *     <li>The count endpoint is called with the contentlet's identifier</li>
     * </ul>
     * <p>
     * Expected result: The count is 0.
     */
    @Test
    public void test_getAllContentletReferencesCount_noReferences_returnsZero() throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        final CountView countView = new ContentResource()
                .getAllContentletReferencesCount(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        content.getIdentifier())
                .getEntity();

        assertEquals("Count must be 0 when content has no references", 0, countView.getCount());
    }

    /**
     * Method to test: {@link ContentResource#getAllContentletReferencesCount}
     * <p>
     * Given scenario:
     * <ul>
     *     <li>A random UUID is used as the identifier — no contentlet with that identifier
     *     exists</li>
     * </ul>
     * <p>
     * Expected result: The endpoint returns count = 0. Note that the
     * {@link DoesNotExistException} guard in the resource is unreachable with the current
     * {@code ESContentletAPIImpl} implementation, which always returns
     * {@code Optional.of(count)}, so a non-existent identifier simply resolves to a count of 0
     * rather than a 404.
     */
    @Test
    public void test_getAllContentletReferencesCount_nonExistentIdentifier_returnsZero()
            throws Exception {
        final CountView countView = new ContentResource()
                .getAllContentletReferencesCount(
                        createAuthenticatedRequest(), mock(HttpServletResponse.class),
                        UUIDGenerator.generateUuid()) // never persisted
                .getEntity();

        assertEquals("Count must be 0 for a non-existent identifier", 0, countView.getCount());
    }

    /**
     * Creates an authenticated HttpServletRequest for a specific user.
     */
    private static HttpServletRequest createRequestForUser(final String email,
            final String password) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request()
                        ).request()
                ).request()
        );
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(
                        (email + ":" + password).getBytes()));
        return request;
    }

    /**
     * Method to test: {@link ContentResource#getContentletReferences}
     * <p>
     * Given scenario: An unauthenticated (anonymous) HTTP request hits the endpoint,
     * which requires {@code AnonymousAccess.NONE}.
     * <p>
     * Expected result: {@link com.dotcms.rest.exception.SecurityException} is thrown,
     * mapping to HTTP 401 — anonymous callers are rejected before any content lookup.
     */
    @Test(expected = com.dotcms.rest.exception.SecurityException.class)
    public void test_getContentletReferences_anonymousRequest_throwsUnauthorized()
            throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        new ContentResource().getContentletReferences(
                createAnonymousRequest(), mock(HttpServletResponse.class),
                content.getIdentifier(), "");
    }

    /**
     * Method to test: {@link ContentResource#getAllContentletReferencesCount}
     * <p>
     * Given scenario: An unauthenticated (anonymous) HTTP request hits the endpoint,
     * which requires {@code AnonymousAccess.NONE}.
     * <p>
     * Expected result: {@link com.dotcms.rest.exception.SecurityException} is thrown,
     * mapping to HTTP 401 — anonymous callers are rejected before any count lookup.
     */
    @Test(expected = com.dotcms.rest.exception.SecurityException.class)
    public void test_getAllContentletReferencesCount_anonymousRequest_throwsUnauthorized()
            throws Exception {
        final Language english = APILocator.getLanguageAPI().getDefaultLanguage();
        final Structure structure = new StructureDataGen().nextPersisted();
        final Contentlet content = new ContentletDataGen(structure.getInode())
                .languageId(english.getId()).nextPersisted();

        new ContentResource().getAllContentletReferencesCount(
                createAnonymousRequest(), mock(HttpServletResponse.class),
                content.getIdentifier());
    }

    /**
     * Creates an unauthenticated (anonymous) HttpServletRequest with no user or auth header.
     */
    private static HttpServletRequest createAnonymousRequest() {
        return new MockAttributeRequest(
                new MockSessionRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()
                ).request()
        ).request();
    }

    /**
     * Creates an authenticated HttpServletRequest with admin credentials.
     */
    private static HttpServletRequest createAuthenticatedRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request()
                        ).request()
                ).request()
        );
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(
                        "admin@dotcms.com:admin".getBytes()));
        return request;
    }
}
