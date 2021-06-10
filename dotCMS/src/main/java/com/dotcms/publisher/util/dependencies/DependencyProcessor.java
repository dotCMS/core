package com.dotcms.publisher.util.dependencies;

import com.dotcms.publisher.util.PusheableAsset;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 *  it allow process a set of Assets to calculate its dependency
 */
public interface DependencyProcessor {

    /**
     * Add a asset to process its dependencies,
     *
     * @param asset
     * @param pusheableAsset
     *
     */
    void addAsset(final Object asset, final PusheableAsset pusheableAsset);

    /**
     * The current thread wait until all the dependencies are processed
     * @throws ExecutionException
     */
    void waitUntilResolveAllDependencies() throws ExecutionException;
}
