package com.dotcms.csspreproc.dartsass;

import com.dotmarketing.util.Config;
import io.vavr.Lazy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class allows you to enable/disable different configuration parameters that define the behavior of the Dart SASS
 * Compiler. If no parameters are changed upon instantiation, the default dotCMS-specific configuration will be used
 * instead. Here's the list of parameters that can be activated for the compiler, which can be found in the {@code
 * dotmarketing-config.properties} file:
 * <ul>
 *     <li>{@code Verbose}: ({@code false} by default) Prints all deprecation warnings even when they're repetitive
 *     .</li>
 *     <li>{@code Expanded CSS}: ({@code true} by default) Controls the output style of the resulting CSS. Dart Sass
 *     supports two output styles: {@code expanded} (the default) writes each selector and declaration on its own
 *     line. And {@code compressed} removes as many extra characters as possible, and writes the entire stylesheet on
 *     a single line.</li>
 *     <li>{@code Error in CSS}: ({@code true} by default) This flag tells Sass whether to emit a CSS file when an
 *     error occurs during compilation. This CSS file describes the error in a comment and in the "content" property
 *     of "body::before", so that you can see the error message in the browser without needing to switch back to the
 *     terminal.</li>
 *     <li>{@code Stop on Error}: ({@code true} by default) This flag tells Sass to stop compiling immediately when
 *     an error is detected, rather than trying to compile other Sass files that may not contain errors.</li>
 *     <li>{@code Deprecation Warnings}: ({@code false} by default) This flag tells Sass to emit any warnings when
 *     compiling. By default, Sass emits warnings when deprecated features are used or when the {@code @warn} rule is
 *     encountered.</li>
 *     <li>{@code Deprecation Warnings From Dependencies}: ({@code false} by default) This flag tells Sass not to
 *     emit deprecation warnings that come from dependencies. It considers any file that’s transitively imported
 *     through a load path to be a "dependency".</li>
 * </ul>
 *
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

    /**
     * Contains the list of commands that are either not required, or only useful when calling the compiler directly
     * from a Terminal.
     */
    private static final List<String> DEFAULT_COMMANDS = List.of("--no-color", "--no-source-map");

    /**
     * Private builder-based class constructor.
     *
     * @param builder The {@link Builder} object containing the specified compilation parameters.
     */
    private CompilerOptions(final Builder builder) {
        this.verbose = builder.verbose.get();
        this.expandedCss = builder.expandedCss.get();
        this.errorInCss = builder.errorInCss.get();
        this.stopOnError = builder.stopOnError.get();
        this.deprecationWarnings = builder.deprecationWarnings.get();
        this.deprecationWarningsFromDependencies = builder.deprecationWarningsFromDependencies.get();
    }

    /**
     * Returns the value of the "verbose" flag.
     *
     * @return Returns {@code true} if enabled.
     */
    public boolean verbose() {
        return this.verbose;
    }

    /**
     * Returns the value of the "Expanded CSS" flag.
     *
     * @return Returns {@code true} if enabled.
     */
    public boolean expandedCss() {
        return this.expandedCss;
    }

    /**
     * Returns the value of the "Error in CSS" flag.
     *
     * @return Returns {@code true} if enabled.
     */
    public boolean errorInCss() {
        return this.errorInCss;
    }

    /**
     * Returns the value of the "Stop on Error" flag.
     *
     * @return Returns {@code true} if enabled.
     */
    public boolean stopOnError() {
        return this.stopOnError;
    }

    /**
     * Returns the value of the "Deprecation Warnings" flag.
     *
     * @return Returns {@code true} if enabled.
     */
    public boolean deprecationWarnings() {
        return this.deprecationWarnings;
    }

    /**
     * Returns the value of the "Deprecation Warnings From Dependencies" flag.
     *
     * @return Returns {@code true} if enabled.
     */
    public boolean deprecationWarningsFromDependencies() {
        return this.deprecationWarningsFromDependencies;
    }

    /**
     * Generates a list with the Dart SASS configuration parameters that were specified when an instance of this class
     * was created.
     *
     * @return A String list containing the expected configuration parameters for the compiler.
     */
    public List<String> generate() {
        final List<String> commands = new ArrayList<>(DEFAULT_COMMANDS);
        if (this.verbose()) {
            commands.add(SassCommands.VERBOSE.enable());
        }
        commands.add(this.expandedCss() ? SassCommands.EXPANDED_CSS.enable() : SassCommands.EXPANDED_CSS.disable());
        commands.add(this.errorInCss() ? SassCommands.ERROR_IN_CSS.enable() : SassCommands.ERROR_IN_CSS.disable());
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
                       + ", defaultCommands=" + DEFAULT_COMMANDS + '}';
    }

    /**
     * Contains the set of allowed Dart SASS Compiler commands that developers can set up in order to change the
     * behavior of the compiler. It's worth noting that some commands require a specific command to disable them, while
     * other don't.
     */
    private enum SassCommands {

        VERBOSE("--verbose", "", "dartsass.compiler.verbose"),
        EXPANDED_CSS("--style=expanded", "--style=compressed", "dartsass.compiler.expanded.css"),
        ERROR_IN_CSS("--error-css", "--no-error-css", "dartsass.compiler.error.in.css"),
        STOP_ON_ERROR("--stop-on-error", "", "dartsass.compiler.stop.on.error"),
        DEPRECATION_WARNINGS("", "--quiet", "dartsass.compiler.deprecation.warnings"),
        DEPRECATION_WARNINGS_FROM_DEPENDENCIES("", "--quiet-deps", "dartsass.compiler.deprecation.warnings.from.dependencies");

        private final String enabledCommand;
        private final String disabledCommand;
        private final String attributeKey;

        /**
         * Specifies the appropriate command used to enable and disable this specific property for the Dart SASS
         * Compiler.
         *
         * @param enabledCommand  The command to enable this configuration parameter -- if required.
         * @param disabledCommand The command to enable this configuration parameter -- if required.
         * @param attributeKey    The key that allows you to customize the value of this compile parameter via the
         * {@code dotmarketing-config.properties} file.
         */
        SassCommands(final String enabledCommand, final String disabledCommand, final String attributeKey) {
            this.enabledCommand = enabledCommand;
            this.disabledCommand = disabledCommand;
            this.attributeKey = attributeKey;
        }

        /**
         * Returns the appropriate command to enable this configuration parameter.
         *
         * @return The CLI command.
         */
        public String enable() {
            return this.enabledCommand;
        }

        /**
         * Returns the appropriate command to disable this configuration parameter.
         *
         * @return The CLI command.
         */
        public String disable() {
            return this.disabledCommand;
        }

        /**
         * Returns the parameter key for retrieving this property's value from the {@code dotmarketing-config
         * .properties} file.
         *
         * @return The attribute key.
         */
        public String key() {
            return this.attributeKey;
        }

    }

    /**
     * Allows you to specify the available configuration parameters for executing the Dart SASS Compiler inside dotCMS.
     */
    public static final class Builder {

        private Lazy<Boolean> verbose = Lazy.of(() -> Config.getBooleanProperty(SassCommands.VERBOSE.key(),
                Boolean.FALSE));
        private Lazy<Boolean> expandedCss = Lazy.of(() -> Config.getBooleanProperty(SassCommands.EXPANDED_CSS.key(),
                Boolean.TRUE));
        private Lazy<Boolean> errorInCss = Lazy.of(() -> Config.getBooleanProperty(SassCommands.ERROR_IN_CSS.key(),
                Boolean.TRUE));
        private Lazy<Boolean> stopOnError = Lazy.of(() -> Config.getBooleanProperty(SassCommands.STOP_ON_ERROR.key(),
                Boolean.TRUE));
        private Lazy<Boolean> deprecationWarnings =
                Lazy.of(() -> Config.getBooleanProperty(SassCommands.DEPRECATION_WARNINGS.key(), Boolean.FALSE));
        private Lazy<Boolean> deprecationWarningsFromDependencies =
                Lazy.of(() -> Config.getBooleanProperty(SassCommands.DEPRECATION_WARNINGS_FROM_DEPENDENCIES.key(),
                        Boolean.FALSE));

        /**
         * Default class constructor.
         */
        public Builder() {
        }

        /**
         * This flag prints all deprecation warnings even when they're repetitive.
         *
         * @param verbose Set to {@code true} to enable this parameter. Otherwise, set to false.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder verbose(final boolean verbose) {
            this.verbose = Lazy.of(() -> verbose);
            return this;
        }

        /**
         * This flag controls the output style of the resulting CSS. Dart Sass supports two output styles: {@code
         * expanded} (the default) writes each selector and declaration on its own line. And {@code compressed} removes
         * as many extra characters as possible, and writes the entire stylesheet on a single line.
         *
         * @param expandedCss Set to {@code true} to enable this parameter. Otherwise, set to false.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder expandedCss(final boolean expandedCss) {
            this.expandedCss = Lazy.of(() -> expandedCss);
            return this;
        }

        /**
         * This flag tells Sass whether to emit a CSS file when an error occurs during compilation. This CSS file
         * describes the error in a comment and in the "content" property of "body::before", so that you can see the
         * error message in the browser without needing to switch back to the terminal.
         *
         * @param errorInCss Set to {@code true} to enable this parameter. Otherwise, set to false.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder errorInCss(final boolean errorInCss) {
            this.errorInCss = Lazy.of(() -> errorInCss);
            return this;
        }

        /**
         * This flag tells Sass to stop compiling immediately when an error is detected, rather than trying to
         * compile other Sass files that may not contain errors.
         *
         * @param stopOnError Set to {@code true} to enable this parameter. Otherwise, set to false.*
         *
         * @return The current {@link Builder} instance.
         */
        public Builder stopOnError(final boolean stopOnError) {
            this.stopOnError = Lazy.of(() -> stopOnError);
            return this;
        }

        /**
         * This flag tells Sass to emit any warnings when compiling. By default, Sass emits warnings when deprecated
         * features are used or when the {@code @warn} rule is encountered.
         *
         * @param deprecationWarnings Set to {@code true} to enable this parameter. Otherwise, set to false.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder deprecationWarnings(final boolean deprecationWarnings) {
            this.deprecationWarnings = Lazy.of(() -> deprecationWarnings);
            return this;
        }

        /**
         * This flag tells Sass not to emit deprecation warnings that come from dependencies. It considers any file
         * that’s transitively imported through a load path to be a "dependency".
         *
         * @param deprecationWarningsFromDependencies Set to {@code true} to enable this parameter. Otherwise, set to
         *                                            false.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder deprecationWarningsFromDependencies(final boolean deprecationWarningsFromDependencies) {
            this.deprecationWarningsFromDependencies = Lazy.of(() -> deprecationWarningsFromDependencies);
            return this;
        }

        /**
         * Creates an instance of the {@link CompilerOptions} class with the specified or default configuration
         * parameters for the Dart SASS Compiler.
         *
         * @return The {@link CompilerOptions} object.
         */
        public CompilerOptions build() {
            return new CompilerOptions(this);
        }

    }

}