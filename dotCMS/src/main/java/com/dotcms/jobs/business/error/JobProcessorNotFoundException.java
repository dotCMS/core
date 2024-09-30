package com.dotcms.jobs.business.error;

/**
 * Exception thrown when no job processor is found for a specified queue. This typically occurs when
 * attempting to process a job from a queue that has no registered processor.
 */
public class JobProcessorNotFoundException extends RuntimeException {

    /**
     * Constructs a new JobProcessorNotFoundException with the specified queue name.
     *
     * @param queueName The name of the queue for which no processor was found
     */
    public JobProcessorNotFoundException(String queueName) {
        super("No job processor found for queue: " + queueName);
    }
}