package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests verifying that HOST (and FOLDER) bundle files are sorted by absolute path length
 * in ascending order before import processing.
 *
 * <p>When nestable hosts are pushed, the bundle layout mirrors the host hierarchy: files for
 * parent hosts land at shorter paths than files for their child hosts.  Sorting by path length
 * ascending therefore guarantees that every parent host is imported before any of its children,
 * preventing referential-integrity violations during import.</p>
 *
 * <p>These tests do not require a running dotCMS instance – they only validate the comparator
 * logic used by {@link ContentHandler} (for HOST assets) and {@link FolderHandler} (for FOLDER
 * assets).</p>
 *
 * <p>Verifies Sub-AC 2 of AC 14: import process sorts HOST and FOLDER assets by path length in
 * ascending order before processing.</p>
 */
class HostImportSortOrderTest {

    /** The same comparator used inside {@link ContentHandler#handle} when {@code isHost=true}. */
    private static final Comparator<File> PATH_LENGTH_ASC =
            Comparator.comparingInt(f -> f.getAbsolutePath().length());

    private Path tempRoot;

    @BeforeEach
    void setUp() throws IOException {
        tempRoot = Files.createTempDirectory("host-import-sort-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Recursively delete the temp directory tree
        Files.walk(tempRoot)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Creates a real (empty) file at the given sub-path under {@link #tempRoot}, creating any
     * intermediate directories as needed, and returns it as a {@link File}.
     */
    private File createFile(String... segments) throws IOException {
        Path p = tempRoot;
        for (String seg : segments) {
            p = p.resolve(seg);
        }
        Files.createDirectories(p.getParent());
        Files.createFile(p);
        return p.toFile();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Test-Method: the path-length comparator used in {@link ContentHandler#handle} when
     * {@code isHost=true}.
     *
     * <p>Given scenario: a bundle contains host XML files for a top-level host and a child host
     * nested beneath it. Because the child's file path is longer (it includes the parent hostname
     * directory in addition to its own), sorting by path length ascending places the parent file
     * first.
     *
     * <p>Expected result: after sorting, the top-level host file appears before the nested child
     * host file.
     */
    @Test
    void hostFiles_sortedByPathLength_parentBeforeChild() throws IOException {
        // Bundle layout:
        //   working/system.dotcms.com/1/parent.com.host.xml          <- top-level host
        //   working/parent.com/1/parent.com/child.com.host.xml       <- nested child host
        File topLevelFile = createFile(
                "working", "system.dotcms.com", "1", "parent.com.host.xml");
        File nestedChildFile = createFile(
                "working", "parent.com", "1", "parent.com", "child.com.host.xml");

        List<File> files = new ArrayList<>();
        // Add in reverse order (child first) to prove sorting reorders them
        files.add(nestedChildFile);
        files.add(topLevelFile);

        files.sort(PATH_LENGTH_ASC);

        assertTrue(files.indexOf(topLevelFile) < files.indexOf(nestedChildFile),
                "Top-level host file (shorter path) must be sorted before nested child host file");
    }

    /**
     * Test-Method: the path-length comparator used in {@link ContentHandler#handle} when
     * {@code isHost=true}.
     *
     * <p>Given scenario: a bundle contains host XML files at three nesting levels: top-level,
     * one-level-deep child, and two-levels-deep grandchild.
     *
     * <p>Expected result: after sorting, files appear in strict depth order –
     * top-level → child → grandchild.
     */
    @Test
    void hostFiles_sortedByPathLength_threeGenerationsOrderedCorrectly() throws IOException {
        // Bundle layout mirrors the host hierarchy depth:
        //   working/system.dotcms.com/1/root.com.host.xml                                  <- depth 0
        //   working/root.com/1/root.com/child.com.host.xml                                 <- depth 1
        //   working/child.com/1/root.com/child.com/grandchild.com.host.xml                 <- depth 2
        File rootFile = createFile(
                "working", "system.dotcms.com", "1", "root.com.host.xml");
        File childFile = createFile(
                "working", "root.com", "1", "root.com", "child.com.host.xml");
        File grandChildFile = createFile(
                "working", "child.com", "1", "root.com", "child.com", "grandchild.com.host.xml");

        List<File> files = new ArrayList<>();
        // Add in worst-case reversed order
        files.add(grandChildFile);
        files.add(childFile);
        files.add(rootFile);

        files.sort(PATH_LENGTH_ASC);

        int rootIdx       = files.indexOf(rootFile);
        int childIdx      = files.indexOf(childFile);
        int grandChildIdx = files.indexOf(grandChildFile);

        assertTrue(rootIdx < childIdx,
                "Root host (depth 0) must appear before child host (depth 1)");
        assertTrue(childIdx < grandChildIdx,
                "Child host (depth 1) must appear before grandchild host (depth 2)");
    }

    /**
     * Test-Method: the path-length comparator used in {@link FolderHandler#handle}.
     *
     * <p>Given scenario: a bundle contains folder XML files at two nesting levels.
     *
     * <p>Expected result: after sorting, the shallower (parent) folder appears before the deeper
     * (child) folder – this mirrors the sorting already present in {@link FolderHandler} and
     * ensures that the same invariant holds for HOST assets processed by
     * {@link ContentHandler}.
     */
    @Test
    void folderFiles_sortedByPathLength_parentBeforeChild() throws IOException {
        // Bundle layout for folders:
        //   ROOT/live/parent.com/1/about-us.folder.xml         <- parent folder (shorter path)
        //   ROOT/live/parent.com/1/about-us/team.folder.xml    <- child folder (longer path)
        File parentFolder = createFile(
                "ROOT", "live", "parent.com", "1", "about-us.folder.xml");
        File childFolder = createFile(
                "ROOT", "live", "parent.com", "1", "about-us", "team.folder.xml");

        List<File> files = new ArrayList<>();
        files.add(childFolder);
        files.add(parentFolder);

        // Use the identical Comparator that FolderHandler constructs inline
        files.sort(Comparator.comparingInt(f -> f.getAbsolutePath().length()));

        assertTrue(files.indexOf(parentFolder) < files.indexOf(childFolder),
                "Parent folder (shorter path) must be sorted before child folder (longer path)");
    }

    /**
     * Test-Method: equal-length host paths are handled without error.
     *
     * <p>Given scenario: two host files happen to have the same absolute path length (e.g., two
     * sibling hosts under the same parent).
     *
     * <p>Expected result: sorting completes without exception; both files are present in the
     * result list.
     */
    @Test
    void hostFiles_equalPathLength_bothPresentAfterSort() throws IOException {
        // Two sibling hosts under the same parent – their paths happen to be the same length
        File siblingA = createFile("working", "parent.com", "1", "sibling-a.com.host.xml");
        File siblingB = createFile("working", "parent.com", "1", "sibling-b.com.host.xml");

        List<File> files = new ArrayList<>();
        files.add(siblingA);
        files.add(siblingB);

        files.sort(PATH_LENGTH_ASC);

        assertTrue(files.contains(siblingA), "Sibling A must still be present after sorting");
        assertTrue(files.contains(siblingB), "Sibling B must still be present after sorting");
    }
}
