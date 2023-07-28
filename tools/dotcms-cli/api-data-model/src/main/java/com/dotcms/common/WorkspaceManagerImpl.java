package com.dotcms.common;

import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.config.WorkspaceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.logging.Logger;
@DefaultBean
@ApplicationScoped
public class WorkspaceManagerImpl implements WorkspaceManager {

    public static final String DOT_WORKSPACE_YML = ".dot-workspace.yml";

    private static final ObjectMapper mapper = new YAMLMapperSupplier().get();

    @Inject
    Logger logger;

    Workspace persist(final Workspace workspace) throws IOException {
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

        final Path rootPath = workspace.root().resolve(DOT_WORKSPACE_YML);
        if (!Files.exists(rootPath)) {
            try (var outputStream = Files.newOutputStream(rootPath)) {
                mapper.writeValue(outputStream, WorkspaceInfo.builder().name("default").build());
            }
        }

        return workspace;
    }

    Workspace workspace(final Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("Path [%s] must be a directory", path)
            );
        }
        //All other fields are derived therefore we only need to set the root
        return Workspace.builder().root(path)
                .build();
    }

    Optional<Path> findProjectRoot(Path currentPath) {

        if (Files.exists(currentPath.resolve(DOT_WORKSPACE_YML))) {
            logger.debug("Found workspace at: " + currentPath.toAbsolutePath());
            return Optional.of(currentPath);
        } else {
            Path parent;
            Path workingPath = currentPath.toAbsolutePath();
            logger.debug("looking up workspace, current path is : " + workingPath + " and parent is " + workingPath.getParent());
            while ((parent = workingPath.getParent()) != null) {
                if (Files.exists(parent.resolve(DOT_WORKSPACE_YML))) {
                    logger.debug("Found workspace at: " + currentPath.toAbsolutePath());
                    return Optional.of(parent);
                }
                workingPath = parent;
            }
        }
        return Optional.empty();
    }

    @Override
    public Workspace getOrCreate(Path currentPath) throws IOException {
        final Optional<Workspace> workspace = findWorkspace(currentPath);
        if (workspace.isPresent()) {
            //calling persist to make sure all the directories were not removed
            return persist(
                 workspace.get()
            );
        } else {
            //No workspace found, create one
            return persist(
                workspace(currentPath)
            );
        }
    }

    @Override
    public Workspace getOrCreate() throws IOException {
        return getOrCreate(Path.of("").toAbsolutePath());
    }

    public Optional<Workspace> findWorkspace(Path currentPath) {
        final Optional<Path> projectRoot = findProjectRoot(currentPath);
        return projectRoot.map(this::workspace);
    }

    public Optional<Workspace> findWorkspace() {
        final Optional<Path> projectRoot = findProjectRoot(Path.of("").toAbsolutePath());
        return projectRoot.map(this::workspace);
    }

    @Override
    public void destroy(final Workspace workspace) throws IOException {
        deleteDirectoryStream(workspace.sites());
        deleteDirectoryStream(workspace.languages());
        deleteDirectoryStream(workspace.contentTypes());
        deleteDirectoryStream(workspace.files());
        Files.deleteIfExists(workspace.root().resolve(DOT_WORKSPACE_YML));
    }

    private void deleteDirectoryStream(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(File::delete);
        }
    }

}
