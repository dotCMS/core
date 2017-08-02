package com.dotcms.api.di.module;

import com.dotcms.api.aop.MethodInterceptor;
import com.dotcms.api.aop.guice.GuiceDelegateMethodInvocation;
import com.dotcms.api.aop.guice.MethodInterceptorConfig;
import com.dotcms.api.di.DotBean;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.util.ClassPathUtils;
import com.dotcms.util.ScannedGroupBean;
import com.dotmarketing.util.Logger;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static com.dotcms.util.ClassPathUtils.scanClasses;
import static com.dotcms.util.ReflectionUtils.getClassFor;
import static com.dotcms.util.ReflectionUtils.getFirstInterface;
import static com.dotcms.util.ReflectionUtils.newInstance;

/**
 * Convenient base module for DotCMS injection.
 * @author jsanca
 */
public class DotModule extends AbstractModule implements ModuleConfigurator {

    private final Class<?> [] classes;
    private final String   [] classesNames;
    private final Package  [] packages;
    private final String   [] packagesNames;
    private final ClassLoader classLoader;

    public DotModule() {
        this(null, null, null, null,
                getDefaultClassLoader());
    }
    public DotModule(final Class<?>[] classes,
                     final String[] classesNames,
                     final Package[] packages,
                     final String[] packagesNames,
                     final ClassLoader classLoader) {

        this.classes       = classes;
        this.classesNames  = classesNames;
        this.packages      = packages;
        this.packagesNames = packagesNames;
        this.classLoader   = classLoader;
    }

    protected static ClassLoader getDefaultClassLoader () {

        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected void configure() {

        processClasses  (merge (this.classes(), this.classesNames()));
        processPackages (merge (this.packages(), this.packagesNames()));
        processCustomConfiguration();
    } // configure.

    protected void processCustomConfiguration() {
        // todo: do custom stuff here
    }

    private Collection<String> merge(final Package[] packages, final String[] packagesNames) {
        final ImmutableSet.Builder<String> pacakgeSetBuilder = new ImmutableSet.Builder<>();

        if (null != packagesNames) {

            pacakgeSetBuilder.add(packagesNames);
        }

        if (null != packages) {

            for (Package aPackage : packages) {

                pacakgeSetBuilder.add(aPackage.getName());
            }
        }

        return pacakgeSetBuilder.build();
    } // merge.

    private Collection<Class<?>> merge(final Class<?>[] classes, final String[] classesNames) {
        final ImmutableSet.Builder<Class<?>> classSetBuilder = new ImmutableSet.Builder<>();

        if (null != classes) {

            classSetBuilder.add(classes);
        }

        if (null != classesNames) {

            for (String className : classesNames) {

                classSetBuilder.add(getClassFor(className));
            }
        }

        return classSetBuilder.build();
    } // merge.

    private void processPackages(final Collection<String> packages) {

        final Collection<ScannedGroupBean> scannedGroupBeans =
                scanClasses(DotBean.class, this.getClassLoader(), packages.toArray(new String [] {}));

        scannedGroupBeans.forEach( scannedGroupBean ->  this.processClass(scannedGroupBean.getClazz()) );
    } // processPackages.

    private void processClasses(final Collection<Class<?>> classes) {

        classes.forEach( this::processClass );
    } // processClasses.

    private void processClass(final Class<?> clazz) {
        // todo: here has to support names, custom qualifier annotations and DotFactoryBean (Providers)
        // use a register class
        Class baseClass = getFirstInterface(clazz); // todo: avoid common interfaces such as Serializable, anything that starts with java.
        if (null != baseClass) {
            bind(baseClass).to(clazz);
        } else {
            bind(clazz);
        }
    }


    /**
     * Scan aspects with the a current thread class loader over the packages passed in the packages parameter
     * @param packages Collection
     */
    public void scanAspects (final String... packages) {

        this.scanAspects(this.getClassLoader(), packages);
    } // scanAspects

    /**
     * Scan aspects with a given class loader over the packages passed in the packages parameter
     * @param packages Collection
     */

    public void scanAspects (final ClassLoader classLoader, final String ...packages) {

        Collection<ScannedGroupBean> classes = null;

        try {

            classes =
                    ClassPathUtils.scanClasses(MethodInterceptorConfig.class,
                            MethodInterceptor.class, classLoader, packages);

            if (null != classes) {

                this.bindInterceptors (classes);
            }
        } catch (Exception e) {

            Logger.error(this, "Error on APIInjector: " + e.getMessage(), e);
        }
    } // scanAspects.

    protected void bindInterceptors (final Collection<ScannedGroupBean> scannedGroups) {

        Class clazz                                         = null;
        MethodInterceptorConfig methodInterceptorConfig     = null;
        Matcher<? super Class<?>> classMatcher              = null;
        String [] packages                                  = null;

        for (ScannedGroupBean scannedGroup : scannedGroups) {

            clazz                   = scannedGroup.getClazz();
            methodInterceptorConfig = (MethodInterceptorConfig)scannedGroup.getAnnotation();
            Logger.debug(this, "Found DotBean on class: " + clazz +
                    ", with config: " + methodInterceptorConfig);

            if (MethodInterceptorConfig.ANY.equalsIgnoreCase(methodInterceptorConfig.packageMatcher())) {

                this.bindInterceptor(Matchers.any(),
                        methodInterceptorConfig.annotation(),
                        (MethodInterceptor)newInstance(clazz));
            } else {

                packages = methodInterceptorConfig.packageMatcher().split(",");
                for (String aspectPackage : packages) { // adding interceptors for each package

                    classMatcher = Matchers.inPackage(Package.getPackage(aspectPackage));
                    this.bindInterceptor(classMatcher,
                            methodInterceptorConfig.annotation(),
                            (MethodInterceptor)newInstance(clazz));
                }
            }
        }
    } // bindInterceptors.

    protected void bindInterceptor(final Matcher<? super Class<?>> classMatcher,
                                   final Class<? extends Annotation> annotationType,
                                   final MethodInterceptor methodInterceptor) {

        this.bindInterceptor(classMatcher,
                Matchers.annotatedWith(annotationType),
                (invocation) -> methodInterceptor.invoke(new GuiceDelegateMethodInvocation(invocation)));
    } // bindInterceptor.

    @Override
    public Class<?>[] classes () {
        return this.classes;
    }

    @Override
    public String[] classesNames() {
        return this.classesNames;
    }

    @Override
    public Package[] packages() {
        return this.packages;
    }

    @Override
    public String[] packagesNames() {
        return this.packagesNames;
    }

    protected  ClassLoader getClassLoader () {
        return this.classLoader;
    }
} // E:O:F:DotModule.
