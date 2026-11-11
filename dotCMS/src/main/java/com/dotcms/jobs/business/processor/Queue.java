package com.dotcms.jobs.business.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the queue a job processor serves.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Queue {

    /**
     * @return the queue name
     */
    String value();

    /**
     * Declares that this queue is entered through the generic endpoint
     * {@code POST /api/v1/jobs/{queueName}}. The default, {@code false}, means "no entry declared
     * here", which is not the same as dedicated: a queue with its own endpoint declares that on the
     * REST method with {@link JobQueueEntryPoint} instead.
     *
     * @return true when the generic endpoint may create jobs on this queue
     */
    boolean genericEntry() default false;
}
