package com.dotcms.util;

import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static com.dotcms.util.AnnotationUtils.getBeanAnnotation;
import static com.dotcms.util.ReflectionUtils.getClassFor;

/**
 * ClassPathUtils
 * @author jsanca
 */
public class ClassPathUtils {


    /**
     * Scan classes for a given packages
     * @param classLoader {@link ClassLoader}
     * @param packages    {@link String} array
     * @return Collection
     */
    public static Collection<Class> scanClasses (final ClassLoader classLoader,
                                                 final String ...packages) {

        ClassPath classPath = null;
        final ImmutableSet.Builder<Class> classesBuilder = new ImmutableSet.Builder<>();
        ImmutableSet<ClassPath.ClassInfo> classes = null;

        try {

            classPath = ClassPath.from(classLoader);
            for (String mypackage : packages) {

                classes =
                        classPath.getTopLevelClassesRecursive(mypackage);

                classes.forEach( classInfo -> {

                    classesBuilder.add(getClassFor(classInfo.getName()));
                } );
            }
        } catch (Exception e) {

            Logger.error(ClassPathUtils.class, "Error on ClassPathUtils: " + e.getMessage(), e);
        }

        return classesBuilder.build();
    } // scanClasses.

    /**
     * Scan classes for a given packages and annotated by annotatedBy parameter
     * @param annotatedBy {@link Class}
     * @param classLoader {@link ClassLoader}
     * @param packages    {@link String} array
     * @return  Collection
     */
    public static Collection<ScannedGroupBean> scanClasses (final Class<? extends Annotation> annotatedBy,
                                                 final ClassLoader classLoader,
                                                 final String ...packages) {

        return scanClasses(annotatedBy, null, classLoader, packages);
    } // scanClasses.

    public static Collection<ScannedGroupBean> scanClasses (final Class<? extends Annotation> annotatedBy,
                                                 final Class<?> instanceOf,
                                                 final ClassLoader classLoader,
                                                 final String ...packages) {

        Class clazz = null;
        ClassPath classPath = null;
        final ImmutableSet.Builder<ScannedGroupBean> scannedGroupBuilder = new ImmutableSet.Builder<>();
        ImmutableSet<ClassPath.ClassInfo> classes = null;
        Annotation annotation = null;

        try {

            classPath = ClassPath.from(classLoader);
            for (String mypackage : packages) {

                classes =
                        classPath.getTopLevelClassesRecursive(mypackage);
                for (ClassPath.ClassInfo classInfo : classes) {

                    clazz      = getClassFor(classInfo.getName());
                    annotation = getBeanAnnotation(clazz, annotatedBy);
                    if ((null != annotation) &&
                            (null == instanceOf || instanceOf.isAssignableFrom(clazz))) {

                        scannedGroupBuilder.add(new ScannedGroupBean(annotation, clazz));
                    }
                }
            }
        } catch (Exception e) {

            Logger.error(ClassPathUtils.class, "Error on ClassPathUtils: " + e.getMessage(), e);
        }

        return scannedGroupBuilder.build();
    } // scanClasses.
} // E:O:F:ClassPathUtils.
