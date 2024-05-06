package com.dotcms.storage;

import java.io.Serializable;

/**
 * Represents an object returned by a Storage Provider. It contains the path to a given object and
 * the object itself. This data structure allows the Storage Replication Job to keep track of all
 * objects in the origin Storage Type and replicate them to one or more Storage Types.
 *
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
