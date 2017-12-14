package com.dotcms.util.transform;

import java.util.List;

/**
 * Interface that contains the definition that a transformer needs to implement in order to convert
 * DB objects into entities(eg. Folder, Template, Containers)
 * @param <T>
 */
public interface DBTransformer<T> {

    /**
     * @return List of converted objects
     */
    List<T> asList();
}
