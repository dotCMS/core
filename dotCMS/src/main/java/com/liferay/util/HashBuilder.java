package com.liferay.util;

/**
 * Encapsulates a builder for a hash.
 * @author jsanca
 */
public interface HashBuilder {

    /**
     * Appends bytes to the algorithm
     * @param bytes byte []
     */
    void append(final byte [] bytes);

    /**
     * Build the hash as a hexadecimal string.
     * @return String
     */
    String buildHexa ();

    /**
     * Build the hash as a byte.
     * @return byte []
     */
    byte [] buildBytes ();
}
