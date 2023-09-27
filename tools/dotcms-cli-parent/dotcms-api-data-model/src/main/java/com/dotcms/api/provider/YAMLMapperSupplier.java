package com.dotcms.api.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;

import java.util.function.Supplier;

public class YAMLMapperSupplier implements Supplier<ObjectMapper> {
    @Override
    public ObjectMapper get() {
        return new ObjectMapper(new YAMLFactory()).
                enable(SerializationFeature.INDENT_OUTPUT).
                registerModule(new Jdk8Module()).
                registerModule(new GuavaModule()).
                registerModule(new JavaTimeModule()).
                registerModule(new VersioningModule()).
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).
                setSerializationInclusion(JsonInclude.Include.NON_DEFAULT).
                findAndRegisterModules();
    }
}
