package com.dotcms.contenttype.transform;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class SerialWrapper<T> implements Serializable{
    private static final long serialVersionUID = 1L;
    @JsonUnwrapped
    final private T inner;
    final private Class implClass;

    public SerialWrapper(T inner, Class clazz) {
        this.inner = inner;
        this.implClass = clazz;
    }

    public T getInner() {
        return inner;
    }

    public Class getImplClass() {
        return implClass;
    }
}
