package com.dotmarketing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link InodeUtils#hasIdFormat(String)}.
 */
public class InodeUtilsTest {

    /**
     * Method to test: {@link InodeUtils#hasIdFormat(String)}
     * Given: ids in the forms dotCMS generates: a dashed UUID, a UUID without dashes (the form of
     * identifiers built from other values) and a positive number (the form of very old inodes)
     * Should: accept them.
     */
    @Test
    public void hasIdFormat_acceptsGeneratedIds() {
        assertTrue(InodeUtils.hasIdFormat("35f36e04-60f1-4252-aab6-4b9e564c38bd"));
        assertTrue(InodeUtils.hasIdFormat("bf49ad2fda9d281e16a729f5ef9f0ccc"));
        assertTrue(InodeUtils.hasIdFormat("12345"));
        assertTrue(InodeUtils.hasIdFormat("35F36E04-60F1-4252-AAB6-4B9E564C38BD"));
    }

    /**
     * Method to test: {@link InodeUtils#hasIdFormat(String)}
     * Given: values that are not generated ids, including relative paths and names that would
     * place a folder outside the one it is built under
     * Should: refuse them.
     */
    @Test
    public void hasIdFormat_refusesOtherValues() {
        assertFalse(InodeUtils.hasIdFormat(null));
        assertFalse(InodeUtils.hasIdFormat(""));
        assertFalse(InodeUtils.hasIdFormat("0"));
        assertFalse(InodeUtils.hasIdFormat("-1"));
        assertFalse(InodeUtils.hasIdFormat("../felix"));
        assertFalse(InodeUtils.hasIdFormat("../35f36e04-60f1-4252-aab6-4b9e564c"));
        assertFalse(InodeUtils.hasIdFormat("35f36e04-60f1-4252-aab6-4b9e564c38bd/.."));
        assertFalse(InodeUtils.hasIdFormat("SYSTEM_HOST"));
        assertFalse(InodeUtils.hasIdFormat("35f36e04-60f1-4252-aab6-4b9e564c38b"));
    }

    /**
     * Method to test: {@link InodeUtils#hasIdFormat(String)}
     * Given: values made only of hexadecimal digits and dashes, but with dashes in other places
     * than the 8-4-4-4-12 layout of a UUID, and a single-digit number, which is too short for the
     * two one-character folders the asset store places above each inode folder
     * Should: refuse them.
     */
    @Test
    public void hasIdFormat_refusesMalformedUuidsAndSingleDigits() {
        assertFalse(InodeUtils.hasIdFormat("35f36e04-60f1-4252-aab6-4b9e564c38bd-"));
        assertFalse(InodeUtils.hasIdFormat("35f36e04--60f1-4252-aab6-4b9e564c38bd"));
        assertFalse(InodeUtils.hasIdFormat("35f36e0460f1-4252-aab6-4b9e-564c38bd"));
        assertFalse(InodeUtils.hasIdFormat("-35f36e04-60f1-4252-aab6-4b9e564c38bd"));
        assertFalse(InodeUtils.hasIdFormat("5"));
    }

}
