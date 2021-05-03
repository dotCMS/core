package com.dotcms.publisher.util.dependencies;

import com.dotcms.publisher.util.PusheableAsset;
import java.util.concurrent.ExecutionException;

public interface DependencyProcessor {
    void addAsset(final Object asset, final PusheableAsset pusheableAsset);
    void waitUntilResolveAllDependencies() throws ExecutionException;
}
