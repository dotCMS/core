package com.dotcms.rest.api;

import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationFeature;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.Feature;
import com.dotcms.repackage.javax.ws.rs.ext.ContextResolver;
import com.dotcms.repackage.javax.ws.rs.ext.Provider;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;

@Provider
public class MyObjectMapperProvider implements ContextResolver<ObjectMapper> {

    final DotObjectMapperProvider objectMapperProvider;

    public MyObjectMapperProvider() {
        objectMapperProvider = DotObjectMapperProvider.getInstance();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapperProvider.getDefaultObjectMapper();
    }

}