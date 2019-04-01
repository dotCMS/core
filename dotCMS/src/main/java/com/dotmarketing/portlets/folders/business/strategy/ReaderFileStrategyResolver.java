package com.dotmarketing.portlets.folders.business.strategy;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

/**
 * Subscribe Strategies and get the strategy for a set of arguments if applies for a ReaderFileStrategy
 * @author jsanca
 */
public class ReaderFileStrategyResolver {

    private static class SingletonHolder {
        private static final ReaderFileStrategyResolver INSTANCE = new ReaderFileStrategyResolver();
    }
    /**
     * Get the instance.
     * @return ReaderFileStrategyResolver
     */
    public static ReaderFileStrategyResolver getInstance() {

        return ReaderFileStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.

    private volatile ReaderFileStrategy defaultOne = null;
    private volatile List<ReaderFileStrategy> strategies = this.getDefaultStrategies();

    private List<ReaderFileStrategy> getDefaultStrategies() {

        final ImmutableList.Builder<ReaderFileStrategy> builder =
                new ImmutableList.Builder<>();

        final SiteBrowserReaderFileStrategyImpl siteBrowserStrategy = new SiteBrowserReaderFileStrategyImpl();
        final FileSystemReaderFileStrategyImpl fileSystemStrategy  = new FileSystemReaderFileStrategyImpl();

        builder.add(siteBrowserStrategy);
        builder.add(fileSystemStrategy);

        this.defaultOne = siteBrowserStrategy;

        return builder.build();
    }

    public ReaderFileStrategy getDefaultStrategy () {

        return defaultOne;
    }

    public synchronized void setDefaultStrategy (final ReaderFileStrategy strategy) {

        if (null != strategy) {

            this.defaultOne = strategy;
        }
    }

    /**
     * Adds a new strategy
     * @param strategy {@link ReaderFileStrategy}
     */
    public synchronized void subscribe (final ReaderFileStrategy strategy) {

        if (null != strategy) {

            final ImmutableList.Builder<ReaderFileStrategy> builder =
                    new ImmutableList.Builder<>();

            builder.addAll(this.strategies);
            builder.add(strategy);

            this.strategies = builder.build();
        }
    }

    /**
     * Get a strategy if applies
     * @param file {@link String}
     * @return Optional ReaderFileStrategy
     */
    public final Optional<ReaderFileStrategy> get(final String file) {

        for (int i = 0; i < this.strategies.size(); ++i) {

            final ReaderFileStrategy strategy = this.strategies.get(i);
            if (strategy.test(file)) {

                return Optional.of(strategy);
            }
        }

        return Optional.empty();
    }
}
