package com.dotmarketing.portlets.contentlet.business.json.types;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Date;

public class SerializerProvider {

    final NullSerializer nullSerializer = new NullSerializer();

    private final ImmutableMap<Class<?>, DataTypeSerializer<? extends Serializable, ? extends Serializable>> serializers = ImmutableMap
            .<Class<?>, DataTypeSerializer<?, ?>>builder()
            .put(Date.class, new DateSerializer())
            .build();

    public DataTypeSerializer getSerializer(Class<?> clazz) {
        final DataTypeSerializer typeSerializer = serializers.get(clazz);
        return typeSerializer == null ? nullSerializer : typeSerializer;
    }

    private SerializerProvider() {
    }

    public enum INSTANCE {
        INSTANCE;
        private final SerializerProvider provider = new SerializerProvider();

        public static SerializerProvider get() {
            return INSTANCE.provider;
        }

    }

}
