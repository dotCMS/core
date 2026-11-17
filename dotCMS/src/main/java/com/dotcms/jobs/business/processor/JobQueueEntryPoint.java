package com.dotcms.jobs.business.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the REST method that creates jobs for a queue, declaring that the queue's entry point is
 * that dedicated endpoint: it validates the request and checks the caller's rights before putting
 * the job on the queue. The generic endpoint {@code POST /api/v1/jobs/{queueName}} refuses such a
 * queue, so those jobs can only be created through the endpoint that validates them.
 * <p>
 * A queue fed by the generic endpoint declares that instead with
 * {@code @Queue(value = "...", genericEntry = true)}. A core queue must declare exactly one entry
 * point.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JobQueueEntryPoint {

    /**
     * @return the name of the queue this method creates jobs for, the same as {@link Queue#value()}
     */
    String value();
}
