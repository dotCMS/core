package com.dotcms.rest.config;

import com.dotcms.api.di.DotBean;
import com.dotcms.api.di.DotInjector;
import com.dotcms.repackage.org.glassfish.hk2.api.MultiException;
import com.dotcms.repackage.org.jvnet.hk2.internal.ServiceLocatorImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.dotcms.util.CollectionsUtils.set;
import static com.dotcms.util.ReflectionUtils.getClassFor;

public class DotServiceLocatorImpl extends ServiceLocatorImpl {

    private final DotInjector dotInjector;

    public DotServiceLocatorImpl(String name, ServiceLocatorImpl parent, final DotInjector dotInjector) {
        super(name, parent);
        this.dotInjector = dotInjector;
    }

    @Override
    public <T> T getService(Class<T> aClass, Annotation... annotations) throws MultiException {

        final T bean = (aClass.isAnnotationPresent(DotBean.class))?
                dotInjector.getBean(aClass, set(annotations)):null;

        return (null != bean)?
                bean: super.getService(aClass,  annotations);
    }

    @Override
    public <T> T getService(Type type, Annotation... annotations) throws MultiException {

        final Class<T> clazz = (Class<T>)getClassFor(type);
        final T bean = (clazz.isAnnotationPresent(DotBean.class))?
                dotInjector.getBean(clazz, set(annotations)):null;

        return (null != bean)?
                bean: super.getService(type,  annotations);
    }
}
