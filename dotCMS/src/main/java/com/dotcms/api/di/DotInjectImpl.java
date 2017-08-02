package com.dotcms.api.di;

import com.dotcms.api.di.module.DotModule;
import com.dotcms.api.di.module.ModuleConfigurator;
import com.dotmarketing.util.Logger;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

import java.lang.annotation.Annotation;
import java.util.Set;

class DotInjectImpl implements DotInjector {

    private volatile Injector injector;

    public DotInjectImpl(final Injector injector) {

        this.injector = injector;
    }

    @Override
    public <T> T getBean(final Class<T> clazz) {
        return this.injector.getInstance(clazz);
    }

    @Override
    public <T> T getBean(final Class<T> clazz, final Annotation annotation) {

        return this.injector.getInstance(Key.get(clazz, annotation));
    }

    @Override
    public <T> T getBean(final Class<T> clazz, final Set<Annotation> qualifiers) {

        T bean = null;

        if (qualifiers.size() > 0) {

            for (Annotation annotation: qualifiers) {

                try {

                    bean = this.getBean(clazz, annotation);
                    if (null != bean) {
                        return bean;
                    }
                } catch (Exception e) {
                    Logger.debug(this, "Error On DotInjector: " +
                                    e.getMessage(), e);
                }
            }
        }

        try {

            bean = this.getBean(clazz);
        } catch (Exception e) {
            Logger.debug(this, "Error On DotInjector: " +
                    e.getMessage(), e);
        }

        return bean;
    }

    @Override
    public DotInjector addModule(final ModuleConfigurator moduleConfigurator) {

        // todo: wrap it
        return this;
    }

    @Override
    public DotInjector addModule(DotModule module) {

        this.injector = this.injector.createChildInjector(module);

        return this;
    }

    @Override
    public DotInjector addModule(Object internalModule) {

        if (internalModule instanceof Module) {

            this.injector = this.injector.createChildInjector((Module)internalModule);
        }

        return this;
    }
}
