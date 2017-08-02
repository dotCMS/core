package com.dotcms.rest.config;

import com.dotcms.api.di.DotBean;
import com.dotcms.api.di.DotInjector;
import com.dotcms.repackage.org.glassfish.hk2.api.MultiException;
import com.dotcms.repackage.org.glassfish.hk2.api.ServiceLocator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import static com.dotcms.util.CollectionsUtils.*;
import static com.dotcms.util.ReflectionUtils.*;

public class DotInjectorServiceLocator extends DotServiceLocatorWrapper {

    private final DotInjector dotInjector;

    public DotInjectorServiceLocator(final ServiceLocator serviceLocator,
                                     final DotInjector dotInjector) {
        super(serviceLocator);
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

        final T bean = dotInjector.getBean((Class<T>) getClassFor(type), set(annotations));

        return (null != bean)?
                bean: super.getService(type,  annotations);
    }
}
