package com.dotcms.publisher.util.dependencies;

public interface DependencyAssetProcessor<T> {
    void process(T asset);
}
