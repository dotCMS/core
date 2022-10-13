package com.dotcms.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import javax.inject.Singleton;

@Singleton
public class RegisterCustomModuleCustomizer implements ObjectMapperCustomizer {

    /**
     * according to:
     * <a href="https://stackoverflow.com/questions/61984336/how-to-configure-objectmapper-for-quarkus-rest-client">...</a>
     * this is how we customize the object mapper here we need to register GuavaModule as we use
     * ImmutableList in the generated code so jackson needs to know how to Serialize/Deserialize it
     * Other proposals can be found here <a href="https://lankydan.dev/providing-your-own-jackson-objectmapper-in-quarkus">...</a>
     */
    public void customize(ObjectMapper objectMapper) {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new VersioningModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
