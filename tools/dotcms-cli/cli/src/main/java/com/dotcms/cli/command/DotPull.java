package com.dotcms.cli.command;

import com.dotcms.cli.common.PullMixin;
import java.util.Optional;

/**
 * Describes a command that has a {@link PullMixin} and can provide the name of its custom mixin.
 * This interface is intended to be implemented by classes that represents specific pull commands
 * and require common pull behaviors and characteristics encapsulated in {@link PullMixin}.
 * Furthermore, it can provide the name of a custom mixin it uses, which is useful for custom
 * command line configurations where specific logic may be associated with certain mixin names.
 */
public interface DotPull extends DotCommand {


    /**
     * Returns the {@link PullMixin} associated with the implementing class. This {@link PullMixin}
     * object encapsulates the behavior, options, and parameters that are common and essential for a
     * pull command in the CLI.
     *
     * @return the {@link PullMixin} associated with this pull command
     */
    PullMixin getPullMixin();

    /**
     * Returns the name of the custom mixin associated with the implementing class. The mixin's name
     * is used to identify the mixin in custom configurations or logic where a specific behavior or
     * setup is required for that mixin.
     *
     * @return the name of the custom mixin associated with this pull command
     */
    default Optional<String> getCustomMixinName() {
        return Optional.empty();
    }

    /**
     * Returns the execution order of this pull command.
     *
     * @return the execution order of this pull command
     */
    default int getOrder() {
        return Integer.MAX_VALUE;  // default to the highest possible value
    }

}
