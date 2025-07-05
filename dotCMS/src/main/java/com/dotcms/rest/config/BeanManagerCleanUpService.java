package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import java.util.Optional;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

/**
 *
 */
public class BeanManagerCleanUpService {

    private BeanManagerCleanUpService() {
    }

    static Optional<BeanManagerImpl> getBeanManagerImpl() {
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

    public static void cleanUp() {
        final Optional<BeanManagerImpl> optional = getBeanManagerImpl();
        if (optional.isPresent()) {
            final BeanManagerImpl impl = optional.get();
            final ClassTransformer classTransformer = impl.getServices().get(ClassTransformer.class);
            classTransformer.cleanup();
            classTransformer.getSharedObjectCache().cleanup();
            classTransformer.getReflectionCache().cleanup();
            Logger.info(BeanManagerCleanUpService.class, "BeanManager cache cleared.");
        } else {
            Logger.warn(BeanManagerCleanUpService.class, "BeanManagerImpl not properly cleaned up");
        }

    }

}
