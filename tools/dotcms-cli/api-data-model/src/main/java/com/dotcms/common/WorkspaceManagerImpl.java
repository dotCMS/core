package com.dotcms.common;

import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.config.WorkspaceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

@DefaultBean
@ApplicationScoped
public class WorkspaceManagerImpl implements WorkspaceManager {

    public static final String DOT_WORKSPACE_DIR_PATTERN = "^dot-workspace-\\d+$";

    public static final String DOT_WORKSPACE_YML = ".dot-workspace.yml";

    private static final ObjectMapper mapper = new YAMLMapperSupplier().get();

    Workspace persist(Workspace workspace) throws IOException {

        final Path files = workspace.files();
        final Path contentTypes = workspace.contentTypes();
        final Path sites = workspace.sites();
        final Path languages = workspace.languages();

        if (!Files.exists(files)) {
            Files.createDirectories(files);
        }

        if (!Files.exists(contentTypes)) {
            Files.createDirectories(contentTypes);
        }

        if (!Files.exists(sites)) {
            Files.createDirectories(sites);
        }

        if (!Files.exists(languages)) {
            Files.createDirectories(languages);
        }

        try (var outputStream = Files.newOutputStream(workspace.root().resolve(DOT_WORKSPACE_YML))) {
            mapper.writeValue(outputStream, WorkspaceInfo.builder().name("default").build());
        }

        return workspace;
    }

    Workspace workspace(final Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("Path [%s] must be a directory", path)
            );
        }

        Path workingPath = path;

        //if we're not already within a workspace directory, create one
        if(!workingPath.getFileName().toString().matches(DOT_WORKSPACE_DIR_PATTERN)){
            workingPath = Path.of(path.toString(), workspaceEnclosingDirName());
        }
        //All other fields are derived therefore we only need to set the root
        return Workspace.builder().root(workingPath)
                .build();
    }

    String workspaceEnclosingDirName() {
        return String.format("dot-workspace-%d",System.currentTimeMillis());
    }

    Optional<Path> findProjectRoot(Path currentPath) {
        if (Files.exists(currentPath.resolve(DOT_WORKSPACE_YML))) {
            return Optional.of(currentPath);
        } else {
            Path parent;
            while ((parent = currentPath.getParent()) != null) {
                if (Files.exists(parent.resolve(DOT_WORKSPACE_YML))) {
                    return Optional.of(parent);
                }
                currentPath = parent;
            }
        }
        return Optional.empty();
    }

    @Override
    public Workspace resolve(Path currentPath) throws IOException {
        final Optional<Path> projectRoot = findProjectRoot(currentPath);
        if (projectRoot.isPresent()) {
            return workspace(projectRoot.get());
        } else {
            return persist(
                workspace(currentPath)
            );
        }
    }

    @Override
    public Workspace resolve() throws IOException {
        return resolve(Path.of("").toAbsolutePath());
    }
}
