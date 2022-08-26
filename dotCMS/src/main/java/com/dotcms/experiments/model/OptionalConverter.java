package com.dotcms.experiments.model;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import java.time.LocalDateTime;
import java.util.Optional;

public class OptionalConverter<T> implements Converter<Optional<T>, T> {

    @Override
    public T convert(Optional<T> value) {

        return value.orElse(null);
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return TypeFactory.defaultInstance().constructType(Optional.class);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return TypeFactory.defaultInstance().constructType(Object.class);
    }
}
