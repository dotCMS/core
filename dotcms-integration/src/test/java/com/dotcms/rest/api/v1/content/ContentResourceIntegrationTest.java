package com.dotcms.rest.api.v1.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
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
