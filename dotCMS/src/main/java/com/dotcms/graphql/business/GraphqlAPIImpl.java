package com.dotcms.graphql.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphqlAPIImpl implements GraphqlAPI {

    private Map<Class<? extends Field>, GraphQLObjectType> fieldClassGraphqlTypeMap = new HashMap<>();

    public GraphqlAPIImpl() {
        fieldClassGraphqlTypeMap.put(BinaryField.class, CustomFieldType.BINARY.getType());
        fieldClassGraphqlTypeMap.put(CategoryField.class, CustomFieldType.CATEGORY.getType());
    }

    @Override
    public GraphQLSchema getSchema() {
//        return generateSchema();
        return null;
    }

    @Override
    public GraphQLType createSchemaType(ContentType contentType) {

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(contentType.variable());

        // add CONTENT interface fields
        builder.fields(InterfaceType.CONTENT.getType().getFieldDefinitions());

        final List<Field> fields = contentType.fields();

        fields.forEach((field)->{
            builder.field(newFieldDefinition()
                .name(field.variable())
                .type(getGraphqlTypeForFieldClass(field.type()))
                .dataFetcher(new FieldDataFetcher())
            );
        });

        return builder.build();
    }

    private GraphQLOutputType getGraphqlTypeForFieldClass(final Class<Field> fieldClass) {

        DotPreconditions.checkArgument(!fieldClass.getName().equals(RelationshipField.class.getName()),
            "Unable to solve RelationshiFields at this point");

        return fieldClassGraphqlTypeMap.get(fieldClass)!= null
            ? fieldClassGraphqlTypeMap.get(fieldClass)
            : GraphQLString;
    }

    @Override
    public void updateSchemaType(ContentType contentType) {

    }

    @Override
    public void deleteSchemaType(String contentTypeVar) {

    }

    @Override
    public void createSchemaTypeField(ContentType contentType, Field field) {

    }

    @Override
    public void updateSchemaTypeField(ContentType contentType, Field field) {

    }

    @Override
    public void deleteSchemaTypeField(ContentType contentType, String fieldVar) {

    }

    private GraphQLSchema generateSchema() throws DotDataException {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        Set<GraphQLType> graphQLTypes = new HashSet<>(InterfaceType.valuesAsSet());

        List<ContentType> allTypes = contentTypeAPI.findAll();

        // create all types without relationship fields
        allTypes.forEach((type)->graphQLTypes.add(createSchemaType(type)));

        // add relationship fields after creating all types



        // Root Type
        GraphQLObjectType rootType = newObject()
            .name("Query")
            .field(newFieldDefinition()
                .name("search")
                .argument(GraphQLArgument.newArgument()
                    .name("query")
                    .type(GraphQLString)
                    .build())
                .type(GraphQLList.list(InterfaceType.CONTENT.getType()))
                .dataFetcher(new ContentletDataFetcher()))
            .build();

        return new GraphQLSchema(rootType, null, graphQLTypes);
    }

}
