package com.dotcms.jobs.business.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to explicitly specify that a job processor should not retry failed jobs.
 * This provides a more semantic way to indicate no-retry behavior compared to setting
 * maxRetries=0 in ExponentialBackoffRetryPolicy.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@literal @}NoRetryPolicy
 * {@literal @}Queue("myQueue")
 * public class MyJobProcessor implements JobProcessor {
 *     // Implementation
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NoRetryPolicy {
    // No elements needed - presence of annotation is sufficient
}
