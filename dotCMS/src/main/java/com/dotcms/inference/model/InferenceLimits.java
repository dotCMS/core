package com.dotcms.inference.model;

import com.dotmarketing.util.Config;

/**
 * The capacity ceilings this endpoint family enforces.
 *
 * <p>All of them are configurable and all of them have defaults, because the risk they bound is
 * real but its right size is deployment-specific. Streaming is the reason most of them exist: a
 * streamed completion parks a request thread for the whole generation rather than for one round
 * trip, so concurrency and elapsed time are the scarce resources here, not request rate.</p>
 *
 * <p>{@link #maxImagesPerRequest()} bounds a different resource. Image generation is priced per
 * image, so a single accepted request can multiply a site's provider spend by whatever it asked
 * for, and per-site spend quotas are out of scope for this work. The ceiling is what stops one
 * request being an unbounded bill.</p>
 *
 * <p>Read through {@link #current()} at the point of use rather than cached in a field, so an
 * operator changing a property does not have to restart the node to see it take effect.</p>
 *
 * @param maxConcurrentStreams     streaming completions allowed at once on this node
 * @param completionTimeoutSeconds hard ceiling on a single completion
 * @param maxRequestBytes          largest request body accepted
 * @param maxImagesPerRequest      most images one generation request may ask for
 */
public record InferenceLimits(int maxConcurrentStreams,
                              int completionTimeoutSeconds,
                              int maxRequestBytes,
                              int maxImagesPerRequest) {

    /** Streaming completions allowed at once on this node. */
    public static final String MAX_CONCURRENT_STREAMS_KEY = "DOT_INFERENCE_MAX_CONCURRENT_STREAMS";
    /** Hard ceiling on a single completion, in seconds. */
    public static final String COMPLETION_TIMEOUT_SECONDS_KEY = "DOT_INFERENCE_COMPLETION_TIMEOUT_SECONDS";
    /** Largest request body accepted, in bytes. */
    public static final String MAX_REQUEST_BYTES_KEY = "DOT_INFERENCE_MAX_REQUEST_BYTES";
    /** Most images one generation request may ask for. */
    public static final String MAX_IMAGES_PER_REQUEST_KEY = "DOT_INFERENCE_MAX_IMAGES_PER_REQUEST";

    /** Each stream holds a request thread for the life of a completion. */
    public static final int DEFAULT_MAX_CONCURRENT_STREAMS = 50;
    /** Five minutes; long enough for a slow reasoning model, short enough to bound a hung stream. */
    public static final int DEFAULT_COMPLETION_TIMEOUT_SECONDS = 300;
    /** 1 MiB; holds a long multi-turn conversation with tool results, and bounds parse cost. */
    public static final int DEFAULT_MAX_REQUEST_BYTES = 1024 * 1024;
    /**
     * Ten, which is the ceiling the adopted format itself documents for this field. Taking the
     * number from the format rather than inventing one means a client written against the
     * standard meets the same limit here that it already handles there.
     */
    public static final int DEFAULT_MAX_IMAGES_PER_REQUEST = 10;

    /**
     * @return the limits as currently configured on this node
     */
    public static InferenceLimits current() {
        return new InferenceLimits(
                Config.getIntProperty(MAX_CONCURRENT_STREAMS_KEY, DEFAULT_MAX_CONCURRENT_STREAMS),
                Config.getIntProperty(COMPLETION_TIMEOUT_SECONDS_KEY, DEFAULT_COMPLETION_TIMEOUT_SECONDS),
                Config.getIntProperty(MAX_REQUEST_BYTES_KEY, DEFAULT_MAX_REQUEST_BYTES),
                Config.getIntProperty(MAX_IMAGES_PER_REQUEST_KEY, DEFAULT_MAX_IMAGES_PER_REQUEST));
    }

    /**
     * @param bytes the size of an incoming request body
     * @return whether it exceeds {@link #maxRequestBytes()}
     */
    public boolean exceedsMaxRequestBytes(final long bytes) {
        return bytes > maxRequestBytes;
    }

    /**
     * @param count how many images a generation request asked for
     * @return whether it exceeds {@link #maxImagesPerRequest()}
     */
    public boolean exceedsMaxImagesPerRequest(final int count) {
        return count > maxImagesPerRequest;
    }
}
