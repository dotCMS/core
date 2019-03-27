package com.dotcms.auth.providers.jwt.factories;

import java.io.Serializable;
import java.security.Key;

/**
 * Provides a mechanism for generating secure signing keys.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public interface SigningKeyFactory extends Serializable {

    /**
     * Creates the custom key to for authenticating secure objects.
     * 
     * @return Key
     */
    public Key getKey();

} // E:O:F:SigningKeyFactory.
