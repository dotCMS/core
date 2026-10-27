package com.dotcms.cli.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.cli.common.ContentTypesTestHelperService.ContentTypeDescriptorCreationResult;
import com.dotcms.model.config.Workspace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that the descriptors the helper writes cannot silently collide. A collision once made
 * {@code ContentTypeCommandIT.Test_Command_Content_Type_Folder_Push} flaky, because two descriptors
 * created in the same millisecond shared a variable and the second overwrote the first.
 */
class ContentTypesTestHelperServiceTest {

    private static final int DESCRIPTORS = 50;
    private static final long FROZEN_MILLIS = 1_700_000_000_000L;

    @TempDir
    Path tempDir;

    private final ContentTypesTestHelperService helper = new ContentTypesTestHelperService();
    private Workspace workspace;

    @BeforeEach
    void setUp() throws IOException {
        // Every call lands in the same millisecond, so uniqueness cannot depend on how fast the
        // machine is.
        helper.clock = () -> FROZEN_MILLIS;
        workspace = Workspace.builder().root(tempDir).build();
        Files.createDirectories(workspace.contentTypes());
    }

    /**
     * Descriptors created within the same millisecond must each get their own file.
     */
    @Test
    void contentTypeDescriptorsCreatedInTheSameMillisecondDoNotCollide() throws IOException {

        final Set<String> variables = new HashSet<>();
        for (int i = 0; i < DESCRIPTORS; i++) {
            variables.add(helper.createContentTypeDescriptor(workspace).variable());
        }

        assertEquals(DESCRIPTORS, variables.size());
        assertEquals(DESCRIPTORS, countDescriptors());
    }

    /**
     * Same as above for the HTML page content type descriptors.
     */
    @Test
    void pageContentTypeDescriptorsCreatedInTheSameMillisecondDoNotCollide() throws IOException {

        final Set<String> variables = new HashSet<>();
        for (int i = 0; i < DESCRIPTORS; i++) {
            variables.add(helper.createPageContentTypeDescriptor(workspace).variable());
        }

        assertEquals(DESCRIPTORS, variables.size());
        assertEquals(DESCRIPTORS, countDescriptors());
    }

    /**
     * Writing a descriptor over an existing one must fail instead of replacing it.
     */
    @Test
    void existingContentTypeDescriptorIsNotOverwritten() throws IOException {

        final ContentTypeDescriptorCreationResult first = helper.createContentTypeDescriptorWithIdData(
                workspace, null, "var_existing");
        final String firstContent = Files.readString(first.path());

        final IOException error = assertThrows(IOException.class,
                () -> helper.createContentTypeDescriptorWithIdData(workspace, "other-id", "var_existing"));

        assertTrue(error.getMessage().contains(first.path().toString()));
        assertEquals(firstContent, Files.readString(first.path()));
    }

    /**
     * Same as above for the HTML page content type descriptors.
     */
    @Test
    void existingPageContentTypeDescriptorIsNotOverwritten() throws IOException {

        final ContentTypeDescriptorCreationResult first = helper.createPageContentTypeDescriptor(
                workspace, null, "var_existing_page");
        final String firstContent = Files.readString(first.path());

        final IOException error = assertThrows(IOException.class,
                () -> helper.createPageContentTypeDescriptor(workspace, "other-id", "var_existing_page"));

        assertTrue(error.getMessage().contains(first.path().toString()));
        assertEquals(firstContent, Files.readString(first.path()));
    }

    private long countDescriptors() throws IOException {
        try (Stream<Path> walk = Files.walk(workspace.contentTypes())) {
            return walk.filter(Files::isRegularFile).count();
        }
    }
}
