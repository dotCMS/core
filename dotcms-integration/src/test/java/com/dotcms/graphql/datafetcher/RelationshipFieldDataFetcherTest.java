package com.dotcms.graphql.datafetcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.exception.PermissionDeniedGraphQLException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for {@link RelationshipFieldDataFetcher}.
 *
 * <p>Regression coverage for https://github.com/dotCMS/core/issues/35037:
 * Anonymous GraphQL queries that traverse a relationship field were throwing
 * {@code Internal Server Error} even when CMS Anonymous had VIEW permission on the content type.
 */
public class RelationshipFieldDataFetcherTest {

    private static User systemUser;
    private static User anonymousUser;
    private static ContentType parentType;
    private static ContentType childType;
    private static Contentlet parentContentlet;
    private static com.dotcms.contenttype.model.field.Field relationshipField;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser    = APILocator.systemUser();
        anonymousUser = APILocator.getUserAPI().getAnonymousUser();

        // Create two content types and a relationship between them
        childType  = new ContentTypeDataGen().nextPersisted();
        parentType = new ContentTypeDataGen().nextPersisted();

        new FieldRelationshipDataGen()
                .parent(parentType)
                .child(childType)
                .persist(null);

        // Get the saved relationship field from the parent content type
        relationshipField = APILocator.getContentTypeFieldAPI()
                .byContentTypeId(parentType.id())
                .stream()
                .filter(f -> f instanceof RelationshipField)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Relationship field not found on parent type"));

        // Create and publish a live contentlet of the parent type
        parentContentlet = new ContentletDataGen(parentType).nextPersistedAndPublish();

        // Grant CMS Anonymous role READ permission on the parent content type
        final Permission readPermission = new Permission(
                parentType.getPermissionId(),
                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                PermissionAPI.PERMISSION_READ,
                true
        );
        APILocator.getPermissionAPI().save(readPermission, parentType, systemUser, false);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (parentContentlet != null) {
            ContentletDataGen.remove(parentContentlet);
        }
        if (parentType != null) {
            APILocator.getContentTypeAPI(systemUser).delete(parentType);
        }
        if (childType != null) {
            APILocator.getContentTypeAPI(systemUser).delete(childType);
        }
    }

    /**
     * Given a content type with a relationship field where CMS Anonymous has READ permission,
     * When {@link RelationshipFieldDataFetcher#get(DataFetchingEnvironment)} is called with an
     * anonymous user context,
     * Then the result should be returned without throwing an exception.
     *
     * <p>Regression test for: https://github.com/dotCMS/core/issues/35037 — before the fix,
     * passing the anonymous user to {@code getRelationshipFromField()} caused a
     * {@code DotSecurityException} because the internal {@code ContentTypeAPI} was built with
     * {@code respectFrontendRoles=false}, stripping the CMS Anonymous role from the permission
     * check. The fix catches {@code DotSecurityException} and surfaces it as a GraphQL error
     * in the response instead of throwing, consistent with the folder collection pattern.
     */
    @Test
    public void testGet_anonymousUser_withCmsAnonymousReadPermission_shouldNotThrow()
            throws Exception {

        final RelationshipFieldDataFetcher fetcher = new RelationshipFieldDataFetcher();
        final DataFetchingEnvironment environment = Mockito.mock(DataFetchingEnvironment.class);

        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(anonymousUser).build());
        Mockito.when(environment.getSource()).thenReturn(parentContentlet);
        Mockito.when(environment.getField()).thenReturn(new graphql.language.Field(relationshipField.variable()));
        Mockito.when(environment.getArgument("query")).thenReturn(null);
        Mockito.when(environment.getArgument("limit")).thenReturn(null);
        Mockito.when(environment.getArgument("offset")).thenReturn(null);
        Mockito.when(environment.getArgument("sort")).thenReturn(null);

        // Should NOT throw DotRuntimeException wrapping DotSecurityException.
        // MANY_TO_MANY cardinality always returns a List (empty when no related content exists).
        final Object result = fetcher.get(environment);

        assertTrue("Fetcher should return a List for anonymous user "
                + "when CMS Anonymous has READ permission on the content type",
                result instanceof java.util.List);
    }

    /**
     * Given a content type with a relationship field where CMS Anonymous does NOT have READ
     * permission,
     * When {@link RelationshipFieldDataFetcher#get(DataFetchingEnvironment)} is called with an
     * anonymous user context,
     * Then the result should be a {@link DataFetcherResult} containing {@code null} data and a
     * {@link PermissionDeniedGraphQLException} error — consistent with the folder collection
     * pattern of returning partial data + errors rather than throwing.
     */
    @Test
    public void testGet_anonymousUser_withoutReadPermission_shouldReturnGraphQLError()
            throws Exception {

        // Create a content type with NO anonymous read permission
        final ContentType restrictedType = new ContentTypeDataGen().nextPersisted();
        final Contentlet restrictedContentlet;
        try {
            new FieldRelationshipDataGen()
                    .parent(restrictedType)
                    .child(childType)
                    .persist(null);

            final Field restrictedRelField = APILocator.getContentTypeFieldAPI()
                    .byContentTypeId(restrictedType.id())
                    .stream()
                    .filter(f -> f instanceof RelationshipField)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Relationship field not found"));

            restrictedContentlet = new ContentletDataGen(restrictedType).nextPersistedAndPublish();

            final RelationshipFieldDataFetcher fetcher = new RelationshipFieldDataFetcher();
            final DataFetchingEnvironment environment = Mockito.mock(DataFetchingEnvironment.class);

            Mockito.when(environment.getContext()).thenReturn(
                    DotGraphQLContext.createServletContext().with(anonymousUser).build());
            Mockito.when(environment.getSource()).thenReturn(restrictedContentlet);
            Mockito.when(environment.getField()).thenReturn(
                    new graphql.language.Field(restrictedRelField.variable()));
            Mockito.when(environment.getArgument("query")).thenReturn(null);
            Mockito.when(environment.getArgument("limit")).thenReturn(null);
            Mockito.when(environment.getArgument("offset")).thenReturn(null);
            Mockito.when(environment.getArgument("sort")).thenReturn(null);

            final Object result = fetcher.get(environment);

            assertTrue("Result should be a DataFetcherResult when permission is denied",
                    result instanceof DataFetcherResult);

            final DataFetcherResult<?> dataFetcherResult = (DataFetcherResult<?>) result;
            assertNotNull("Errors list should not be null", dataFetcherResult.getErrors());
            assertFalse("Errors list should not be empty", dataFetcherResult.getErrors().isEmpty());
            assertTrue("Error should be a PermissionDeniedGraphQLException",
                    dataFetcherResult.getErrors().get(0) instanceof PermissionDeniedGraphQLException);
        } finally {
            APILocator.getContentTypeAPI(systemUser).delete(restrictedType);
        }
    }
}
