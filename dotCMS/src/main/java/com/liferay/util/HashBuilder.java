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
    HashBuilder append(final byte [] bytes);

    /**
     * Max quantity of bytes
     * @param bytes    byte [] array
     * @param maxBytes {@link Integer}
     */
    HashBuilder append(final byte [] bytes, final int maxBytes);

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

    /**
     * This returns the hash as the unix tools used to do/
     * @return String
     */
    String buildUnixHash();
}
