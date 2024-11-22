package com.dotcms.rest.api.v1;

import com.dotcms.jobs.business.api.JobProcessorScanner;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.util.AnnotationUtils;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

/**
 * Helper class for managing job queue processors in the JobQueueManagerAPI.
 * <p>
 * This class is responsible for discovering job processors, registering them with
 * the JobQueueManagerAPI, and shutting down the JobQueueManagerAPI when needed.
 */
@ApplicationScoped
public class JobQueueManagerHelper {

    private JobQueueManagerAPI jobQueueManagerAPI;
    private JobProcessorScanner scanner;

    /**
     * Constructor that injects the {@link JobProcessorScanner} and {@link JobQueueManagerAPI}.
     *
     * @param scanner The JobProcessorScanner to discover job processors
     * @param jobQueueManagerAPI The JobQueueManagerAPI instance to register processors with
     */
    @Inject
    public JobQueueManagerHelper(final JobProcessorScanner scanner, final JobQueueManagerAPI jobQueueManagerAPI) {
        this.scanner = scanner;
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }

    /**
     * Default constructor required by CDI.
     */
    public JobQueueManagerHelper() {
        // Default constructor required by CDI
    }

    /**
     * Registers all discovered job processors with the JobQueueManagerAPI.
     * If the JobQueueManagerAPI is not started, it starts the API before registering the processors.
     */
    public void registerProcessors() {
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            Logger.info(this.getClass(), "JobQueueManagerAPI started");
        }

        List<Class<? extends JobProcessor>> processors = scanner.discoverJobProcessors();
        processors.forEach(processor -> {
            try {
                if (!testInstantiation(processor)) {
                    return;
                }
                registerProcessor(processor);
            } catch (Exception e) {
                Logger.error(this.getClass(), "Unable to register JobProcessor ", e);
            }
        });
    }

    /**
     * Tests whether a given job processor can be instantiated by attempting to
     * create an instance of the processor using its default constructor.
     *
     * @param processor The processor class to test for instantiation
     * @return true if the processor can be instantiated, false otherwise
     */
    private boolean testInstantiation(final Class<? extends JobProcessor> processor) {
        try {
            Constructor<? extends JobProcessor> declaredConstructor = processor.getDeclaredConstructor();
            declaredConstructor.newInstance();
            return true;
        } catch (Exception e) {
            Logger.error(this.getClass(), String.format(" JobProcessor [%s] cannot be instantiated and will be ignored.", processor.getName()), e);
        }
        return false;
    }

    /**
     * Registers a job processor with the JobQueueManagerAPI using the queue name specified
     * in the {@link Queue} annotation, if present. If no annotation is found, the processor's
     * class name is used as the queue name.
     *
     * @param processor the processor class to register
     */
    private void registerProcessor(final Class<? extends JobProcessor> processor) {
        Queue queue = AnnotationUtils.getBeanAnnotation(processor, Queue.class);
        if (Objects.nonNull(queue)) {
            jobQueueManagerAPI.registerProcessor(queue.value(), processor);
        } else {
            jobQueueManagerAPI.registerProcessor(processor.getName(), processor);
        }
    }

    /**
     * Shuts down the JobQueueManagerAPI if it is currently started.
     * If the JobQueueManagerAPI is started, it attempts to close it gracefully.
     * In case of an error during the shutdown process, the error is logged.
     */
    public void shutdown() {
        if (jobQueueManagerAPI.isStarted()) {
            try {
                jobQueueManagerAPI.close();
                Logger.info(this.getClass(), "JobQueueManagerAPI successfully closed");
            } catch (Exception e) {
                Logger.error(this.getClass(), e.getMessage(), e);
            }
        }
    }
}
