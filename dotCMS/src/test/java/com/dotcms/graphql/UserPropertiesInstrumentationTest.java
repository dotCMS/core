package com.dotcms.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.filter.ContentFieldFilter;
import com.dotcms.graphql.business.PageAPIGraphQLTypesProvider;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import graphql.Scalars;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UserPropertiesInstrumentation}: the lock owner of a page follows the user properties
 * setting.
 *
 * @author hassandotcms
 */
class UserPropertiesInstrumentationTest {

    private static final List<String> SETTINGS = List.of(
            ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY,
            ContentFieldFilter.FIELDS_TO_MASK_PROPERTY,
            ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY);

    private final Map<String, String> originalSettings = new LinkedHashMap<>();

    /** Remembers the configuration a test changes. */
    @BeforeEach
    void rememberSettings() {
        SETTINGS.forEach(name -> originalSettings.put(name, Config.getStringProperty(name, null)));
    }

    /** Puts back the configuration changed by a test. */
    @AfterEach
    void restoreSettings() {
        originalSettings.forEach(Config::setProperty);
        originalSettings.clear();
    }

    /**
     * Given an anonymous caller and a locked page, when the user properties setting is on, then the lock owner
     * gets the placeholder; when it is off while another content property is listed to mask, then the lock owner
     * is sent as stored; when it is off and the list names the lock owner, then it gets the placeholder.
     */
    @Test
    void sendsPageLockOwnerAsTheUserPropertiesSettingSays() throws Exception {
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, "title");
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, null);

        Config.setProperty(ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY, "true");
        assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, fetchPageField("lockedBy", "dotcms.org.1"));
        assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, fetchPageField("lockedByName", "Admin User"));

        Config.setProperty(ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY, "false");
        assertEquals("dotcms.org.1", fetchPageField("lockedBy", "dotcms.org.1"));
        assertEquals("Admin User", fetchPageField("lockedByName", "Admin User"));

        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, "lockedBy");
        assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, fetchPageField("lockedBy", "dotcms.org.1"));
    }

    /** Fetches a field of a {@code DotPage} for an anonymous caller through the instrumentation. */
    private static Object fetchPageField(final String fieldName, final Object stored) throws Exception {
        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);
        final DotGraphQLContext context = mock(DotGraphQLContext.class);
        when(context.getUser()).thenReturn(anonymous);
        final GraphQLObjectType page = mock(GraphQLObjectType.class);
        when(page.getName()).thenReturn(PageAPIGraphQLTypesProvider.DOT_PAGE);
        final DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getFieldDefinition()).thenReturn(GraphQLFieldDefinition.newFieldDefinition()
                .name(fieldName).type(Scalars.GraphQLString).build());
        when(environment.getParentType()).thenReturn(page);
        when(environment.getContext()).thenReturn(context);
        final InstrumentationFieldFetchParameters parameters = mock(InstrumentationFieldFetchParameters.class);
        when(parameters.getEnvironment()).thenReturn(environment);

        final DataFetcher<?> original = fetched -> stored;
        return new UserPropertiesInstrumentation().instrumentDataFetcher(original, parameters).get(environment);
    }
}
