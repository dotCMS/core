package com.dotcms.api.di;

import com.dotcms.api.di.module.DotModule;
import com.dotcms.api.di.module.ModuleConfigurator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Abstraction of the DotInjector in charge of providing the implementations for DOTCMS.
 * @author jsanca
 */
public interface DotInjector {

    /**
     * Get the bean associated to this clazz.
     * @param clazz {@link Class}
     * @param <T>
     * @return T
     */
    public <T> T getBean (Class<T> clazz);
    public <T> T getBean (Class<T> clazz, Annotation annotation);

    public <T> T getBean (Class<T> clazz, Set<Annotation> qualifiers);

    public DotInjector addModule (ModuleConfigurator moduleConfigurator);

    public DotInjector addModule (DotModule module);

    public DotInjector addModule (Object internalModule);

} // E:O:F:DotInjector.
