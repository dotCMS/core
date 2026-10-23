package com.dotcms.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.filter.ContentFieldFilter;
import com.dotcms.graphql.business.PageAPIGraphQLTypesProvider;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the GraphQL instrumentation that keeps user properties out of results returned to
 * callers who are neither CMS administrators nor back-end users.
 *
 * @author hassandotcms
 */
class UserPropertiesInstrumentationTest {

    private static final List<String> SETTINGS = List.of(
            ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY,
            ContentFieldFilter.FIELDS_TO_MASK_PROPERTY,
            ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY);

    private static final String CUSTOMER_TYPE = "Blog";
    private static final String ROOT_TYPE = "Query";

    private final Map<String, String> originalSettings = new LinkedHashMap<>();
    /** No content type is stored in a unit test, so every field type is unknown. */
    private final UserPropertiesInstrumentation instrumentation =
            new UserPropertiesInstrumentation(() -> (contentType, fieldVariable) -> null);

    /** Turns the user properties setting on with empty lists, remembering the previous configuration. */
    @BeforeEach
    void configure() {
        SETTINGS.forEach(name -> originalSettings.put(name, Config.getStringProperty(name, null)));
        Config.setProperty(ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY, "true");
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, null);
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, null);
    }

    /** Puts back the configuration changed by a test. */
    @AfterEach
    void restoreSettings() {
        originalSettings.forEach(Config::setProperty);
        originalSettings.clear();
    }

    /**
     * Given an anonymous caller and a _map result with modUser and ownerUserName, when it is resolved, then
     * those keys are removed.
     */
    @Test
    @SuppressWarnings("unchecked")
    void filtersContentMapForNonBackendCallers() throws Exception {
        final Object result = resolve(anonymous(), CUSTOMER_TYPE, "_map", Map.of(), contentMap());

        final Map<String, Object> map = (Map<String, Object>) result;
        assertFalse(map.containsKey("modUser"));
        assertFalse(map.containsKey("ownerUserName"));
        assertEquals("Hello", map.get("title"));
        assertEquals("abc-1", map.get("identifier"));
    }

    /**
     * Given an anonymous caller, when DotPage lockedBy and lockedByName are resolved, then they are null.
     */
    @Test
    void hidesPageLockOwnerFromNonBackendCallers() throws Exception {
        assertNull(resolve(anonymous(), PageAPIGraphQLTypesProvider.DOT_PAGE, "lockedBy", Map.of(), "dotcms.org.1"));
        assertNull(resolve(anonymous(), PageAPIGraphQLTypesProvider.DOT_PAGE, "lockedByName", Map.of(), "Admin User"));
    }

    /**
     * Given a back-end caller, when the same fields are resolved, then they are unchanged.
     */
    @Test
    void leavesFieldsUnchangedForBackendUsers() throws Exception {
        assertEquals(contentMap(), resolve(backendUser(), CUSTOMER_TYPE, "_map", Map.of(), contentMap()));
        assertEquals("dotcms.org.1",
                resolve(backendUser(), PageAPIGraphQLTypesProvider.DOT_PAGE, "lockedBy", Map.of(), "dotcms.org.1"));
        assertEquals("Admin User",
                resolve(backendUser(), PageAPIGraphQLTypesProvider.DOT_PAGE, "lockedByName", Map.of(), "Admin User"));
    }

    /**
     * Given an anonymous caller and a customer content type field named lockedBy, when it is resolved, then
     * it is unchanged.
     */
    @Test
    void leavesCustomerFieldsWithTheSameNameUnchanged() throws Exception {
        assertEquals("Reviewer A", resolve(anonymous(), CUSTOMER_TYPE, "lockedBy", Map.of(), "Reviewer A"));
    }

    /**
     * Given an anonymous caller, when a search query names a user property, then the field returns an
     * error and no data.
     */
    @Test
    void refusesSearchesOnUserProperties() throws Exception {
        assertRefused("search", Map.of("query", "+owner:x*"));
        assertRefused("BlogCollection", Map.of("query", "+modUser:x*"));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Resolves a search field and checks that the original fetcher is not called and that the outcome is a
     * field error: an exception, or a result with errors and no data.
     */
    private void assertRefused(final String fieldName, final Map<String, Object> arguments) {
        final AtomicBoolean searched = new AtomicBoolean(false);
        final DataFetcher<Object> search = environment -> {
            searched.set(true);
            return List.of(contentMap());
        };
        final DataFetchingEnvironment environment = environment(anonymous(), ROOT_TYPE, fieldName, arguments);

        Object outcome;
        try {
            outcome = instrument(search, environment).get(environment);
        } catch (final Exception e) {
            outcome = e;
        }

        assertFalse(searched.get(), fieldName + ": a refused search must not run");
        if (outcome instanceof DataFetcherResult) {
            final DataFetcherResult<?> result = (DataFetcherResult<?>) outcome;
            assertTrue(result.hasErrors(), fieldName + ": the refusal must be reported as a field error");
            assertNull(result.getData(), fieldName + ": a refused search returns no data");
        } else {
            assertTrue(outcome instanceof Exception, fieldName + ": the refusal must be a field error, got " + outcome);
        }
    }

    /** Resolves one field for the given caller through the instrumented fetcher and returns its data. */
    private Object resolve(final User caller, final String parentType, final String fieldName,
                           final Map<String, Object> arguments, final Object fetchedValue) throws Exception {
        final DataFetchingEnvironment environment = environment(caller, parentType, fieldName, arguments);
        final Object result = instrument(ignored -> fetchedValue, environment).get(environment);
        return result instanceof DataFetcherResult ? ((DataFetcherResult<?>) result).getData() : result;
    }

    /** Wraps a fetcher with the instrumentation under test. */
    @SuppressWarnings("unchecked")
    private DataFetcher<Object> instrument(final DataFetcher<Object> fetcher, final DataFetchingEnvironment environment) {
        final InstrumentationFieldFetchParameters parameters = new InstrumentationFieldFetchParameters(
                mock(ExecutionContext.class), environment, null, false);
        return (DataFetcher<Object>) instrumentation.instrumentDataFetcher(fetcher, parameters);
    }

    /** Builds the environment of one field of the given parent type, resolved for the given caller. */
    private static DataFetchingEnvironment environment(final User caller, final String parentType,
                                                       final String fieldName, final Map<String, Object> arguments) {
        final GraphQLFieldDefinition field = GraphQLFieldDefinition.newFieldDefinition()
                .name(fieldName).type(Scalars.GraphQLString).build();
        final GraphQLObjectType parent = GraphQLObjectType.newObject().name(parentType).field(field).build();
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .context(DotGraphQLContext.createServletContext().with(caller).build())
                .source(new Object())
                .arguments(arguments)
                .fieldDefinition(field)
                .parentType(parent)
                .build();
    }

    /** Returns the anonymous user, without any role lookup. */
    private static User anonymous() {
        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);
        return anonymous;
    }

    /** Returns a back-end user, without any role lookup. */
    private static User backendUser() {
        final User backend = mock(User.class);
        when(backend.isBackendUser()).thenReturn(true);
        return backend;
    }

    /** Returns a content map with modUser and ownerUserName. */
    private static Map<String, Object> contentMap() {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put("identifier", "abc-1");
        content.put("inode", "inode-1");
        content.put("contentType", CUSTOMER_TYPE);
        content.put("title", "Hello");
        content.put("modUser", "dotcms.org.1");
        content.put("ownerUserName", "Admin User");
        return content;
    }
}
