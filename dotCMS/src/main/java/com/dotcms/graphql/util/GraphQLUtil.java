package com.dotcms.graphql.util;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.InterfaceType;
import com.dotmarketing.business.APILocator;
import graphql.schema.GraphQLType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class GraphQLUtil {

    public static Set<String> getFieldReservedWords() {
        return InterfaceType.RESERVED_GRAPHQL_FIELD_NAMES;
    }

    public static boolean isVariableGraphQLCompatible(final Field field) {
        // first let's check if there's an inherited field with the same variable
        if(InterfaceType.getContentletInheritedFields().containsKey(field.name())) {
            // now let's check if the graphql types are compatible

            // get inherited graphql field type
            GraphQLType inheritedFieldGraphQLType = InterfaceType.getContentletInheritedFields()
                    .get(field.name()).getType();

            // get new field type
            GraphQLType fieldGraphQLType = APILocator.getGraphqlAPI()
                    .getGraphqlTypeForFieldClass(field.type(), field);

            return (!isCustomFieldType(inheritedFieldGraphQLType)
                    && !isCustomFieldType(fieldGraphQLType))
                    || inheritedFieldGraphQLType.equals(fieldGraphQLType);
        }

        return true;
    }

    private static boolean isCustomFieldType(final GraphQLType type) {
        return APILocator.getGraphqlAPI().getCustomFieldTypes().contains(type);
    }
}
