package com.dotcms.graphql.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.ContentResolver;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
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

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.BASE_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CATEGORIES;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENTLET_FOLER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENTLET_HOST;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENT_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.DELETED;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.EXPIRE_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.IDENTIFIER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.INODE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LANGUAGE_ID;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LIVE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LOCKED;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PARENT_PATH;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PATH;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PUBLISH_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.TITLE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.URL_MAP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_ASSIGN;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_CREATED_BY;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_STEP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKING;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILE_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SITE_OR_FOLDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_TITLE_FIELD_VAR;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphqlAPIImpl implements GraphqlAPI {

    @Override
    public GraphQLSchema getSchema() {
//        return generateSchema();
        return null;
    }

    @Override
    public GraphQLType createSchemaType(ContentType contentType) {
        return  null;
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

        return null;
    }

    // TODO: only widgetTitle returned here
//    private static Map<String, GraphQLOutputType> widgetInterfaceFields = new HashMap<>();
    private static Map<String, GraphQLOutputType> fileAssetInterfaceFields = new HashMap<>();

    private static Map<String, GraphQLObjectType> customFieldTypes = new HashMap<>();

    static {

        fileAssetInterfaceFields.put(FILEASSET_SITE_OR_FOLDER_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_FILEASSET_FIELD_VAR, CustomFieldType.BINARY.getType());
        fileAssetInterfaceFields.put(FILEASSET_TITLE_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_FILE_NAME_FIELD_VAR, GraphQLString);
        fileAssetInterfaceFields.put(FILEASSET_DESCRIPTION_FIELD_VAR, GraphQLString);




    }

//    private static Map<String, Graph>


    private GraphQLInterfaceType createContentInterface() {

        final Map<String, GraphQLOutputType> contentInterfaceFields = new HashMap<>();



        return TypeUtil.createInterfaceType("Content", contentInterfaceFields, new ContentResolver());
    }

}
