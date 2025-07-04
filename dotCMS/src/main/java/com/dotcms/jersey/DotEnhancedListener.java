package com.dotcms.jersey;

import java.lang.reflect.Field;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import org.jboss.weld.environment.servlet.DotWeldServletLifecycle;
import org.jboss.weld.environment.servlet.EnhancedListener;
import org.jboss.weld.environment.servlet.WeldServletLifecycle;
import org.jboss.weld.environment.servlet.logging.WeldServletLogger;

public class DotEnhancedListener extends EnhancedListener {

    static final String INSTANCE_ATTRIBUTE_NAME = WeldServletLifecycle.class.getPackage().getName() + ".lifecycleInstance";

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext context) throws ServletException {
        WeldServletLogger.LOG.initializeWeldUsingServletContainerInitializer();
        context.setAttribute(ENHANCED_LISTENER_USED_ATTRIBUTE_NAME, Boolean.TRUE);
        //Here we set up our own ServletLifeCycle
        final DotWeldServletLifecycle lifecycle = new DotWeldServletLifecycle();
        // If not initialized properly, don't register itself as a listener
        if (lifecycle.initialize(context)) {
            setLifecycle(lifecycle);
            context.setAttribute(INSTANCE_ATTRIBUTE_NAME, lifecycle);
            context.addListener(this);
            super.contextInitialized(new ServletContextEvent(context));
        }
    }

    public void setLifecycle(WeldServletLifecycle lifecycle) {
        try {
            Field lifecycleField = EnhancedListener.class.getDeclaredField("lifecycle");
            lifecycleField.setAccessible(true);
            lifecycleField.set(this, lifecycle);
        } catch (Exception e) {
            throw new RuntimeException("Could not set lifecycle field", e);
        }
    }

}
