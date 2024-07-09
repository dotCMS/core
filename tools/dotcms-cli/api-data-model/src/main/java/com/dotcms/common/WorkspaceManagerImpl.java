package com.dotcms.common;

import static com.dotcms.common.WorkspaceManager.*;

import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.config.WorkspaceInfo;
import com.dotcms.model.config.WorkspaceInfo.Builder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.inject.Inject;
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

        final Builder builder = WorkspaceInfo.builder();
        if (null != workspace.id()) {
            builder.id(workspace.id());
        }
        final WorkspaceInfo info = builder.build();
        if (!Files.exists(rootPath)) {
            try (var outputStream = Files.newOutputStream(rootPath)) {
                mapper.writeValue(outputStream, info);
            }
        }

        return Workspace.builder().from(workspace).id(info.id()).build();
    }

    Workspace workspace(final Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("Path [%s] must be a directory", path)
            );
        }

        return workspaceInfo(path)
                //if the workspace info is present, we will use it to create the workspace extracting the id
                .map(info -> Workspace.builder().id(info.id()).root(path).build())
                //if the workspace info is not present, we will create a new empty workspace
                .orElseGet(() -> Workspace.builder().root(path).build());
    }

    Optional<Path> findProjectRoot(Path currentPath) {

        if (Files.exists(currentPath.resolve(DOT_WORKSPACE_YML))) {
            logger.info("Found workspace at: " + currentPath.toAbsolutePath());
            return Optional.of(currentPath);
        } else {
            Path parent;
            Path workingPath = currentPath.toAbsolutePath();
            logger.info("looking up workspace, current path is : " + workingPath + " and parent is "
                    + workingPath.getParent());
            while ((parent = workingPath.getParent()) != null) {
                if (Files.exists(parent.resolve(DOT_WORKSPACE_YML))) {
                    logger.info("Found workspace at: " + currentPath.toAbsolutePath());
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
            //calling persist to make sure all the directories are still there
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

    public Workspace getOrCreate(final Path currentPath, final boolean findWorkspace) throws IOException {

        if(findWorkspace){
            return getOrCreate(currentPath);
        }

        //This will force that use of the current path as the workspace. If no workspace info is found, it will create one.
        return persist(
                workspace(currentPath)
        );
    }

    public Optional<Workspace> findWorkspace(Path currentPath) {
        // Resolve the path as it may be relative
        final Path resolvedPath = resolvePath(currentPath);
        logger.debugf("currentPath = %s", resolvedPath);

        final File file = resolvedPath.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException(
                    String.format("Path [%s] does not exist", resolvedPath)
            );
        }
        final Optional<Path> projectRoot = findProjectRoot(resolvedPath);
        return projectRoot.map(this::workspace);
    }

    /**
     * Reads the file
     * @param path the path to the workspace
     * @return the workspace info
     */
    Optional<WorkspaceInfo> workspaceInfo(final Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("Path [%s] must be a directory", path)
            );
        }
        final Path resolved = path.resolve(DOT_WORKSPACE_YML);
        if (Files.exists(resolved)) {
            try {
                return Optional.of(mapper.readValue(resolved.toFile(), WorkspaceInfo.class));
            } catch (IOException e) {
                logger.error("Error reading workspace info", e);
            }
        }
        return Optional.empty();
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
