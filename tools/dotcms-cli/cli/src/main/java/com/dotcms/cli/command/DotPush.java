package com.dotcms.cli.command;

import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Describes a command that has a {@link PushMixin} and can provide the name of its custom mixin.
 * This interface is intended to be implemented by classes that represents specific push commands
 * and require common push behaviors and characteristics encapsulated in {@link PushMixin}.
 * Furthermore, it can provide the name of a custom mixin it uses, which is useful for custom
 * command line configurations where specific logic may be associated with certain mixin names.
 */
public interface DotPush extends DotCommand {

    /**
     * Returns the {@link PushMixin} associated with the implementing class. This {@link PushMixin}
     * object encapsulates the behavior, options, and parameters that are common and essential for a
     * push command in the CLI.
     *
     * @return the {@link PushMixin} associated with this push command
     */
    PushMixin getPushMixin();

    /**
     * Returns the name of the custom mixin associated with the implementing class. The mixin's name
     * is used to identify the mixin in custom configurations or logic where a specific behavior or
     * setup is required for that mixin.
     *
     * @return the name of the custom mixin associated with this push command
     */
    default Optional<String> getCustomMixinName() {
        return Optional.empty();
    }

    /**
     * Returns the execution order of this push command.
     *
     * @return the execution order of this push command
     */
    default int getOrder() {
        return Integer.MAX_VALUE;  // default to the highest possible value
    }

    /**
     * Returns whether this push command is a global push command. A global push command is a push
     * @return true if this push command is a global push command; false otherwise
     */
    default boolean isGlobalPush() {
        return false;
    }

    WorkspaceManager workspaceManager();

    default Optional<Workspace> workspace(){
        final Path path = getPushMixin().path();
        final WorkspaceManager workspaceManager = workspaceManager();
        // This should only really concern Integration tests.
        // Workspace param is hidden we only use for testing in the Push commands
        final File workspace = getPushMixin().workspace;

        //If No explicit workspace is provided, we rely on the path to find the workspace
       return workspaceManager.findWorkspace( workspace != null ? workspace.toPath() : path);
    }

    default Path workingRootDir() {
        final Optional<Workspace> workspace = workspace();
        if (workspace.isPresent()) {
            return workspace.get().root();
        }
        throw new IllegalArgumentException("No valid workspace found.");
    }

}