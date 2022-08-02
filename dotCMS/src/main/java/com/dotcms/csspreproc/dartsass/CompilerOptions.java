package com.dotcms.csspreproc.dartsass;

import com.dotmarketing.util.Config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jose Castro
 * @since Jul 25th, 2022
 */
public class CompilerOptions implements Serializable {

    private final boolean verbose;
    private final boolean expandedCss;
    private final boolean errorInCss;
    private final boolean stopOnError;
    private final boolean deprecationWarnings;
    private final boolean deprecationWarningsFromDependencies;

    private static final String VERBOSE = "dartsass.compiler.verbose";
    private static final String ENABLE_EXPANDED_CSS = "dartsass.compiler.expanded.css";
    private static final String ERROR_IN_CSS = "dartsass.compiler.error.in.css";
    private static final String STOP_ON_ERROR = "dartsass.compiler.stop.on.error";
    private static final String DEPRECATION_WARNINGS = "dartsass.compiler.deprecation.warnings";
    private static final String DEPRECATION_WARNINGS_FROM_DEPENDENCIES = "dartsass.compiler.deprecation.warnings.from.dependencies";
    private final List<String> defaultCommands = List.of("--no-color", "--no-source-map");

    /**
     *
     * @param builder
     */
    private CompilerOptions(final Builder builder) {
        this.verbose = builder.verbose;
        this.expandedCss = builder.expandedCss;
        this.errorInCss = builder.errorInCss;
        this.stopOnError = builder.stopOnError;
        this.deprecationWarnings = builder.deprecationWarnings;
        this.deprecationWarningsFromDependencies = builder.deprecationWarningsFromDependencies;
    }

    /**
     *
     * @return
     */
    public boolean verbose() {
        return this.verbose;
    }

    /**
     *
     * @return
     */
    public boolean expandedCss() {
        return this.expandedCss;
    }

    /**
     *
     * @return
     */
    public boolean errorInCss() {
        return this.errorInCss;
    }

    /**
     *
     * @return
     */
    public boolean stopOnError() {
        return this.stopOnError;
    }

    /**
     *
     * @return
     */
    public boolean deprecationWarnings() {
        return this.deprecationWarnings;
    }

    /**
     *
     * @return
     */
    public boolean deprecationWarningsFromDependencies() {
        return this.deprecationWarningsFromDependencies;
    }

    /**
     *
     * @return
     */
    public List<String> generate() {
        final List<String> commands = new ArrayList<>();
        commands.addAll(defaultCommands);
        if (this.verbose()) {
            commands.add(SassCommands.VERBOSE.enable());
        }
        if (this.expandedCss()) {
            commands.add(SassCommands.EXPANDED_CSS.enable());
        } else {
            commands.add(SassCommands.EXPANDED_CSS.disable());
        }
        if (this.errorInCss()) {
            commands.add(SassCommands.ERROR_IN_CSS.enable());
        } else  {
            commands.add(SassCommands.ERROR_IN_CSS.disable());
        }
        if (this.stopOnError()) {
            commands.add(SassCommands.STOP_ON_ERROR.enable());
        }
        if (!this.deprecationWarnings()) {
            commands.add(SassCommands.DEPRECATION_WARNINGS.disable());
        }
        if (!this.deprecationWarningsFromDependencies()) {
            commands.add(SassCommands.DEPRECATION_WARNINGS_FROM_DEPENDENCIES.disable());
        }
        return commands;
    }

    @Override
    public String toString() {
        return "CompilerOptions{" + "verbose=" + verbose + ", expandedCss=" + expandedCss + ", errorInCss="
                       + errorInCss + ", stopOnError=" + stopOnError + ", deprecationWarnings=" + deprecationWarnings
                       + ", deprecationWarningsFromDependencies=" + deprecationWarningsFromDependencies
                       + ", defaultCommands=" + defaultCommands + '}';
    }

    /**
     *
     */
    private enum SassCommands {

        VERBOSE("--verbose", ""),
        EXPANDED_CSS("--style=expanded", "--style=compressed"),
        ERROR_IN_CSS("--error-css", "--no-error-css"),
        STOP_ON_ERROR("--stop-on-error", ""),
        DEPRECATION_WARNINGS("", "--quiet"),
        DEPRECATION_WARNINGS_FROM_DEPENDENCIES("", "--quiet-deps");

        private final String enabledCommand;
        private final String disabledCommand;

        /**
         *
         * @param enabledCommand
         * @param disabledCommand
         */
        SassCommands(final String enabledCommand, final String disabledCommand) {
            this.enabledCommand = enabledCommand;
            this.disabledCommand = disabledCommand;
        }

        /**
         *
         * @return
         */
        public String enable() {
            return this.enabledCommand;
        }

        /**
         *
         * @return
         */
        public String disable() {
            return this.disabledCommand;
        }

    }

    /**
     *
     */
    public static final class Builder {

        private boolean verbose = Config.getBooleanProperty(VERBOSE, Boolean.FALSE);
        private boolean expandedCss = Config.getBooleanProperty(ENABLE_EXPANDED_CSS, Boolean.TRUE);
        private boolean errorInCss = Config.getBooleanProperty(ERROR_IN_CSS, Boolean.TRUE);
        private boolean stopOnError = Config.getBooleanProperty(STOP_ON_ERROR, Boolean.TRUE);
        private boolean deprecationWarnings = Config.getBooleanProperty(DEPRECATION_WARNINGS, Boolean.FALSE);
        private boolean deprecationWarningsFromDependencies =
                Config.getBooleanProperty(DEPRECATION_WARNINGS_FROM_DEPENDENCIES, Boolean.FALSE);

        /**
         *
         */
        public Builder() {
        }

        public Builder verbose(final boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder expandedCss(final boolean expandedCss) {
            this.expandedCss = expandedCss;
            return this;
        }

        public Builder errorInCss(final boolean errorInCss) {
            this.errorInCss = errorInCss;
            return this;
        }

        public Builder stopOnError(final boolean stopOnError) {
            this.stopOnError = stopOnError;
            return this;
        }

        public Builder deprecationWarnings(final boolean deprecationWarnings) {
            this.deprecationWarnings = deprecationWarnings;
            return this;
        }

        public Builder deprecationWarningsFromDependencies(final boolean deprecationWarningsFromDependencies) {
            this.deprecationWarningsFromDependencies = deprecationWarningsFromDependencies;
            return this;
        }

        public CompilerOptions build() {
            return new CompilerOptions(this);
        }

    }

}
