package com.dotmarketing.db;

import java.util.Properties;

/**
 * Simple no-cache implementation for Hibernate 5.6
 * Since caching is disabled in the configuration, this class can be empty
 */
public class NoCacheProvider {
    // This class is no longer needed with modern Hibernate 5.6
    // Cache configuration is handled through standard properties
    // hibernate.cache.use_second_level_cache=false
    // hibernate.cache.use_query_cache=false

}
