package com.dotcms;

/*
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;

import javax.enterprise.inject.Instance;
import javax.inject.Singleton;
*/
public class ObjectMapperConfiguration {

   /* @Singleton
    ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers) {
        // Your own `ObjectMapper` or one provided by another library
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // Apply customizations (includes customizations from Quarkus)
        for (ObjectMapperCustomizer customizer : customizers) {
            customizer.customize(mapper);
        }
        return mapper;
    }

    */
}