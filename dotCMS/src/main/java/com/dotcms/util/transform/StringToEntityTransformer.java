package com.dotcms.util.transform;

/**
 * Transform a String to an entity type T
 * @param <T>
 */
@FunctionalInterface
public interface StringToEntityTransformer<T> {

    /**
     * Transform from string to T
     * @param value {@link String}
     * @return T
     */
    T from(String value);
} // E:O:F:StringToEntityTransformer.
