package com.dotcms.rest.api.v1;

import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationFeature;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;

/**
 * Encapsulates the configuration for the Object Mapper on the Resources.
 * @author jsanca
 */
public class DotObjectMapperProvider {

    private final ObjectMapper defaultObjectMapper;

    /**
     * Get's the default object mapper.
     * @return ObjectMapper
     */
    public ObjectMapper getDefaultObjectMapper() {
        return defaultObjectMapper;
    }


    private DotObjectMapperProvider() {
        this(createDefaultMapper());
    }

    @VisibleForTesting
    protected DotObjectMapperProvider(final ObjectMapper defaultObjectMapper) {
        this.defaultObjectMapper = defaultObjectMapper;
    }

    public static ObjectMapper createDefaultMapper() {

        final ObjectMapper result = new ObjectMapper();
        result.disable(DeserializationFeature.WRAP_EXCEPTIONS);
        return result;
    }

    private static class SingletonHolder {
        private static final DotObjectMapperProvider INSTANCE = new DotObjectMapperProvider();
    }
    /**
     * Get the instance.
     * @return DotObjectMapperProvider
     */
    public static DotObjectMapperProvider getInstance() {

        return DotObjectMapperProvider.SingletonHolder.INSTANCE;
    } // getInstance.



} // E:O:F:DotObjectMapperProvider.


