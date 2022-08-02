package com.dotcms.csspreproc.dartsass;

import com.dotmarketing.util.*;
import io.vavr.Lazy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Jose Castro
 * @since Jul 26th, 2022
 */
public class DartSassCompiler {

    private final Lazy<String> DART_SASS_LOCATION = Lazy.of(
            () -> Config.getStringProperty("dartsass.compiler.expanded.location", "./WEB-INF/dart-sass/sass"));

    private File inputFile;
    private File outputFile;
    private final CompilerOptions compilerOptions;
    private RuntimeUtils.TerminalOutput terminalOutput;

    /**
     *
     */
    public DartSassCompiler() {
        this(new CompilerOptions.Builder().build(), null, null);
    }

    /**
     *
     * @param compilerOptions
     */
    public DartSassCompiler(final CompilerOptions compilerOptions) {
        this(compilerOptions, null, null);
    }

    public DartSassCompiler(final File inputFile, final File outputFile) {
        this(new CompilerOptions.Builder().build(), inputFile, outputFile);
    }

    public DartSassCompiler(final CompilerOptions compilerOptions, final File inputFile, final File outputFile) {
        this.compilerOptions = compilerOptions;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    /**
     *
     * @return
     */
    public File inputFile() {
        return this.inputFile;
    }

    /**
     *
     * @param inputFile
     */
    public void inputFile(final File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     *
     * @return
     */
    public File outputFile() {
        return this.outputFile;
    }

    /**
     *
     * @param outputFile
     */
    public void outputFile(final File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     *
     * @return
     */
    public CompilerOptions compilerOptions() {
        return this.compilerOptions;
    }

    public RuntimeUtils.TerminalOutput terminalOutput() {
        return this.terminalOutput;
    }

    /**
     *
     * @return
     * @throws DotSassCompilerException
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
     *
     * @return
     */
    public boolean isSuccessful() {
        return this.terminalOutput().exitValue() == 0 && UtilMethods.isSet(this.terminalOutput());
    }

    /**
     *
     * @return
     */
    private String[] buildCompilationCommand() {
        final List<String> compileInstructions = new ArrayList<>();
        compileInstructions.add(DART_SASS_LOCATION.get());
        compileInstructions.addAll(this.compilerOptions().generate());
        compileInstructions.add(this.inputFile().getAbsolutePath());
        compileInstructions.add(this.outputFile().getAbsolutePath());
        return compileInstructions.toArray(String[]::new);
    }

    /**
     * Validate
     */
    private void validateInputData() {
        if (null == this.inputFile() || !this.inputFile().exists()) {
            throw new IllegalArgumentException("Input SCSS file is null or doesn't exist");
        }
        if (null == this.outputFile()) {
            throw new IllegalArgumentException("Output SCSS file is not set");
        }
        if (UtilMethods.isNotSet(this.DART_SASS_LOCATION.get())) {
            throw new IllegalArgumentException("Dart SASS compiler is not set correctly");
        }
    }

    private String getOsBasedLibLocation() {
        return "";
    }

}
