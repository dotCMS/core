package com.dotmarketing.util.jwt;

import io.jsonwebtoken.impl.crypto.MacProvider;

import java.io.Serializable;
import java.security.Key;

/**
 * Creates a custom signing key factory, MacProvider by default.
 * @author jsanca
 */
public interface SigningKeyFactory extends Serializable {

    /**
     * Creates the custom key
     * @return Key
     */
    public default Key getKey () {

        return MacProvider.generateKey();
    } // createKey

} // E:O:F:SigningKeyFactory.
