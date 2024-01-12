package com.dotcms.common;

import com.dotcms.model.config.Workspace;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WorkspaceManagerTest {

    @Inject
    WorkspaceManager workspaceManager;

    @BeforeEach
    public void setupTest() throws IOException {

    }

    /**
     * Given scenario: This is a simple capabilities test for the WorkspaceManager
     * Expected result: The WorkspaceManager should be able to create and destroy a workspace
     * @throws IOException
     */
    @Test
    void  Create_Verify_Then_Destroy_Workspace() throws IOException {
        final Path testDir = Files.createTempDirectory("tmpDirPrefix").toAbsolutePath();
        Files.createDirectories(testDir);
        Assertions.assertTrue(testDir.toFile().exists());
        final Optional<Workspace> workspace = workspaceManager.findWorkspace(testDir);
        Assertions.assertFalse(workspace.isPresent());
        final Workspace newWorkspace = workspaceManager.getOrCreate(testDir);
        Assertions.assertTrue(Files.exists(newWorkspace.root().resolve(".dot-workspace.yml" )));
        Assertions.assertTrue(Files.exists(newWorkspace.contentTypes()));
        Assertions.assertTrue(Files.exists(newWorkspace.languages()));
        Assertions.assertTrue(Files.exists(newWorkspace.files()));
        Assertions.assertTrue(Files.exists(newWorkspace.sites()));
        workspaceManager.destroy(newWorkspace);
        Assertions.assertFalse(Files.exists(newWorkspace.root().resolve(".dot-workspace.yml" )));
        Assertions.assertFalse(Files.exists(newWorkspace.contentTypes()));
        Assertions.assertFalse(Files.exists(newWorkspace.languages()));
        Assertions.assertFalse(Files.exists(newWorkspace.sites()));
    }


    @Test
    void  Create_Nested_Workspaces_Then_Verify_Path() throws IOException {
        //Let's make a workspace
        final Path testDir = Files.createTempDirectory("tmpDirPrefix").toAbsolutePath();
        Files.createDirectories(testDir);
        Assertions.assertTrue(testDir.toFile().exists());
        final Workspace workspace = workspaceManager.getOrCreate(testDir);
        //Now lets make a nested workspace (which is not the recommended approach) but it serves as a test
        final Path testDir2 = workspace.root().resolve("testDir2");
        Files.createDirectories(testDir2);
        Assertions.assertTrue(testDir2.toFile().exists());
        //Test the new method that will create a workspace without attempting to find a root parent
        final Workspace nested = workspaceManager.getOrCreate(testDir2, false);
        Assertions.assertTrue(Files.exists(nested.root().resolve(".dot-workspace.yml" )));
        Assertions.assertEquals(nested.root(), testDir2);

        final Path testDir3 = workspace.root().resolve("testDir3");
        Files.createDirectories(testDir3);
        Assertions.assertTrue(testDir3.toFile().exists());
        final Workspace parent = workspaceManager.getOrCreate(testDir2, true);

    }

}