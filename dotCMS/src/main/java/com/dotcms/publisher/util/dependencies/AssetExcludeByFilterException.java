package com.dotcms.publisher.util.dependencies;

/**
 * Throw when try to add a Asset into a bundle but it was exclude by {@link com.dotcms.publishing.FilterDescriptor}
 */
public class AssetExcludeByFilterException extends AssetExcludeException {
    AssetExcludeByFilterException(String message) {
        super(message);
    }
}
