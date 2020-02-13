package com.dotcms.graphql.util;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.InterfaceType;
import com.dotmarketing.business.APILocator;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLType;
import java.util.Set;

public class GraphQLUtil {

    public static boolean isVariableGraphQLCompatible(final Field field) {
        // first let's check if there's an inherited field with the same variable
        if(InterfaceType.getContentletInheritedFields().containsKey(field.name())) {
            // now let's check if the graphql types are compatible

            // get inherited field's graphql type
            GraphQLType inheritedFieldGraphQLType = InterfaceType.getContentletInheritedFields()
                    .get(field.name()).getType();

            // get new field's type
            GraphQLType fieldGraphQLType = APILocator.getGraphqlAPI()
                    .getGraphqlTypeForFieldClass(field.type(), field);

            // if at least one of them is a custom type, they need to be equal to be compatible
            return (!isCustomFieldType(inheritedFieldGraphQLType)
                    && !isCustomFieldType(fieldGraphQLType))
                    || inheritedFieldGraphQLType.equals(fieldGraphQLType);
        }

        return true;
    }

    private static boolean isCustomFieldType(final GraphQLType type) {
           return  type instanceof GraphQLList ? APILocator.getGraphqlAPI().getCustomFieldTypes()
                   .contains(((GraphQLList) type).getWrappedType())
                   : APILocator.getGraphqlAPI().getCustomFieldTypes().contains(type);
    }
}
