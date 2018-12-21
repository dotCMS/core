package com.dotcms.graphql.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.ContentResolver;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.*;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILE_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_METADATA_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SITE_OR_FOLDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_CODE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_PRE_EXECUTE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_USAGE_FIELD_VAR;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphqlAPIImpl implements GraphqlAPI {

    @Override
    public GraphQLSchema getSchema() {
        return generateSchema();
    }

    @Override
    public GraphQLType createSchemaType(ContentType contentType) {

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

        Set<GraphQLType> graphQLTypes = new HashSet<>(createBaseTypes());

        List<ContentType> allTypes = contentTypeAPI.findAll();
        allTypes.forEach((type)->graphQLTypes.add(createSchemaType(type)));

        final GraphQLInterfaceType contentInterface = createContentInterface();

        // Root Type
        GraphQLObjectType rootType = newObject()
            .name("Query")
            .field(newFieldDefinition()
                .name("search")
                .argument(GraphQLArgument.newArgument()
                    .name("query")
                    .type(GraphQLString)
                    .build())
                .type(GraphQLList.list(contentInterface))
                .dataFetcher(new ContentletDataFetcher()))
            .build();

        return new GraphQLSchema(rootType, null, graphQLTypes);
    }

    private Set<GraphQLType> createBaseTypes() {


    }

    private static Map<String, GraphQLOutputType> contentInterfaceFields = new HashMap<>();
    // TODO: only widgetTitle returned here
//    private static Map<String, GraphQLOutputType> widgetInterfaceFields = new HashMap<>();
    private static Map<String, GraphQLOutputType> fileAssetInterfaceFields = new HashMap<>();

    private static Map<String, GraphQLOutputType> binaryTypeFields = new HashMap<>();

    static {


        contentInterfaceFields.put(MOD_DATE, GraphQLString);
        contentInterfaceFields.put(TITLE, GraphQLString);
        contentInterfaceFields.put(CONTENT_TYPE, GraphQLString);
        contentInterfaceFields.put(BASE_TYPE, GraphQLInt);
        contentInterfaceFields.put(LIVE, GraphQLBoolean);
        contentInterfaceFields.put(WORKING, GraphQLBoolean);
        contentInterfaceFields.put(DELETED, GraphQLBoolean);
        contentInterfaceFields.put(LOCKED, GraphQLBoolean);
        contentInterfaceFields.put(LANGUAGE_ID, GraphQLBoolean);
        contentInterfaceFields.put(IDENTIFIER, GraphQLID);
        contentInterfaceFields.put(INODE, GraphQLID);
        contentInterfaceFields.put(CONTENTLET_HOST, GraphQLID);
        contentInterfaceFields.put(CONTENTLET_FOLER, GraphQLID);
        contentInterfaceFields.put(PARENT_PATH, GraphQLString);
        contentInterfaceFields.put(PATH, GraphQLString);
        contentInterfaceFields.put(WORKFLOW_CREATED_BY, GraphQLString);
        contentInterfaceFields.put(WORKFLOW_ASSIGN, GraphQLString);
        contentInterfaceFields.put(WORKFLOW_STEP, GraphQLString);
        contentInterfaceFields.put(WORKFLOW_MOD_DATE, GraphQLString);
        contentInterfaceFields.put(PUBLISH_DATE, GraphQLString);
        contentInterfaceFields.put(EXPIRE_DATE, GraphQLString);
        contentInterfaceFields.put(URL_MAP, GraphQLString);
        contentInterfaceFields.put(CATEGORIES, GraphQLString);

        fileAssetInterfaceFields.put(FILEASSET_SITE_OR_FOLDER_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_TITLE_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_FILE_NAME_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_METADATA_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_DESCRIPTION_FIELD_VAR, GraphQLString);

        binaryTypeFields.put()


    }


    private GraphQLInterfaceType createContentInterface() {

        final GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface().name("Content");

        contentInterfaceFields.keySet().forEach((key)->{
            builder.field(newFieldDefinition()
                .name(key)
                .type(contentInterfaceFields.get(key))
                .dataFetcher(new FieldDataFetcher())
            );
        });

        builder.typeResolver(new ContentResolver());
        return builder.build();
    }

}
