package com.dotcms.common;

import com.dotcms.model.config.Workspace;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
class WorkspaceManagerTest {

    @Inject
    WorkspaceManager workspaceManager;

    @BeforeEach
    public void setupTest() throws IOException {

    }

    @Test
    void  Create_Verify_Then_Destroy_Workspace() throws IOException {
        final Path testDir = Files.createTempDirectory("tmpDirPrefix").toAbsolutePath();
        Files.createDirectories(testDir);
        Assert.assertTrue(testDir.toFile().exists());
        final Optional<Workspace> workspace = workspaceManager.findWorkspace(testDir);
        Assert.assertFalse(workspace.isPresent());
        final Workspace newWorkspace = workspaceManager.getOrCreate(testDir);
        Assert.assertTrue(Files.exists(newWorkspace.contentTypes()));
        Assert.assertTrue(Files.exists(newWorkspace.languages()));
        Assert.assertTrue(Files.exists(newWorkspace.files()));
        Assert.assertTrue(Files.exists(newWorkspace.sites()));
        workspaceManager.destroy(newWorkspace);
        Assert.assertFalse(Files.exists(newWorkspace.contentTypes()));
        Assert.assertFalse(Files.exists(newWorkspace.languages()));
        Assert.assertFalse(Files.exists(newWorkspace.sites()));
    }

}