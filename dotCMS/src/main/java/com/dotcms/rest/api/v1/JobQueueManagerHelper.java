package com.dotcms.rest.api.v1;

import com.dotcms.jobs.business.api.JobProcessorScanner;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.util.List;

@ApplicationScoped
public class JobQueueManagerHelper {

    private JobProcessorScanner scanner;

    @Inject
    public JobQueueManagerHelper(JobProcessorScanner scanner) {
        this.scanner = scanner;
    }

    public JobQueueManagerHelper() {
    }

    public void registerProcessors(JobQueueManagerAPI jobQueueManagerAPI) {
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
                registerProcessor(jobQueueManagerAPI, processor);
            } catch (Exception e) {
                Logger.error(this.getClass(), "Unable to register JobProcessor ", e);
            }
        });
    }

    /**
     * Test if a processor can be instantiated
     * @param processor The processor to tested
     * @return true if the processor can be instantiated, false otherwise
     */
    private boolean testInstantiation(Class<? extends JobProcessor> processor) {
        try {
            Constructor<? extends JobProcessor> declaredConstructor = processor.getDeclaredConstructor();
            declaredConstructor.newInstance();
            return true;
        } catch (Exception e) {
            Logger.error(this.getClass(), String.format(" JobProcessor [%s] cannot be instantiated and will be ignored.", processor.getName()), e);
        }
        return false;
    }

    private void registerProcessor(JobQueueManagerAPI jobQueueManagerAPI, Class<? extends JobProcessor> processor) {
        if (processor.isAnnotationPresent(Queue.class)) {
            Queue queue = processor.getAnnotation(Queue.class);
            jobQueueManagerAPI.registerProcessor(queue.value(), processor);
        } else {
            jobQueueManagerAPI.registerProcessor(processor.getName(), processor);
        }
    }

    public void shutdown(JobQueueManagerAPI jobQueueManagerAPI) {
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

