package com.dotcms.graphql.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import graphql.schema.GraphQLSchema;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cache implementation used to store the generated {@link GraphQLSchema}
 */

public class GraphQLSchemaCache implements Cachable {
    private final static String GRAPHQL_SCHEMA_GROUP = GraphQLSchemaCache.class.getSimpleName().toLowerCase();

    private final static String[] GROUPS = {GRAPHQL_SCHEMA_GROUP};

    private final static String SCHEMA_KEY = "graphqlschema";

    private final static String ANON_SCHEMA_KEY = SCHEMA_KEY + "::anon";

    final private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return GRAPHQL_SCHEMA_GROUP;
    }

    public Optional<GraphQLSchema> getSchema() {
        try {
            return Optional.ofNullable((GraphQLSchema) cache.get(SCHEMA_KEY, GRAPHQL_SCHEMA_GROUP));
        } catch (DotCacheException e) {
            return Optional.empty();
        }
    }

    public Optional<GraphQLSchema> getSchema(final boolean anonymousUser) {
        if (anonymousUser) {
            try {
                return Optional.ofNullable(
                        (GraphQLSchema) cache.get(ANON_SCHEMA_KEY, GRAPHQL_SCHEMA_GROUP));
            } catch (DotCacheException e) {
                return Optional.empty();
            }
        } else {
            return getSchema();
        }
    }


    public void putSchema(final GraphQLSchema schema) {
        cache.put(SCHEMA_KEY, schema, GRAPHQL_SCHEMA_GROUP);
    }

    public void putSchema(final boolean anonymousUser, final GraphQLSchema schema) {
        if(anonymousUser){
            cache.put(ANON_SCHEMA_KEY, schema, GRAPHQL_SCHEMA_GROUP);
        } else {
            putSchema(schema);
        }
    }

    public void removeSchema() {
        cache.remove(SCHEMA_KEY, GRAPHQL_SCHEMA_GROUP);
        cache.remove(ANON_SCHEMA_KEY, GRAPHQL_SCHEMA_GROUP);
    }

    @Override
    public String[] getGroups() {
        return GROUPS;
    }

    @Override
    public void clearCache() {
        Arrays.asList(getGroups()).forEach(cache::flushGroup);
    }
}
