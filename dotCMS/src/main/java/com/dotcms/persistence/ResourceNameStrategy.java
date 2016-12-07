package com.dotcms.persistence;

import java.io.Serializable;
import java.util.List;

/**
 * Based on a class a resource name could be:
 * - A file in the classpath.
 * - A file in the filesystem.
 * - A {@link QueryMap} class name implementation.
 * @author jsanca
 */
public interface ResourceNameStrategy extends Serializable {

    /**
     * Return a collection of resources names (a resource could be a file in the classpath,
     * a file in the filesystem or a class name implementing a {@link QueryMap})
     * @param persistenceClass {@link Class}
     * @return List
     */
    List<String> getResourceNames (Class persistenceClass);

} // E:O:F:ResourceNameStrategy.
