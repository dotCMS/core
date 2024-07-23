package com.dotmarketing.business.portal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface XmlImmutableBuilder<T> {

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
                if (paramType.getRawType() == XmlImmutableBuilder.class) {
                    Type type = paramType.getActualTypeArguments()[0];
                    return (Class<T>) type;
                }
            }
        }

        throw new RuntimeException("Invalid class definition");
    }



    default T fromXml(String xml) throws IOException {
        return XmlUtil.fromXml(getInstanceClass(),xml);
    }

    default T fromXml(InputStream is) throws IOException {
        return XmlUtil.fromXml(getInstanceClass(),is);
    }

}
