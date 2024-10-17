package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.error.JobProcessorInstantiationException;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotmarketing.util.Logger;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobProcessorFactory {

    public JobProcessorFactory() {
        // Default constructor for CDI
    }

    /**
     * Creates a new instance of the specified job processor class.
     *
     * @param processorClass The class of the job processor to create.
     * @return An optional containing the new job processor instance, or an empty optional if the
     * processor could not be created.
     */
    JobProcessor newInstance(
            Class<? extends JobProcessor> processorClass) {
        try {
            return processorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Logger.error(this, "Error creating job processor", e);
            throw new JobProcessorInstantiationException(processorClass, e);
        }
    }

}
