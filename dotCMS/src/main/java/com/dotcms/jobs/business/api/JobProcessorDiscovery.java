package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

/**
 * Discovers all classes that implement the JobProcessor interface using CDI.
 */
@ApplicationScoped
public class JobProcessorDiscovery {

    private final BeanManager beanManager;

    @Inject
    public JobProcessorDiscovery(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Default constructor required by CDI.
     */
    public JobProcessorDiscovery() {
        this.beanManager = null;
    }

    /**
     * Discovers all classes that implement the JobProcessor interface using CDI. Does not create
     * instances, only finds the classes.
     *
     * @return A list of classes that implement the JobProcessor interface.
     */
    public List<Class<? extends JobProcessor>> discoverJobProcessors() {

        List<Class<? extends JobProcessor>> processors = new ArrayList<>();

        try {
            Set<Bean<?>> beans = beanManager.getBeans(JobProcessor.class, Any.Literal.INSTANCE);

            for (Bean<?> bean : beans) {
                Class<?> beanClass = bean.getBeanClass();

                if (JobProcessor.class.isAssignableFrom(beanClass)
                        && isValidateScope(bean)) {
                    processors.add((Class<? extends JobProcessor>) beanClass);
                    Logger.debug(this, "Discovered JobProcessor: " + beanClass.getName());
                }
            }
        } catch (Exception e) {
            var errorMessage = "Error discovering JobProcessors";
            Logger.error(this, errorMessage, e);
            throw new DotRuntimeException(errorMessage, e);
        }

        if (processors.isEmpty()) {
            Logger.warn(this, "No JobProcessors were discovered");
        }

        return processors;
    }

    /**
     * Validates that the scope of the bean is correct for a JobProcessor.
     *
     * @param bean The bean to validate.
     * @return True if the scope is valid, false otherwise.
     */
    private boolean isValidateScope(Bean<?> bean) {

        Class<?> scope = bean.getScope();
        if (scope != Dependent.class) {
            final String errorMessage = "JobProcessor " + bean.getBeanClass().getName() +
                    " must use @Dependent scope, found: " + scope.getName() + " scope. "
                    + "Won't be registered.";
            Logger.error(this, errorMessage);
            return false;
        }

        return true;
    }

}