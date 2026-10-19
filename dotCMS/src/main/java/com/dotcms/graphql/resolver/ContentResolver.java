package com.dotcms.graphql.resolver;

import com.dotcms.graphql.InterfaceType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import graphql.GraphQLException;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.TypeResolver;

public class ContentResolver implements TypeResolver {

    /**
     * Resolves a contentlet to the object type of its content type.
     *
     * <p>An asset content type whose own field clashes with a flat asset property is left out of
     * the asset interfaces (see {@code ContentAPIGraphQLTypesProvider}). When one of its assets is
     * resolved for a field typed with one of those interfaces -- an Image or File field -- its own
     * type would not be a possible type of that interface, so the property-clash type of its base
     * type answers instead. Everywhere else the content type's own object type is returned.
     * See #34540.
     *
     * @param env the resolution environment, carrying the contentlet and the field's type
     * @return the object type to resolve the contentlet as
     */
    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        final Contentlet contentlet = env.getObject();
        final GraphQLType type = env.getSchema().getType(contentlet.getContentType().variable());
        Logger.debug(this, ()-> "Resolving type for contentlet: " +
                contentlet.getIdentifier() + " type: " + contentlet.getContentType().variable());
        if(!UtilMethods.isSet(type)) {
            throw new GraphQLException("Type does not exist");
        }

        final GraphQLObjectType objectType = (GraphQLObjectType) type;
        final GraphQLType fieldType = null == env.getFieldType()
                ? null : GraphQLTypeUtil.unwrapAll(env.getFieldType());
        if (fieldType instanceof GraphQLInterfaceType
                && !env.getSchema().isPossibleType((GraphQLInterfaceType) fieldType, objectType)) {
            final GraphQLObjectType standIn = InterfaceType.getPropertyClashType(
                    contentlet.getContentType().baseType());
            if (null != standIn) {
                return env.getSchema().getObjectType(standIn.getName());
            }
        }
        return objectType;
    }
}
