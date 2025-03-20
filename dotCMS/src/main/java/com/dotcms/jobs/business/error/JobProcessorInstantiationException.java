package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.processor.JobProcessor;

/**
 * Exception thrown when an error occurs while attempting to instantiate a new JobProcessor.
 */
public class JobProcessorInstantiationException extends RuntimeException{

    /**
     * Constructs a new JobProcessorInstantiationException with the specified processor class and cause.
     *
     * @param processorClass The class of the JobProcessor that could not be instantiated
     * @param cause The underlying cause of the error (can be null)
     */
    public JobProcessorInstantiationException(Class<? extends JobProcessor> processorClass, Throwable cause) {
        super("Failed to instantiate a new JobProcessor out of the provided class: " + processorClass.getName(), cause);
    }

}
