package com.dotcms.util.marshal;

import com.dotcms.util.jackson.SqlTimeStampSerializer;
import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

public class JacksonMarshalUtilsImpl implements MarshalUtils{

    private final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule().addSerializer(java.sql.Time.class, new SqlTimeStampSerializer()));
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        return objectMapper;
    });

    @Override
    public String marshal(Object object) {
        return Try.of(()->objectMapper.get().writeValueAsString(object)).getOrElseThrow(
                DotRuntimeException::new);
    }

    @Override
    public void marshal(Writer writer, Object object) {
        Try.of(()->{
            objectMapper.get().writeValue(writer, object);
            return Void.TYPE;
        }).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(String s, Class<? extends T> clazz) {
        return Try.of(()->objectMapper.get().readValue(s,clazz)).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(String s, Type typeOfT) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unmarshal(String s, TypeReference<T> typeOfT) {
        return Try.of(()->objectMapper.get().readValue(s,typeOfT)).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(Reader reader, Class<? extends T> clazz) {
        return Try.of(()->objectMapper.get().readValue(reader,clazz)).getOrElseThrow(DotRuntimeException::new);
    }

    @Override
    public <T> T unmarshal(InputStream inputStream, Class<T> clazz) {
        return Try.of(()->objectMapper.get().readValue(inputStream,clazz)).getOrElseThrow(DotRuntimeException::new);
    }
}
