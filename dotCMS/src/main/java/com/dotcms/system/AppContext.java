package com.dotcms.system;

/**
 * It is intended to be a global context
 */
public interface AppContext {

    <T> T getAttribute(String attributeName);

    <T> void setAttribute(String attributeName, T attributeValue);

    /**
     * The Id for the context, by default is the hashCode on string representation, it could change depending on the implementation
     * @return String
     */
    default String getId () {

        return String.valueOf(this.hashCode());
    }
}
