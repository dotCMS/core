package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcher;
import com.dotcms.graphql.exception.FieldGenerationException;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;

/**
 * This implementation generates a {@link GraphQLFieldDefinition} only for {@link Field}s
 * of type {@link com.dotcms.contenttype.model.field.RelationshipField}
 */
class RelationshipFieldGenerator implements GraphQLFieldGenerator {

    private RelationshipAPI relationshipAPI;

    @VisibleForTesting
    public RelationshipFieldGenerator(RelationshipAPI relationshipAPI) {
        this.relationshipAPI = relationshipAPI;
    }

    public RelationshipFieldGenerator() {
        this(APILocator.getRelationshipAPI());
    }

    @Override
    public GraphQLFieldDefinition generateField(final Field field) {
        return createRelationshipField(field);
    }

    private GraphQLFieldDefinition createRelationshipField(
            final Field field) {

        final ContentType relatedContentType;
        ContentType contentType = null;

        Logger.debug(this, ()->
                "Creating relationship field for field: " + field.variable() + " of content type: "
                        + field.contentTypeId());
        try {
            contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(field.contentTypeId());
            relatedContentType = getRelatedContentTypeForField(field);
        } catch (DotSecurityException | DotDataException e) {
            throw new FieldGenerationException(
                    "Unable to create relationship field type for field: " + (contentType!=null ? contentType.variable() : "N/D")
                            + "." + field.variable(), e);
        }

        Relationship relationship;

        try {

            Logger.debug(this, ()-> "Getting relationship for field: " + field.variable() + " of content type: "
                    + field.contentTypeId() + " and related content type: " + relatedContentType.variable());
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

        GraphQLOutputType outputType = GraphQLTypeReference.typeRef(relatedContentType.variable());

        outputType = records.doesAllowOnlyOne()
                ? outputType
                : list(outputType);

        return newFieldDefinition()
                .name(field.variable())
                .argument(GraphQLArgument.newArgument()
                        .name("query")
                        .type(GraphQLString)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("limit")
                        .type(GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("offset")
                        .type(GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("sortBy")
                        .type(GraphQLString)
                        .build())
                .type(field.required() ? nonNull(outputType) : outputType)
                .dataFetcher(new RelationshipFieldDataFetcher()).build();
    }

    private ContentType getRelatedContentTypeForField(final Field field) {
        DotPreconditions.checkNotNull(field, "Field can't be null");
        DotPreconditions.checkArgument(UtilMethods.isSet(field.variable()), "Field variable needs to be set");
        DotPreconditions.checkArgument(UtilMethods.isSet(field.relationType()), "Field relationType needs to be set");

        final Relationship relationship;
        try {
            relationship = relationshipAPI.getRelationshipFromField(field,
                    APILocator.systemUser());
        } catch (DotSecurityException | DotDataException e) {
            throw new FieldGenerationException("Relationship with name:"
                    + field.relationType() + " not found. Field var:" + field.variable()
                    + ". Field ID: " + field.id());
        }

        if (relationship == null) {
            throw new FieldGenerationException("Relationship with name:"
                    + field.relationType() + " not found. Field var:" + field.variable()
                    + ". Field ID: " + field.id());
        }

        final String relatedContentTypeId =
                relationship.getParentStructureInode().equals(field.contentTypeId())
                        ? relationship.getChildStructureInode()
                        : relationship.getParentStructureInode();

        ContentType type;

        try {
            type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(relatedContentTypeId);
        } catch (DotSecurityException | DotDataException e) {
            throw new FieldGenerationException("Unable to find content type with id:" + relatedContentTypeId, e);
        }

        return type;
    }
}
