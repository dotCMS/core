package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;

import com.dotcms.concurrent.Debouncer;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.util.LogTime;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.visibility.DefaultGraphqlFieldVisibility;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GraphqlAPIImpl implements GraphqlAPI {

    private final Set<GraphQLTypesProvider> typesProviders = new HashSet<>();
    private final Set<GraphQLFieldsProvider> fieldsProviders = new HashSet<>();

    private GraphQLSchemaCache schemaCache;

    @VisibleForTesting
    protected GraphqlAPIImpl(final GraphQLSchemaCache schemaCache) {
        typesProviders.add(ContentAPIGraphQLTypesProvider.INSTANCE);
        typesProviders.add(PageAPIGraphQLTypesProvider.INSTANCE);
        typesProviders.add(QueryMetadataTypeProvider.INSTANCE);
        typesProviders.add(PaginationTypeProvider.INSTANCE);
        fieldsProviders.add(ContentAPIGraphQLFieldsProvider.INSTANCE);
        fieldsProviders.add(PageAPIGraphQLFieldsProvider.INSTANCE);
        fieldsProviders.add(QueryMetadataFieldProvider.INSTANCE);
        fieldsProviders.add(PaginationFieldProvider.INSTANCE);
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

        if(schema.isPresent()) {
            return schema.get();
        }
        synchronized (this) {
            schema = schemaCache.getSchema();
            if(schema.isPresent()) {
                return schema.get();
            }
            final GraphQLSchema generatedSchema = generateSchema(APILocator.systemUser());
            schemaCache.putSchema(generatedSchema);
            return generatedSchema;
        }

    }

    public GraphQLSchema getSchema(final User user) throws DotDataException {

        Optional<GraphQLSchema> schema = schemaCache.getSchema(user.isAnonymousUser());
        if(schema.isPresent()) {
            return schema.get();
        }
        synchronized (this) {
            schema = schemaCache.getSchema(user.isAnonymousUser());
            if(schema.isPresent()) {
                return schema.get();
            }
            final GraphQLSchema generatedSchema = generateSchema(user);
            schemaCache.putSchema(user.isAnonymousUser(), generatedSchema);
            return generatedSchema;
        }
    }

    final Debouncer debouncer = new Debouncer();
    final Runnable removeSchema = ()->{schemaCache.removeSchema();};

    /**
     * Nullifies the schema so it is regenerated next time it is fetched
     * This method is debounced for 5 seconds to prevent overloading when
     * content types are saved.
     */
    @Override
    public void invalidateSchema() {
        final int delay = Config.getIntProperty("GRAPHQL_SCHEMA_DEBOUNCE_DELAY_MILLIS", 5000);
        
        if(delay<=0) {
            removeSchema.run();
            return;
        }

        debouncer.debounce("invalidateGraphSchema", removeSchema , delay, TimeUnit.MILLISECONDS);


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
        return generateSchema(APILocator.systemUser());
    }

    @LogTime(loggingLevel = "INFO")
    @VisibleForTesting
    protected GraphQLSchema generateSchema(final User user) {

        Logger.debug(this, ()-> "Generating GraphQL Schema for the user: " + user.getUserId());
        final Set<GraphQLType> graphQLTypes = new HashSet<>();

        Logger.debug(this, ()-> "Generating types by providers");
        for (GraphQLTypesProvider typesProvider : typesProviders) {
            try {
                graphQLTypes.addAll(typesProvider.getTypes());
            } catch (DotDataException e) {
                Logger.error("Unable to get types for type provider:" + typesProvider
                        .getClass(), e);
            }
        }

        // let's log if we are including dupe types
        final Map<String, GraphQLType> localTypesMap = new HashMap<>();
        graphQLTypes.forEach((type)-> {
            if(localTypesMap.containsKey(TypeUtil.getName(type))) {
                Logger.warn(this, "Dupe GraphQLType detected!: " + TypeUtil.getName(type));
                // removing dupes based on Config property
                if(Config.getBooleanProperty("GRAPHQL_REMOVE_DUPLICATED_TYPES", false)) {
                    return;
                }
            }
            localTypesMap.put(TypeUtil.getName(type), type);
        });

        final Set<GraphQLType> finalTypesSet = new HashSet<>(localTypesMap.values());


        final List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();

        Logger.debug(this, ()-> "Generating fields by providers");
        for (GraphQLFieldsProvider fieldsProvider : fieldsProviders) {
            try {

                Logger.debug(this, ()-> "Getting fields for provider: " + fieldsProvider.getClass());
                fieldDefinitions.addAll(fieldsProvider.getFields());
            } catch (DotDataException e) {
                Logger.error("Unable to get types for type provider:" + fieldsProvider.getClass(), e);
            }
        }

        // Root Type
        final GraphQLObjectType.Builder rootTypeBuilder = createRootTypeBuilder().fields(fieldDefinitions);

        final GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();

        if(user.isAnonymousUser()) {
            Logger.debug(this, ()-> "Setting NoIntrospectionGraphqlFieldVisibility, the user is anonymous");
            codeRegistryBuilder.fieldVisibility(
                    NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY).build();
        } else {
            Logger.debug(this, ()-> "Setting NoIntrospectionGraphqlFieldVisibility, the user is not anonymous");
            codeRegistryBuilder.fieldVisibility(
                    DefaultGraphqlFieldVisibility.DEFAULT_FIELD_VISIBILITY).build();
        }

        return new GraphQLSchema.Builder()
                .codeRegistry(codeRegistryBuilder.build())
                .query(rootTypeBuilder.build())
                .additionalTypes(finalTypesSet).build();
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
                    .name("page")
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
