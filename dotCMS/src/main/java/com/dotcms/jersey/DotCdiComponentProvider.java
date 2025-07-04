package com.dotcms.jersey;

import com.dotmarketing.util.Logger;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import org.glassfish.jersey.ext.cdi1x.internal.AbstractCdiBeanSupplier;
import org.glassfish.jersey.ext.cdi1x.internal.CdiUtil;
import org.glassfish.jersey.ext.cdi1x.internal.LocalizationMessages;
import org.glassfish.jersey.ext.cdi1x.internal.RequestScopedCdiBeanSupplier;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.glassfish.jersey.internal.util.collection.Cache;

@Priority(300)
public class DotCdiComponentProvider extends org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider {

    public DotCdiComponentProvider() {
        super();
        System.out.println(" ::: dot ini construct ::: ");
    }

    @Override
    public void initialize(final InjectionManager injectionManager) {
        super.initialize(injectionManager);
        System.out.println(" ::: dot ini ::: ");
    }

    protected InjectionManager getInjectionManager() {
        try {
            Field field = getClass().getSuperclass().getDeclaredField("injectionManager");
            field.setAccessible(true);
            return (InjectionManager) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot access injectionManager", e);
        }
    }

    protected BeanManager getBeanManager() {
        try {
            Field field = getClass().getSuperclass().getDeclaredField("beanManager");
            field.setAccessible(true);
            return (javax.enterprise.inject.spi.BeanManager) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot access beanManager field", e);
        }
    }

     protected boolean isJaxRsComponentType(Class<?> clazz) {
        try {
            Method method = getClass().getSuperclass().getDeclaredMethod("isJaxRsComponentType", Class.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(this, clazz);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot access isJaxRsComponentType method", e);
        }
    }

     protected boolean isJerseyOrDependencyType(Class<?> clazz) {
        try {
            Method method = getClass().getSuperclass().getDeclaredMethod("isJerseyOrDependencyType", Class.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(null, clazz); // null because it's static
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot access isJerseyOrDependencyType method", e);
        }
    }

    protected boolean isCdiComponent(Class<?> component) {
        try {
            Method method = getClass().getSuperclass().getDeclaredMethod("isCdiComponent", Class.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(this, component); // 'this' because it's not static
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot access isCdiComponent method", e);
        }
    }

    protected boolean isManagedBean(Class<?> component) {
        try {
            Method method = getClass().getSuperclass().getDeclaredMethod("isManagedBean", Class.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(this, component);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot access isManagedBean method", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Cache<Class<?>, Boolean> getJaxRsResourceCache() {
        try {
            Field field = getClass().getSuperclass().getDeclaredField("jaxRsResourceCache");
            field.setAccessible(true);
            return (Cache<Class<?>, Boolean>) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot access jaxRsResourceCache field", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Set<Class<?>> getRequestScopedComponents() {
        try {
            Field field = getClass().getSuperclass().getDeclaredField("requestScopedComponents");
            field.setAccessible(true);
            return (Set<Class<?>>) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot access requestScopedComponents field", e);
        }
    }

    @Override
    public boolean bind(final Class<?> clazz, final Set<Class<?>> providerContracts) {
        Logger.debug(DotCdiComponentProvider.class,LocalizationMessages.CDI_CLASS_BEING_CHECKED(clazz));

        BeanManager beanManager = getBeanManager();
        final InjectionManager injectionManager = getInjectionManager();

        if (beanManager == null) {
            return false;
        }

        if (isJerseyOrDependencyType(clazz)) {
            return false;
        }

        final boolean isCdiManaged = isCdiComponent(clazz);
        final boolean isManagedBean = isManagedBean(clazz);
        final boolean isJaxRsComponent = isJaxRsComponentType(clazz);

        if (!isCdiManaged && !isManagedBean && !isJaxRsComponent) {
            return false;
        }

        final Cache<Class<?>, Boolean> jaxRsResourceCache = getJaxRsResourceCache();

        final boolean isJaxRsResource = jaxRsResourceCache.apply(clazz);

        final Class<? extends Annotation> beanScopeAnnotation = CdiUtil.getBeanScope(clazz, beanManager);
        final boolean isRequestScoped = beanScopeAnnotation == RequestScoped.class
                || (beanScopeAnnotation == Dependent.class && isJaxRsResource);

        @SuppressWarnings("unchecked")
        Supplier<AbstractCdiBeanSupplier> beanFactory = isRequestScoped
                ? new RequestScopedCdiBeanSupplier(clazz, injectionManager, beanManager, isCdiManaged)
                : new DotGenericCdiBeanSupplier(clazz, injectionManager, beanManager, isCdiManaged);

        SupplierInstanceBinding<AbstractCdiBeanSupplier> builder = Bindings.supplier(beanFactory).to(clazz);
        for (final Class contract : providerContracts) {
            builder.to(contract);
        }
        injectionManager.register(builder);

        if (isRequestScoped) {
            getRequestScopedComponents().add(clazz);
        }

        Logger.debug(DotCdiComponentProvider.class, LocalizationMessages.CDI_CLASS_BOUND_WITH_CDI(clazz));

        return true;
    }



}
