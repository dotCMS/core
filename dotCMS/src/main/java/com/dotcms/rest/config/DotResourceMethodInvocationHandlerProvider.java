package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

/**
 * Proactive InvocationHandler that guarantees method and target use the same ClassLoader
 * to prevent IllegalArgumentException: object is not an instance of declaring class
 * The problem we are solving is that the original version provided by Jersey has a default static handler
 * that keeps the ClassLoader of the method, which can lead to issues when the method is invoked on a different ClassLoader
 * e.g. after uninstall and re-install a Resource from an OSGI plugin
 * This handler proactively resolves ClassLoader differences before invoking the method if any
 */
@Provider
@Priority(1)
@Singleton
public class DotResourceMethodInvocationHandlerProvider implements ResourceMethodInvocationHandlerProvider {
    /**
     * This method is called by Jersey to create an InvocationHandler for the resource method.
     * It returns a ProactiveClassLoaderHandler that ensures ClassLoader consistency.
     *
     * @param resourceMethod The resource method for which the handler is created.
     * @return An InvocationHandler that guarantees ClassLoader consistency.
     */
    @Override
    public InvocationHandler create(Invocable resourceMethod) {
        return new ProactiveClassLoaderHandler();
    }

    /**
     * InvocationHandler that GUARANTEES method and target use the same ClassLoader
     * by resolving ClassLoader differences BEFORE method invocation
     */
    private static class ProactiveClassLoaderHandler implements InvocationHandler {

        /**
         * This method is invoked when the resource method is called.
         * It checks ClassLoader consistency and resolves any differences before invoking the method.
         *
         * @param proxy The proxy instance that the method was invoked on.
         * @param method The method that was invoked.
         * @param args The arguments passed to the method.
         * @return The result of the method invocation.
         * @throws Throwable If an error occurs during method invocation.
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Get target and its ClassLoader
            Class<?> targetClass = proxy.getClass();
            ClassLoader targetClassLoader = targetClass.getClassLoader();
            ClassLoader methodClassLoader = method.getDeclaringClass().getClassLoader();

            // If ClassLoaders differ, resolve BEFORE invoking
            if (!isSameClassLoader(targetClassLoader, methodClassLoader)) {
                Logger.debug("DotResourceMethodInvocationHandlerProvider",
                        "ClassLoader mismatch detected - method: " + method.getName() +
                        ", target class: " + targetClass.getName() +
                        ", method class: " + method.getDeclaringClass().getName());

                method = getMethodFromTargetClassLoader(proxy, method);
            }

            // Invoke with GUARANTEED ClassLoader consistency
            return method.invoke(proxy, args);
        }

        /**
         * Checks if two ClassLoaders are the same
         * Handles null ClassLoaders (bootstrap ClassLoader)
         */
        private boolean isSameClassLoader(ClassLoader cl1, ClassLoader cl2) {
            // Both null means same ClassLoader (bootstrap)
            if (cl1 == null && cl2 == null) return true;

            // One null, other not = different ClassLoaders
            if (cl1 == null || cl2 == null) return false;

            // Direct comparison
            return cl1.equals(cl2);
        }

        /**
         * Gets the EXACT method from the target's ClassLoader
         * This ensures method and target share the same ClassLoader
         */
        private Method getMethodFromTargetClassLoader(Object target, Method originalMethod) throws NoSuchMethodException {
            Class<?> targetClass = target.getClass();
            // Get method from target's class (same ClassLoader guaranteed)
            Method targetMethod = targetClass.getMethod(
                    originalMethod.getName(),
                    originalMethod.getParameterTypes()
            );

            Logger.debug(DotResourceMethodInvocationHandlerProvider.class,
                    "Resolved method from target ClassLoader - " +
                    "Original method: " + originalMethod.getDeclaringClass().getName() +
                    ", Target method: " + targetMethod.getDeclaringClass().getName());
            return targetMethod;
        }
    }
}