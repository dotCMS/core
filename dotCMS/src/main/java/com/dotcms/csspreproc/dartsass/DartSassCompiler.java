package com.dotcms.csspreproc.dartsass;

import com.dotcms.csspreproc.dartsass.strategy.SassCommandStrategy;
import com.dotcms.csspreproc.dartsass.strategy.SassCommandStrategyResolver;
import com.dotmarketing.util.*;
import com.liferay.util.StringPool;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides Dart SASS Compilation capabilities in dotCMS. This implementation shells out and calls the appropriate Dart
 * SASS binary under the covers. Dart Sass is the primary implementation of Sass, which means it gets new features
 * before any other implementation. It's very fast and easy to deploy.
 * <p>It's worth noting that the Dart SASS binaries used to compile SCSS files are specific to your current Operating
 * System and architecture. In order to make things easier to handle and configure, the
 * {@link SassCommandStrategyResolver} allows you to specify the way the compiler command must be assembled for a
 * specific environment.</p>
 *
 * @author Jose Castro
 * @since Jul 26th, 2022
 */
public class DartSassCompiler {

    private final SassCommandStrategyResolver commandStrategyResolver = SassCommandStrategyResolver.getInstance();
    private File inputFile;
    private File outputFile;
    private final CompilerOptions compilerOptions;
    private RuntimeUtils.TerminalOutput terminalOutput;

    /**
     * Creates an instance of the dotCMS Dart SASS Compiler with the default compilation parameters.
     */
    public DartSassCompiler() {
        this(new CompilerOptions.Builder().build(), null, null);
    }

    /**
     * Creates an instance of the dotCMS Dart SASS Compiler with the specified compilation parameters.
     *
     * @param compilerOptions The {@link CompilerOptions} object with the expected compilation parameters.
     */
    public DartSassCompiler(final CompilerOptions compilerOptions) {
        this(compilerOptions, null, null);
    }

    /**
     * Creates an instance of the dotCMS Dart SASS Compiler with the default compilation parameters, the input SCSS
     * File and the resulting CSS File.
     *
     * @param inputFile  The SASS File that will be compiled into CSS code.
     * @param outputFile The resulting CSS File.
     */
    public DartSassCompiler(final File inputFile, final File outputFile) {
        this(new CompilerOptions.Builder().build(), inputFile, outputFile);
    }

    /**
     * Creates an instance of the dotCMS Dart SASS Compiler with the specified compilation parameters, the input SCSS
     * File and the resulting CSS File.
     *
     * @param compilerOptions The {@link CompilerOptions} object with the expected compilation parameters.
     * @param inputFile       The SASS File that will be compiled into CSS code.
     * @param outputFile      The resulting CSS File.
     */
    public DartSassCompiler(final CompilerOptions compilerOptions, final File inputFile, final File outputFile) {
        this.compilerOptions = compilerOptions;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    /**
     * Returns the SCSS File that is being compiled.
     *
     * @return The SASS File.
     */
    public File inputFile() {
        return this.inputFile;
    }

    /**
     * Sets the SCSS File that will be compiled.
     *
     * @param inputFile The SASS Fie.
     */
    public void inputFile(final File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Returns the resulting CSS File. Keep in mind that this file may not exist yet until the compilation process has
     * run and finished correctly.
     *
     * @return The resulting CSS File.
     */
    public File outputFile() {
        return this.outputFile;
    }

    /**
     * Sets the location of the resulting CSS File.
     *
     * @param outputFile The location of the resulting CSS File.
     */
    public void outputFile(final File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Returns the object containing the specified Dart SASS Compiler parameters for this instance.
     *
     * @return The {@link CompilerOptions} that have been set for the compiler.
     */
    public CompilerOptions compilerOptions() {
        return this.compilerOptions;
    }

    /**
     * The result obtained after the Dart SASS compiler has finished running. Given that this compiler runs from the
     * CLI, both the terminal output and exit value are retrieved in order to provide developers as much information on
     * the result of the compilation as possible.
     *
     * @return The {@link RuntimeUtils.TerminalOutput} object containing information on the final status of the
     * execution of the command.
     */
    public RuntimeUtils.TerminalOutput terminalOutput() {
        return this.terminalOutput;
    }

    /**
     * Compiles the incoming SCSS File and produces the expected CSS File.
     * <p>After validating the involved CSS Files and the location of the Dart SASS library, the final compilation
     * command is put together based on the specified parameters. Executing it via the Java API will result in the
     * expected CSS File that will be returned to the browser.</p>
     *
     * @return An {@link Optional} object containing the results of the compilation.
     *
     * @throws DotSassCompilerException An error occurred when compiling the SCSS File.
     */
    public Optional<String> compile() throws DotSassCompilerException {
        validateInputData();
        final String[] compileInstructions = buildCompilationCommand();
        this.terminalOutput = RuntimeUtils.runProcessAndGetOutputWithError(compileInstructions);
        try {
            return FileUtil.read(this.outputFile());
        } catch (final IOException e) {
            final String errorMsg = String.format("An error occurred when reading resulting CSS file '%s': %s",
                    this.outputFile().getAbsolutePath(), e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotSassCompilerException(errorMsg, e);
        }
    }

    /**
     * Generates the CLI call to the Dart SASS compiler based on the location of the library in dotCMS, and the
     * specified configuration parameters that define the behavior of the compiler.
     *
     * @return An array containing the parts that make up the compile command.
     */
    private String[] buildCompilationCommand() {
        final List<String> compileInstructions = new ArrayList<>();
        final String location = this.commandStrategyResolver.getStrategy().get().apply();
        compileInstructions.add(location);
        compileInstructions.addAll(this.compilerOptions().generate());
        compileInstructions.add(this.inputFile().getAbsolutePath());
        compileInstructions.add(this.outputFile().getAbsolutePath());
        return compileInstructions.toArray(String[]::new);
    }

    /**
     * Utility method used to validate that the information required by the compiler is available and valid right before
     * carrying out the compilation process.
     *
     * @throws DotSassCompilerException An error occurred when validating required compiler input data.
     */
    private void validateInputData() throws DotSassCompilerException {
        if (null == this.inputFile() || !this.inputFile().exists() || !this.inputFile.canRead()) {
            throw new DotSassCompilerException("Input SCSS file is null, doesn't exist or cannot be read");
        }
        if (null == this.outputFile()) {
            throw new DotSassCompilerException("Output SCSS file is not specified");
        }
        final Optional<SassCommandStrategy> sassCommandStrategyOpt = this.commandStrategyResolver.getStrategy();
        if (sassCommandStrategyOpt.isEmpty()) {
            throw new DotSassCompilerException("Current server OS and/or arch is not compatible with the provided " +
                                                       "Dart SASS libraries.");
        }
        if (invalidPathFound(sassCommandStrategyOpt.get().apply())) {
            throw new DotSassCompilerException("Dart SASS compiler location is incorrect or invalid");
        }
    }

    /**
     * Verifies that the specified location to the Dart SASS library does not try to access upper level folders or
     * unexpected harmful commands, and is not empty.
     *
     * @param path Path to the parent folder of the Dart SASS compiler.
     *
     * @return If the folder path is valid, returns {@code true}.
     */
    private boolean invalidPathFound(final String path) {
        if (UtilMethods.isNotSet(path)) {
            return Boolean.FALSE;
        }
        final String sanitizedPath = path.replaceAll("\\.{2}|[&;|]+", StringPool.BLANK);
        return !path.equals(sanitizedPath) ? Boolean.TRUE : Boolean.FALSE;
    }

}
