package com.dotcms.storage;

import java.io.Serializable;

public class ObjectPath implements Serializable {

    private final String path;
    private final Object object;

    public ObjectPath(final String path, final Object object) {
        this.path = path;
        this.object = object;
    }

    public String getPath() {
        return path;
    }

    public Object getObject() {
        return object;
    }
}
