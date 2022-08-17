package com.dotcms.csspreproc.dartsass.config;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RuntimeUtils;
import com.google.common.collect.ImmutableList;
import io.vavr.Lazy;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This Strategy Resolver determines how the Dart SASS Compiler command must be assembled based on the current Operating
 * System and architecture, e.g, ARM64, AMD64, X86_64, etc. This is very important because the SASS compiler binaries
 * that dotCMS uses to compile SCSS files depend on the current environment.
 *
 * @author Jose Castro
 * @since Aug 16th, 2022
 */
public class SassCommandStrategyResolver {

    private static final Lazy<String> DART_SASS_LOCATION = Lazy.of(
            () -> Config.getStringProperty("dartsass.compiler.location",
                    System.getProperty("catalina.base") + "/webapps/ROOT/WEB-INF/bin/"));

    private volatile Optional<SassCommandStrategy> selected = Optional.empty();
    private volatile List<SassCommandStrategy> strategies = this.getDefaultStrategies();

    /**
     * Singleton holder class based on initialization on demand.
     */
    private static class SingletonHolder {

        private static final SassCommandStrategyResolver INSTANCE = new SassCommandStrategyResolver();

    }

    /**
     * Returns the singleton instance of this Strategy Resolver.
     *
     * @return The current {@link SassCommandStrategyResolver} instance.
     */
    public static SassCommandStrategyResolver getInstance() {
        return SassCommandStrategyResolver.SingletonHolder.INSTANCE;
    }

    /**
     * Returns the current list of default SASS Command Strategy Resolvers.
     *
     * @return The list of {@link SassCommandStrategy} objects.
     */
    private List<SassCommandStrategy> getDefaultStrategies() {
        final ImmutableList.Builder<SassCommandStrategy> builder = new ImmutableList.Builder<>();
        builder.add(new MacOSSassCommandStrategyImpl());
        builder.add(new LinuxSassCommandStrategyImpl());
        return builder.build();
    }

    /**
     * Adds new SASS Command Strategy to the current list, in case a new Operating System and or architecture must be
     * supported.
     *
     * @param strategy The new {@link SassCommandStrategy}.
     */
    public synchronized void subscribe(final SassCommandStrategy strategy) {
        if (null != strategy) {
            final ImmutableList.Builder<SassCommandStrategy> builder = new ImmutableList.Builder<>();
            builder.addAll(this.strategies);
            builder.add(strategy);
            this.strategies = builder.build();
        }
    }

    /**
     * Returns an Optional with the appropriate SASS Command Strategy based on the current environment. Considering this
     * is an automatic process, there's no need for parameters or any other user input for dotCMS to correctly determine
     * what specific command must be used for your current Operating System and architecture.
     *
     * @return The appropriate {@link SassCommandStrategy} wrapped into an Optional.
     */
    public Optional<SassCommandStrategy> getStrategy() {
        if (this.selected.isPresent()) {
            return this.selected;
        }
        for (final SassCommandStrategy strategy : this.strategies) {
            if (strategy.test()) {
                this.selected = Optional.of(strategy);
                return this.selected;
            }
        }
        return Optional.empty();
    }

    /**
     * Assembles the appropriate Dart SASS compiler command for Mac OS X environments and their respective architectures.
     */
    private class MacOSSassCommandStrategyImpl implements SassCommandStrategy {

        private final Map<String, String> DART_SASS_CMD = Map.of(
                RuntimeUtils.ARM64_ARCH, "dart-sass-macos-arm64/sass",
                RuntimeUtils.X86_64_ARCH, "dart-sass-macos-x64/sass");

        @Override
        public boolean test() {
            if (SystemUtils.IS_OS_MAC) {
                if (SystemUtils.OS_ARCH.equalsIgnoreCase(RuntimeUtils.ARM64_ARCH) || SystemUtils.OS_ARCH.equalsIgnoreCase(RuntimeUtils.X86_64_ARCH)) {
                    return Boolean.TRUE;
                } else {
                    Logger.error(this, String.format("Dart SASS lib for Mac OS is not available for the specified " +
                                                             "arch '%s'", SystemUtils.OS_ARCH));
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public String apply() {
            final String location = DART_SASS_LOCATION.get().endsWith(File.separator) ? DART_SASS_LOCATION.get() :
                                            DART_SASS_LOCATION.get() + File.separator;
            return location + this.DART_SASS_CMD.get(SystemUtils.OS_ARCH);
        }
    }

    /**
     * Assembles the appropriate Dart SASS compiler command for Linux environments and their respective architectures.
     */
    private class LinuxSassCommandStrategyImpl implements SassCommandStrategy {

        private final Map<String, String> DART_SASS_CMD = Map.of(
                RuntimeUtils.ARM64_ARCH, "dart-sass-linux-arm64/sass",
                RuntimeUtils.AMD64_ARCH, "dart-sass-linux-x64/sass");

        @Override
        public boolean test() {
            if (SystemUtils.IS_OS_LINUX) {
                if (SystemUtils.OS_ARCH.equalsIgnoreCase(RuntimeUtils.ARM64_ARCH) || SystemUtils.OS_ARCH.equalsIgnoreCase(RuntimeUtils.AMD64_ARCH)) {
                    return Boolean.TRUE;
                } else {
                    Logger.error(this, String.format("Dart SASS lib for Linux is not available for the specified arch" +
                                                             " '%s'", SystemUtils.OS_ARCH));
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public String apply() {
            final String location = DART_SASS_LOCATION.get().endsWith(File.separator) ? DART_SASS_LOCATION.get() :
                                            DART_SASS_LOCATION.get() + File.separator;
            return location + this.DART_SASS_CMD.get(SystemUtils.OS_ARCH);
        }
    }

}
