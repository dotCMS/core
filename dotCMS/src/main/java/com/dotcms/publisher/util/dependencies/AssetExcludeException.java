package com.dotcms.publisher.util.dependencies;

/**
 * Throw when try to add a Asset into a bundle but it was exclude for any reason.
 */
public class AssetExcludeException extends Exception {
    AssetExcludeException(String message) {
        super(message);
    }
}
