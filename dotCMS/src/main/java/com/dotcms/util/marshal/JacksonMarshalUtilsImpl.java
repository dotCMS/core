package com.dotcms.util.marshal;

import com.dotcms.util.jackson.SimpleContentletSerializer;
import com.dotcms.util.jackson.SqlTimeStampSerializer;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

public class JacksonMarshalUtilsImpl implements MarshalUtils{

    private final Lazy<ObjectMapper> defaultMapper = Lazy.of(() -> {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule().addSerializer(java.sql.Time.class, new SqlTimeStampSerializer()));
        objectMapper.registerModule(new SimpleModule().addSerializer(Contentlet.class, new SimpleContentletSerializer()));
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return objectMapper;
    });



    @Override
    public String marshal(final Object object) {
        final String json = Try.of(()-> defaultMapper.get().writeValueAsString(object)).getOrElseThrow(DotRuntimeException::new);
        Logger.info(this, ()->" ::  "+json);
        return json;
    }

    @Override
    public void marshal(final Writer writer, final Object object) {
        Try.of(()->{
            defaultMapper.get().writeValue(writer, object);
            return Void.TYPE;
        }).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(final String string, final Class<? extends T> clazz) {
        return Try.of(()-> {
            System.out.println(string);
            System.out.println(clazz);
            return defaultMapper.get().readValue(string, clazz);
        }).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(final String s, final TypeReference<T> typeOfT) {
        return Try.of(()-> defaultMapper.get().readValue(s,typeOfT)).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(final Reader reader, final Class<? extends T> clazz) {
        return Try.of(()-> defaultMapper.get().readValue(reader,clazz)).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(final InputStream inputStream, final Class<T> clazz) {
        return Try.of(()-> defaultMapper.get().readValue(inputStream,clazz)).getOrElseThrow(DotRuntimeException::new);
    }
}
