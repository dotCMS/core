package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;

import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.util.LogTime;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.SchemaPrinter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GraphqlAPIImpl implements GraphqlAPI {

    private final Set<GraphQLTypesProvider> typesProviders = new HashSet<>();
    private final Set<GraphQLFieldsProvider> fieldsProviders = new HashSet<>();

    private GraphQLSchemaCache schemaCache;

    @VisibleForTesting
    protected GraphqlAPIImpl(final GraphQLSchemaCache schemaCache) {
        typesProviders.add(ContentAPIGraphQLTypesProvider.INSTANCE);
        typesProviders.add(PageAPIGraphQLTypesProvider.INSTANCE);
        typesProviders.add(QueryMetadataTypeProvider.INSTANCE);
        fieldsProviders.add(ContentAPIGraphQLFieldsProvider.INSTANCE);
        fieldsProviders.add(PageAPIGraphQLFieldsProvider.INSTANCE);
        fieldsProviders.add(QueryMetadataFieldProvider.INSTANCE);
        this.schemaCache = schemaCache;
    }

    public GraphqlAPIImpl() {
        this(CacheLocator.getGraphQLSchemaCache());
    }

    /**
     * Returns the {@link GraphQLSchema}.
     * <p>
     * If the schema hasn't been generated it will generate it and put it into cache.
     * <p>
     * The actions that invalidate the cache are:
     * <ul>
     * <li>CRUD operations on {@link com.dotcms.contenttype.model.type.ContentType}s
     * <li>CRUD operations on {@link com.dotcms.contenttype.model.field.Field}s
     * </ul>
     * @return the GraphQL schema
     * @throws DotDataException in case of invalid data
     */
    @Override
    public GraphQLSchema getSchema() throws DotDataException {
        Optional<GraphQLSchema> schema = schemaCache.getSchema();

        if(!schema.isPresent()) {
            final GraphQLSchema generatedSchema = generateSchema();
            schemaCache.putSchema(generatedSchema);
            return generatedSchema;
        } else {
            return schema.get();
        }
    }

    /**
     * Nullifies the schema so it is regenerated next time it is fetched
     */
    @Override
    public void invalidateSchema() {
        schemaCache.removeSchema();
    }

    @Override
    public void printSchema() {
        if (Config.getBooleanProperty("GRAPHQL_PRINT_SCHEMA", false)) {
            SchemaPrinter printer = new SchemaPrinter();
            try {
                File graphqlDirectory = new File(ConfigUtils.getGraphqlPath());

                if(!graphqlDirectory.exists()) {
                    graphqlDirectory.mkdirs();
                }

                File schemaFile = new File(graphqlDirectory.getPath() + File.separator + "schema.graphqls");
                schemaFile.createNewFile();
                Files.write(schemaFile.toPath(), printer.print(getSchema()).getBytes());
            } catch (DotDataException | IOException e) {
                Logger.error(this, "Error printing schema", e);
            }
        }
    }

    @LogTime(loggingLevel = "INFO")
    @VisibleForTesting
    protected GraphQLSchema generateSchema() {
        final Set<GraphQLType> graphQLTypes = new HashSet<>();

        for (GraphQLTypesProvider typesProvider : typesProviders) {
            try {
                graphQLTypes.addAll(typesProvider.getTypes());
            } catch (DotDataException e) {
                Logger.error("Unable to get types for type provider:" + typesProvider
                        .getClass(), e);
            }
        }

        List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();

        for (GraphQLFieldsProvider fieldsProvider : fieldsProviders) {
            try {
                fieldDefinitions.addAll(fieldsProvider.getFields());
            } catch (DotDataException e) {
                Logger.error("Unable to get types for type provider:" + fieldsProvider.getClass(), e);
            }
        }

        // Root Type
        GraphQLObjectType.Builder rootTypeBuilder = createRootTypeBuilder().fields(fieldDefinitions);

        return new GraphQLSchema.Builder().query(rootTypeBuilder.build()).additionalTypes(graphQLTypes).build();
    }

    private Builder createRootTypeBuilder() {
        return newObject()
            .name("Query")
            .field(newFieldDefinition()
                .name("search")
                .argument(GraphQLArgument.newArgument()
                    .name("query")
                    .type(nonNull(GraphQLString))
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
                .type(list(InterfaceType.CONTENTLET.getType()))
                .dataFetcher(new ContentletDataFetcher()));
    }
}
