package com.dotcms.common;

import com.dotcms.model.config.Workspace;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import jakarta.inject.Inject;
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


    /**
     * Given scenario: We want to create a workspace and then create a nested workspace inside of it
     * then corroborate that if we use the getOrCreate method despite the value passed in the findWorkspace parameter we get the same proper workspace
     * Expected result: when calling getOrCreate from the nested workspace we should get the same workspace despite
     * the value passed in the findWorkspace parameter. This because the nested workspace is fully capable of finding no need to find a parent
     * @throws IOException
     */
    @Test
    void  Create_Nested_Workspaces_Then_Verify_Path() throws IOException {
        //Let's make a workspace
        final Path testDir = Files.createTempDirectory("tmpDirPrefix").toAbsolutePath();
        Files.createDirectories(testDir);
        Assertions.assertTrue(testDir.toFile().exists());
        final Workspace workspace = workspaceManager.getOrCreate(testDir);
        Assertions.assertNotNull(workspace.id());
        //Now lets make a nested workspace (which is not the recommended approach) but it serves as a test
        final Path nestedDir2 = workspace.root().resolve("nestedDir2");
        Files.createDirectories(nestedDir2);
        Assertions.assertTrue(nestedDir2.toFile().exists());
        //Test the new method that will create a workspace without attempting to find a root parent
        final Workspace nested = workspaceManager.getOrCreate(nestedDir2, false);
        Assertions.assertTrue(Files.exists(nested.root().resolve(".dot-workspace.yml" )));
        Assertions.assertEquals(nested.root(), nestedDir2);

        //Now test the "get or create" method won't attempt to find a root parent
        //Since we're giving it a nested workspace it should return the same workspace
        final Workspace test = workspaceManager.getOrCreate(nestedDir2, true);
        Assertions.assertEquals(test.root(), nested.root());
        Assertions.assertEquals(test.id(), nested.id());
    }

}