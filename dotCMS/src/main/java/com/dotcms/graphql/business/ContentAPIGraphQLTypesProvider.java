package com.dotcms.graphql.business;

import static com.dotcms.graphql.CustomFieldType.isCustomFieldType;
import static com.dotcms.graphql.business.GraphqlAPI.TYPES_AND_FIELDS_VALID_NAME_REGEX;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.BinaryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.CategoryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.FileFieldDataFetcher;
import com.dotcms.graphql.datafetcher.KeyValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.MultiValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcher;
import com.dotcms.graphql.datafetcher.SiteOrFolderFieldDataFetcher;
import com.dotcms.graphql.datafetcher.TagsFieldDataFetcher;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum ContentAPIGraphQLTypesProvider implements GraphQLTypesProvider {

    INSTANCE;

    private RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

    private final Map<Class<? extends Field>, GraphQLOutputType> fieldClassGraphqlTypeMap = new HashMap<>();

    private final Map<Class<? extends Field>, DataFetcher> fieldClassGraphqlDataFetcher = new HashMap<>();

    private final Map<String, GraphQLType> typesMap = new HashMap<>();

    {
        // custom type mappings
        this.fieldClassGraphqlTypeMap.put(BinaryField.class, CustomFieldType.BINARY.getType());
        this.fieldClassGraphqlTypeMap
                .put(CategoryField.class, list(CustomFieldType.CATEGORY.getType()));
        this.fieldClassGraphqlTypeMap.put(ImageField.class, CustomFieldType.FILEASSET.getType());
        this.fieldClassGraphqlTypeMap.put(FileField.class, CustomFieldType.FILEASSET.getType());
        this.fieldClassGraphqlTypeMap
                .put(KeyValueField.class, list(CustomFieldType.KEY_VALUE.getType()));
        this.fieldClassGraphqlTypeMap.put(CheckboxField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap.put(MultiSelectField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap.put(TagField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap
                .put(HostFolderField.class, CustomFieldType.SITE_OR_FOLDER.getType());

        // custom data fetchers
        this.fieldClassGraphqlDataFetcher.put(BinaryField.class, new BinaryFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(CategoryField.class, new CategoryFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(ImageField.class, new FileFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(FileField.class, new FileFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(KeyValueField.class, new KeyValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher
                .put(CheckboxField.class, new MultiValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher
                .put(MultiSelectField.class, new MultiValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(TagField.class, new TagsFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher
                .put(HostFolderField.class, new SiteOrFolderFieldDataFetcher());
    }

    @VisibleForTesting
    protected void setRelationshipAPI(final RelationshipAPI relationshipAPI) {
        this.relationshipAPI = relationshipAPI;
    }

    @Override
    public Collection<GraphQLType> getTypes() throws DotDataException {
        // we want to generate them always - no cache
        getContentAPITypes()
                .forEach((graphQLType)->typesMap.put(graphQLType.getName(), graphQLType));
        return typesMap.values();
    }

    Map<String, GraphQLType> getCachedTypesAsMap() throws DotDataException {
        if (!UtilMethods.isSet(typesMap)) {
            getContentAPITypes()
                    .forEach((graphQLType)->typesMap.put(graphQLType.getName(), graphQLType));
        }
        return typesMap;
    }


    private Set<GraphQLType> getContentAPITypes() throws DotDataException {

        Set<GraphQLType> contentAPITypes = new HashSet<>(InterfaceType.valuesAsSet());

        contentAPITypes.add(ExtendedScalars.DateTime);

        List<ContentType> allTypes = APILocator.getContentTypeAPI(APILocator.systemUser())
                .findAllRespectingLicense();
        // create all types
        Map<String, GraphQLObjectType> concreteTypes = new HashMap<>();

        allTypes.forEach((type) -> createContentAPISchemaType(type, concreteTypes));

        // add here the rest of types
        contentAPITypes.addAll(concreteTypes.values());

        return contentAPITypes;
    }

    private void createContentAPISchemaType(ContentType contentType,
            final Map<String, GraphQLObjectType> graphqlObjectTypes) {

        // skip contentType.variable not sticking to the regex
        if (!contentType.variable().matches(TYPES_AND_FIELDS_VALID_NAME_REGEX)) {
            return;
        }

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(contentType.variable());

        // add CONTENT interface fields
        builder.fields(InterfaceType.CONTENTLET.getType().getFieldDefinitions());

        if (InterfaceType.getInterfaceForBaseType(contentType.baseType()) != null) {
            builder.withInterface(InterfaceType.getInterfaceForBaseType(contentType.baseType()));
        }

        final List<Field> fields = contentType.fields();

        fields.forEach((field) -> {
            // skip field.variable not sticking to the regex
            if (!field.variable().matches(TYPES_AND_FIELDS_VALID_NAME_REGEX)
                    || field instanceof RelationshipsTabField) {
                return;
            }

            if (!(field instanceof RowField) && !(field instanceof ColumnField)) {
                if (field instanceof RelationshipField) {
                    try {
                        handleRelationshipField(contentType, builder, field, graphqlObjectTypes);
                    } catch (DotStateException e) {
                        Logger.error(this, "Unable to create relationship field", e);
                    }
                } else {
                    builder.field(newFieldDefinition()
                            .name(field.variable())
                            .type(field.required()
                                    ? nonNull(getGraphqlTypeForFieldClass(field.type(), field))
                                    : getGraphqlTypeForFieldClass(field.type(), field))
                            .dataFetcher(getGraphqlDataFetcherForFieldClass(field.type()))
                    );
                }
            }
        });

        builder.withInterface(InterfaceType.CONTENTLET.getType());
        final GraphQLObjectType graphQLType = builder.build();

        graphqlObjectTypes.put(graphQLType.getName(), graphQLType);
    }

    private void handleRelationshipField(final ContentType contentType,
            GraphQLObjectType.Builder builder,
            final Field field, final Map<String, GraphQLObjectType> typesMap) {

        final ContentType relatedContentType;
        try {
            relatedContentType = getRelatedContentTypeForField(field, APILocator.systemUser());
        } catch (DotSecurityException | DotDataException e) {
            throw new DotStateException(
                    "Unable to create relationship field type for field: " + contentType.variable()
                            + "." + field.variable(), e);
        }

        Relationship relationship;

        try {
            relationship = relationshipAPI.getRelationshipFromField(field,
                    APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }

        final ContentletRelationships contentletRelationships = new ContentletRelationships(null);
        final ContentletRelationships.ContentletRelationshipRecords
                records = contentletRelationships.new ContentletRelationshipRecords(
                relationship,
                relationshipAPI.isChildField(relationship, field));

        GraphQLOutputType outputType = typesMap.get(relatedContentType.variable()) != null
                ? typesMap.get(relatedContentType.variable())
                : GraphQLTypeReference.typeRef(relatedContentType.variable());

        outputType = records.doesAllowOnlyOne()
                ? outputType
                : list(outputType);

        builder.field(newFieldDefinition()
                .name(field.variable())
                .type(field.required() ? nonNull(outputType) : outputType)
                .dataFetcher(new RelationshipFieldDataFetcher())
        );
    }

    public GraphQLOutputType getGraphqlTypeForFieldClass(final Class<? extends Field> fieldClass,
            final Field field) {
        return fieldClassGraphqlTypeMap.get(fieldClass) != null
                ? fieldClassGraphqlTypeMap.get(fieldClass)
                : fieldClass.equals(TextField.class) && field.dataType().equals(DataTypes.INTEGER)
                        ? GraphQLInt
                        : fieldClass.equals(TextField.class) && field.dataType()
                                .equals(DataTypes.FLOAT) ? GraphQLFloat
                                : GraphQLString;
    }

    private DataFetcher getGraphqlDataFetcherForFieldClass(final Class<Field> fieldClass) {
        return fieldClassGraphqlDataFetcher.get(fieldClass) != null
                ? fieldClassGraphqlDataFetcher.get(fieldClass)
                : new FieldDataFetcher();
    }

    private ContentType getRelatedContentTypeForField(final Field field, final User user)
            throws DotSecurityException, DotDataException {
        final Relationship relationship = relationshipAPI.getRelationshipFromField(field,
                user);

        if (relationship == null) {
            throw new DotDataException("Relationship with name:"
                    + field.relationType() + " not found. Field var:" + field.variable()
                    + ". Field ID: " + field.id());
        }

        final String relatedContentTypeId =
                relationship.getParentStructureInode().equals(field.contentTypeId())
                        ? relationship.getChildStructureInode()
                        : relationship.getParentStructureInode();

        return APILocator.getContentTypeAPI(user).find(relatedContentTypeId);
    }

    public boolean isVariableGraphQLCompatible(final Field field) {
        // first let's check if there's an inherited field with the same variable
        if (InterfaceType.getContentletInheritedFields().containsKey(field.name())) {
            // now let's check if the graphql types are compatible

            // get inherited field's graphql type
            final GraphQLType inheritedFieldGraphQLType = InterfaceType
                    .getContentletInheritedFields()
                    .get(field.name()).getType();

            // get new field's type
            final GraphQLType fieldGraphQLType = getGraphqlTypeForFieldClass(field.type(), field);

            // if at least one of them is a custom type, they need to be equal to be compatible
            return (!isCustomFieldType(inheritedFieldGraphQLType)
                    && !isCustomFieldType(fieldGraphQLType))
                    || inheritedFieldGraphQLType.equals(fieldGraphQLType);
        }

        return true;
    }
}
