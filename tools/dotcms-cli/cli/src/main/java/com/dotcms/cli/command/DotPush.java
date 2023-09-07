package com.dotcms.cli.command;

import com.dotcms.cli.common.PushMixin;
import java.util.Optional;

/**
 * Describes a command that has a {@link PushMixin} and can provide the name of its custom mixin.
 * This interface is intended to be implemented by classes that represents specific push commands
 * and require common push behaviors and characteristics encapsulated in {@link PushMixin}.
 * Furthermore, it can provide the name of a custom mixin it uses, which is useful for custom
 * command line configurations where specific logic may be associated with certain mixin names.
 */
public interface DotPush {

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

}