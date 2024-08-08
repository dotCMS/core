package com.dotmarketing.business.portal;

import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface XMLEnabledBuilder<T> {

    @JsonIgnore
    default Class<T> getInstanceClass() {
        // Check the superclass first
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType) {
            Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
            return (Class<T>) type;
        }

        // If not found, check the interfaces specifically for XmlImmutableBuilder
        Type[] interfaces = getClass().getGenericInterfaces();
        for (Type iface : interfaces) {
            if (iface instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) iface;
                if (paramType.getRawType() == XMLEnabledBuilder.class) {
                    Type type = paramType.getActualTypeArguments()[0];
                    return (Class<T>) type;
                }
            }
        }

        throw new DotRuntimeException("Cannot find parameterized type for XMLEnabledBuilder");
    }



    default T fromXml(String xml) throws IOException {
        return SerializationHelper.fromXml(getInstanceClass(),xml);
    }

    default T fromXml(InputStream is) throws IOException {
        return SerializationHelper.fromXml(getInstanceClass(),is);
    }

}
