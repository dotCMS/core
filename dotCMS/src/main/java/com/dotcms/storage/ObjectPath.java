package com.dotcms.storage;

import java.io.Serializable;

/**
 * Represents an object recovered from a storage, it contains the path and the object itself.
 * @author jsanca
 */
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
