package com.dotcms.cdi;

import com.dotmarketing.util.Logger;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

/**
 * Utility class to get beans from CDI container
 */
public class CDIUtils {

    private static final AtomicBoolean cleanupFlag = new AtomicBoolean(false);

    /**
     * Private constructor to avoid instantiation
     */
    private CDIUtils() {
    }

    /**
     * Get a bean from CDI container and return an Optional with the bean if found, empty otherwise
     * @param clazz the class of the bean
     * @return an Optional with the bean if found, empty otherwise
     */
    public static <T> Optional<T> getBean(Class<T> clazz) {
            try {
                return Optional.of(getBeanThrows(clazz));
            } catch (Exception e) {
                // Exception is already logged in getBeanThrows
            }
            return Optional.empty();
    }


    /**
     * Get a bean from CDI container but throw an exception if the bean is not found
     * @param clazz the class of the bean
     * @return the bean
     * @param <T> the type of the bean
     */
    public static <T> T getBeanThrows(Class<T> clazz) {
        try {
            return CDI.current().select(clazz).get();
        } catch (Exception e) {
            String errorMessage = String.format("Unable to find bean of class [%s]: %s", clazz, e.getMessage());
            Logger.error(CDIUtils.class, errorMessage);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    /**
     * Get all beans of a given type from CDI container
     * @param clazz the class of the beans
     * @return a List containing all beans of the given type
     * @param <T> the type of the beans
     */
    public static <T> List<T> getBeans(Class<T> clazz) {
        try {
            return CDI.current().select(clazz).stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            String errorMessage = String.format("Unable to find beans of class [%s]: %s", clazz, e.getMessage());
            Logger.error(CDIUtils.class, errorMessage);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    /**
     * Get BeanManager implementation
     * @return BeanManagerImpl
     */
    private static Optional<BeanManagerImpl> getBeanManagerImpl() {
        final BeanManager beanManager = CDI.current().getBeanManager();
        if (beanManager instanceof BeanManagerImpl) {
            return Optional.of ((BeanManagerImpl) beanManager);
        }
        if (beanManager instanceof BeanManagerProxy) {
            BeanManagerProxy proxy = (BeanManagerProxy) beanManager;
            return Optional.of(proxy.delegate());
        }
        return Optional.empty();
    }

    /**
     * This method cleans the internal cache of the bean manager
     * CDI will continue to work But use with caution
     */
    public static void cleanUpCache() {
        if (!cleanupFlag.compareAndSet(false, true)) {
            Logger.debug(CDIUtils.class,"Cleanup already performed");
            return;
        }
        try {
            internalCleanUp();
        } finally {
            cleanupFlag.set(false);
        }
    }

    /**
     * internal method
     */
    private static void internalCleanUp() {
        final Optional<BeanManagerImpl> beanManager = getBeanManagerImpl();
        if (beanManager.isEmpty()) {
            Logger.warn(CDIUtils.class, "BeanManager not available");
            return;
        }

        final ServiceRegistry services = beanManager.get().getServices();
        if (services == null) {
            Logger.warn(CDIUtils.class, "ServiceRegistry not available");
            return;
        }

        final ClassTransformer classTransformer = services.get(ClassTransformer.class);
        if (classTransformer == null) {
            Logger.warn(CDIUtils.class, "ClassTransformer not available");
            return;
        }

        try {
            classTransformer.cleanup();

            if (classTransformer.getSharedObjectCache() != null) {
                classTransformer.getSharedObjectCache().cleanup();
            }

            if (classTransformer.getReflectionCache() != null) {
                classTransformer.getReflectionCache().cleanup();
            }

            Logger.info(CDIUtils.class, "BeanManager cache cleared");
        } catch (Exception e) {
            Logger.error(CDIUtils.class, "Cache cleanup failed", e);
            throw e;
        }
    }

}
