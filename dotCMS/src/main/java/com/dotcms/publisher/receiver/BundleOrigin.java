package com.dotcms.publisher.receiver;

/**
 * How a bundle reached this server.
 */
public enum BundleOrigin {

    /** A logged-in user uploaded the bundle file. */
    USER_UPLOAD,

    /** The bundle was sent by a registered sending endpoint. */
    REGISTERED_SENDER
}
