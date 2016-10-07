package com.dotcms.contenttype.transform;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class JsonWrapper<T> {
    @JsonUnwrapped
    final private T inner;
    final private Class implClass;

    public JsonWrapper(T inner, Class field) {
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
