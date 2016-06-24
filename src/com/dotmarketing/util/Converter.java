package com.dotmarketing.util;

import java.io.Serializable;

/**
 * Just a contract to convert an original bean to destiny bean
 * @author jsanca
 */
public interface Converter<Original, Destiny> extends Serializable {

    /**
     * Does the convertion
     * @param original Original
     * @return Destiny
     */
    Destiny convert (Original original);

} // E:O:F:Converter.
