package com.dotcms.jobs.business.error;

/**
 * Exception thrown when no processor is found for a specified queue. This typically occurs when
 * attempting to process a job from a queue that has no registered processor.
 */
public class ProcessorNotFoundException extends RuntimeException {

    /**
     * Constructs a new NoProcessorFoundException with the specified queue name.
     *
     * @param queueName The name of the queue for which no processor was found
     */
    public ProcessorNotFoundException(String queueName) {
        super("No processor found for queue: " + queueName);
    }
}