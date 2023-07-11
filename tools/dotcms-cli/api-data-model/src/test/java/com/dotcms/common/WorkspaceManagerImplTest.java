package com.dotcms.common;

import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WorkspaceManagerImplTest {

    @Inject
    WorkspaceManager workspaceManager;

    @BeforeEach
    public void setupTest() throws IOException {

    }

    @Test
    void Test_Find_Root() throws IOException {
        final Path homeDir = Path.of(System.getProperty("user.home"),".dotcms").toAbsolutePath();
        WorkspaceManagerImpl workspaceManager = new WorkspaceManagerImpl();
        final Optional<Path> root = workspaceManager.findProjectRoot(homeDir);
        System.out.println(root);
    }

}