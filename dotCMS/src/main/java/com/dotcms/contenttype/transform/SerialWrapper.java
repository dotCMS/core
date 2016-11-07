package com.dotcms.contenttype.transform;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class SerialWrapper<T> implements Serializable{
    private static final long serialVersionUID = 1L;
    @JsonUnwrapped
    final private T inner;
    final private Class implClass;

    public SerialWrapper(T inner, Class field) {
        this.inner = inner;
        this.implClass = field;
    }

    public T getInner() {
        return inner;
    }

    public Class getImplClass() {
        return implClass;
    }
}
