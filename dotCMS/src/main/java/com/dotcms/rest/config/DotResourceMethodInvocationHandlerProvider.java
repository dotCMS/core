package com.dotcms.rest.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

@Priority(1)
@Provider
@ApplicationScoped
public class DotResourceMethodInvocationHandlerProvider implements
        ResourceMethodInvocationHandlerProvider {

    private static final InvocationHandler SAFE_DEFAULT_HANDLER = new SafeInvocationHandler();

    @Override
    public InvocationHandler create(Invocable resourceMethod) {
        return SAFE_DEFAULT_HANDLER;
    }

    private static class SafeInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(proxy, args);

            } catch (IllegalArgumentException e) {
                if (e.getMessage() != null && e.getMessage()
                        .contains("object is not an instance of declaring class")) {

                    return handleClassLoaderConflict(proxy, method, args);
                }
                throw e;
            }
        }


        private Object handleClassLoaderConflict(Object target, Method originalMethod,
                Object[] args) throws Throwable {
            try {
                ClassLoader targetClassLoader = target.getClass().getClassLoader();

                Class<?> targetClass = targetClassLoader.loadClass(target.getClass().getName());

                Class<?>[] paramTypes = originalMethod.getParameterTypes();
                Method correctMethod = targetClass.getMethod(originalMethod.getName(), paramTypes);

                Object result = correctMethod.invoke(target, args);

                return result;

            } catch (Exception fallbackException) {

                return handleLastResortRecreation(target, originalMethod, args);
            }
        }

        private Object handleLastResortRecreation(Object target, Method method, Object[] args) {
            try {

                ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

                Class<?> newTargetClass = currentClassLoader.loadClass(target.getClass().getName());

                Object newTarget = newTargetClass.getDeclaredConstructor().newInstance();

                Method correctMethod = newTargetClass.getMethod(method.getName(),
                        method.getParameterTypes());

                return correctMethod.invoke(newTarget, args);

            } catch (Exception e) {

                throw new RuntimeException(
                        "Unable to resolve ClassLoader conflict for method: " + method.getName(),
                        e);
            }
        }
    }

}
