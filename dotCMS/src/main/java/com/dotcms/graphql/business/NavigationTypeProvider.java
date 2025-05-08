package com.dotcms.graphql.business;

import static com.dotcms.graphql.business.NavigationFieldProvider.DOT_NAVIGATION;
import static com.dotcms.graphql.util.TypeUtil.createObjectType;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import com.dotmarketing.util.Logger;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for the Navigation GraphQL type.
 */
public enum NavigationTypeProvider implements GraphQLTypesProvider {

    INSTANCE;

    /**
     * Creates the fields for the Navigation GraphQL type.
     * The 'children' field is defined as a recursive reference to the Navigation type itself.
     * @return a Map of field names to GraphQL output types
     */
    private Map<String, GraphQLOutputType> createNavigationFields() {
        final Map<String, GraphQLOutputType> fields = new HashMap<>();
        // Using GraphQLTypeReference to create a recursive structure
        // This allows children to have the same structure as their parent
        fields.put("children", GraphQLList.list(new GraphQLTypeReference(DOT_NAVIGATION)));
        fields.put("code", GraphQLString);
        fields.put("folder", GraphQLString);
        fields.put("hash", GraphQLInt);
        fields.put("host", GraphQLString);
        fields.put("href", GraphQLString);
        fields.put("languageId", GraphQLInt);
        fields.put("order", GraphQLInt);
        fields.put("target", GraphQLString);
        fields.put("title", GraphQLString);
        fields.put("type", GraphQLString);
        return fields;
    }

    final Map<String, GraphQLOutputType> navigationFields = createNavigationFields();

    final GraphQLObjectType navigationType = createObjectType(DOT_NAVIGATION, navigationFields, null);

    /**
     * Returns the GraphQL type for Navigation.
     * @return a GraphQLObjectType representing Navigation
     */
    @Override
    public Collection<? extends GraphQLType> getTypes() {
        Logger.debug(this, ()->"Creating DotNavigationAPI types");
        return List.of(navigationType);
    }
}