package com.dotcms.util;

/**
 * Simple class for mark a class DotCloneable
 * This give us some control over the cache objects in order to make sure that we really want to clone
 * the object
 * @author jsanca
 */
public interface DotCloneable extends Cloneable {

    Object clone() throws CloneNotSupportedException;
}
