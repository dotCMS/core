package com.dotcms.cli.command;

import java.util.Optional;

public interface CustomMixinNamed {

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

}
