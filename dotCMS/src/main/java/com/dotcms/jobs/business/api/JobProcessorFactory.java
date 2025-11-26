package com.dotcms.jobs.business.api;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.jobs.business.error.JobProcessorInstantiationException;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotmarketing.util.Logger;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobProcessorFactory {

    /**
     * Creates a new instance of the specified job processor class.
     * First attempts to get the processor from CDI context, falls back to direct instantiation if that fails.
     *
     * @param processorClass The class of the job processor to create.
     * @return A new job processor instance
     * @throws JobProcessorInstantiationException if creation fails through both methods
     */
    JobProcessor newInstance(Class<? extends JobProcessor> processorClass) {

        Optional<? extends JobProcessor> cdiInstance = CDIUtils.getBean(processorClass);

        if (cdiInstance.isPresent()) {
            return cdiInstance.get();
        }

        // If CDI fails, try direct instantiation
        return createInstance(processorClass);
    }

    /**
     * Creates a new instance using reflection.
     */
    private JobProcessor createInstance(Class<? extends JobProcessor> processorClass) {
        try {
            return processorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Logger.error(this, "Error creating job processor via reflection", e);
            throw new JobProcessorInstantiationException(processorClass, e);
        }
    }
}