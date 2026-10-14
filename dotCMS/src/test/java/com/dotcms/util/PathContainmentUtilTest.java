package com.dotcms.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link PathContainmentUtil}.
 */
public class PathContainmentUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Method to test: {@link PathContainmentUtil#isPlainSegment(String)}
     * When: the name is a single file name
     * Should: return true.
     */
    @Test
    public void test_isPlainSegment_acceptsSingleFileNames() {
        assertTrue(PathContainmentUtil.isPlainSegment("bundle.tar.gz"));
        assertTrue(PathContainmentUtil.isPlainSegment("6f1c2d3e-0a4b-4c5d-8e9f-a1b2c3d4e5f6.tar.gz"));
        assertTrue(PathContainmentUtil.isPlainSegment("my bundle (1).tar.gz"));
        assertTrue(PathContainmentUtil.isPlainSegment(".hidden"));
    }

    /**
     * Method to test: {@link PathContainmentUtil#isPlainSegment(String)}
     * When: the name is null, empty, a dot segment, or has a separator or a NUL
     * Should: return false.
     */
    @Test
    public void test_isPlainSegment_rejectsNamesThatAreNotSingleSegments() {
        assertFalse(PathContainmentUtil.isPlainSegment(null));
        assertFalse(PathContainmentUtil.isPlainSegment(""));
        assertFalse(PathContainmentUtil.isPlainSegment("."));
        assertFalse(PathContainmentUtil.isPlainSegment(".."));
        assertFalse(PathContainmentUtil.isPlainSegment("a/b"));
        assertFalse(PathContainmentUtil.isPlainSegment("../a"));
        assertFalse(PathContainmentUtil.isPlainSegment("a\\b"));
        assertFalse(PathContainmentUtil.isPlainSegment("/absolute"));
        assertFalse(PathContainmentUtil.isPlainSegment("a\0b"));
    }

    /**
     * Method to test: {@link PathContainmentUtil#isContainedIn(File, File)}
     * When: the candidate is a direct child or a deeper descendant of the root
     * Should: return true.
     */
    @Test
    public void test_isContainedIn_acceptsDescendants() throws IOException {
        final File root = temporaryFolder.newFolder("root");

        assertTrue(PathContainmentUtil.isContainedIn(root, new File(root, "child.tar.gz")));
        assertTrue(PathContainmentUtil.isContainedIn(root, new File(root, "dir/child.txt")));
    }

    /**
     * Method to test: {@link PathContainmentUtil#isContainedIn(File, File)}
     * When: the candidate is the root itself
     * Should: return false.
     */
    @Test
    public void test_isContainedIn_rejectsTheRootItself() throws IOException {
        final File root = temporaryFolder.newFolder("root");

        assertFalse(PathContainmentUtil.isContainedIn(root, root));
        assertFalse(PathContainmentUtil.isContainedIn(root, new File(root, "dir/..")));
    }

    /**
     * Method to test: {@link PathContainmentUtil#isContainedIn(File, File)}
     * When: the candidate resolves above the root through parent segments
     * Should: return false.
     */
    @Test
    public void test_isContainedIn_rejectsParentSegments() throws IOException {
        final File root = temporaryFolder.newFolder("root");

        assertFalse(PathContainmentUtil.isContainedIn(root, new File(root, "../outside.txt")));
        assertFalse(PathContainmentUtil.isContainedIn(root, new File(root, "dir/../../outside.txt")));
    }

    /**
     * Method to test: {@link PathContainmentUtil#isContainedIn(File, File)}
     * When: the candidate is in a sibling directory whose name starts with the name of the root
     * Should: return false.
     */
    @Test
    public void test_isContainedIn_rejectsSiblingSharingNamePrefix() throws IOException {
        final File root = temporaryFolder.newFolder("bundles");
        final File sibling = temporaryFolder.newFolder("bundles_other");

        assertFalse(PathContainmentUtil.isContainedIn(root, new File(sibling, "file.txt")));
        assertFalse(PathContainmentUtil.isContainedIn(root, new File(root, "../bundles_other/file.txt")));
    }

    /**
     * Method to test: {@link PathContainmentUtil#isContainedIn(File, File)}
     * When: the candidate is reached through a symbolic link inside the root that points outside
     * Should: return false.
     */
    @Test
    public void test_isContainedIn_rejectsSymbolicLinkPointingOutside() throws IOException {
        final File root = temporaryFolder.newFolder("root");
        final File outside = temporaryFolder.newFolder("elsewhere");
        final File link = new File(root, "link");
        Files.createSymbolicLink(link.toPath(), outside.toPath());

        assertFalse(PathContainmentUtil.isContainedIn(root, new File(link, "file.txt")));
    }

    /**
     * Method to test: {@link PathContainmentUtil#isContainedIn(File, File)}
     * When: either argument is null
     * Should: return false.
     */
    @Test
    public void test_isContainedIn_rejectsNullArguments() throws IOException {
        final File root = temporaryFolder.newFolder("root");

        assertFalse(PathContainmentUtil.isContainedIn(null, new File(root, "a")));
        assertFalse(PathContainmentUtil.isContainedIn(root, null));
    }
}
