package com.dotcms.util;


import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Utils for Annotations
 * @author jsanca
 */
public class AnnotationUtils {

    private static final String NULL = "NULL";

    private static final Map<MultiKey, Object> beansAnnotatedByCacheMap =
            new ConcurrentHashMap<>(256);

    private static final Map<MultiKey, String []> attributesAnnotatedByCacheMap =
            new ConcurrentHashMap<>(256);

    private AnnotationUtils(){
        throw new IllegalStateException("Utility class");
    }


    /**
     * Determinate if the bean is annotated by specified annotation
     * @param bean Object
     * @param annotationType Class
     * @return boolean
     */
    public static boolean isBeanAnnotatedBy (final Object bean, final Class<? extends Annotation> annotationType) {

        return isBeanAnnotatedBy(bean.getClass(), annotationType);
    } // isBeanAnnotatedBy.

    /**
     * Determinate if the bean is annotated by specified annotation
     * @param beanClass Class
     * @param annotationType Class
     * @return boolean
     */
    public static boolean isBeanAnnotatedBy (final Class beanClass, final Class<? extends Annotation> annotationType) {

        for (final Annotation annotation : Arrays.asList(beanClass.getDeclaredAnnotations())) {
            if (annotation.annotationType().equals(annotationType)) {
                return true;
            }
        }
        return false;
    } // isBeanAnnotatedBy.

    /**
     * Returns the annotation for the beanClass, if the bean is not annotated returns null.
     * @param beanClass Class
     * @param annotationType Class
     * @return boolean
     */
    public static <T extends Annotation> T getBeanAnnotation (final Class beanClass, final Class<T> annotationType) {

        T annotationFound   = null;

        if (null != beanClass) {

            final MultiKey multiKey =
                    new MultiKey(beanClass, annotationType);

            if (beansAnnotatedByCacheMap.containsKey(multiKey)) {

                if (!NULL.equals(beansAnnotatedByCacheMap.get(multiKey))) {
                    annotationFound =
                            (T) beansAnnotatedByCacheMap.get(multiKey);
                }
            } else {

                for (final Annotation annotation : Arrays.asList(beanClass.getDeclaredAnnotations())) {
                    if (annotation.annotationType().equals(annotationType)) {
                        annotationFound = (T) annotation;
                    }
                }

                beansAnnotatedByCacheMap.put(multiKey,
                        (null != annotationFound) ? annotationFound : NULL);
            }
        }

        return annotationFound;
    } // getBeanAnnotation.


    /**
     * Get the Attributes names annotated by annotationType
     * @param bean Object
     * @param annotationType Class
     * @return array of string with the attr names
     */
    public static String [] getAttributesAnnotatedBy (final Object bean, final Class<? extends Annotation> annotationType) {

        return getAttributesAnnotatedBy(bean.getClass(), annotationType);
    } // getAttributesAnnotatedBy.

    /**
     * Get the Attributes names annotated by annotationType
     * @param beanClass Class
     * @param annotationType Class
     * @return array of string with the attr names
     */
    public static String [] getAttributesAnnotatedBy (final Class beanClass, final Class<? extends Annotation> annotationType) {

        final MultiKey multiKey =
                new MultiKey(beanClass, annotationType);
        String [] attributesAnnotated = null;

        if (attributesAnnotatedByCacheMap.containsKey(multiKey)) {

            attributesAnnotated =
                    attributesAnnotatedByCacheMap.get(multiKey);
        } else {

            final List<String> fieldList = new ArrayList<>();
            for (final Field field : beanClass.getDeclaredFields()) {

                if (isFieldAnnotatedBy(field, annotationType)) {

                    fieldList.add(field.getName());
                }
            }

            attributesAnnotated =
                    fieldList.toArray(new String [] {});

            attributesAnnotatedByCacheMap.put(multiKey, attributesAnnotated);
        }

        return attributesAnnotated;
    } // getAttributesAnnotatedBy.

    /**
     * Returns true if the field is annotated by annotationType
     * @param fieldName String
     * @param beanClass Class
     * @param annotationType Class
     * @return boolean true if it is annotated by annotationType
     */
    public static boolean isFieldAnnotatedBy (final String fieldName, final Class beanClass,
                                              final Class<? extends Annotation> annotationType) {

        Field field = null;

        try {

            field = beanClass.getDeclaredField(fieldName);
            return isFieldAnnotatedBy(field, annotationType);
        } catch (NoSuchFieldException e) {
            // Quiet
        }

        return false;
    } // isFieldAnnotatedBy.

    /**
     * Get the Annotation for the field, null if does not exists.
     * @param fieldName Object
     * @param beanClass Class
     * @param annotationType Class
     * @return array of string with the attr names
     */
    public static <T extends Annotation> T getAttributeAnnotation (final String fieldName, final Class beanClass,
                                                                   final Class<T> annotationType) {

        Field field = null;

        try {

            field = beanClass.getDeclaredField(fieldName);
            return (T)getAttributeAnnotation(field, annotationType);
        } catch (NoSuchFieldException e) {
            // Quiet
        }

        return null;
    } // getAttributeAnnotation.

    /**
     * Get the Annotation for the field, null if does not exists.
     * @param field Object
     * @param annotationType Class
     * @return array of string with the attr names
     */
    public static Annotation getAttributeAnnotation (final Field field, final Class<? extends Annotation> annotationType) {

        final Annotation [] annotations = field.getDeclaredAnnotations();

        for (final Annotation annotation : annotations) {

            if (annotation.annotationType().equals(annotationType)) {

                return annotation;
            }
        }

        return null;
    } // getAttributeAnnotation.

    /**
     * Returns true if the field is annotated by annotationType
     * @param field
     * @param annotationType
     * @return boolean true if it is annotated by annotationType
     */
    public static boolean isFieldAnnotatedBy (final Field field, final Class<? extends Annotation> annotationType) {

        final Annotation [] annotations = field.getDeclaredAnnotations();

        for (final Annotation annotation : annotations) {

            if (annotation.annotationType().equals(annotationType)) {

                return true;
            }
        }

        return false;
    } // isFieldAnnotatedBy.

    /**
     * Returns true if the method is annotated by annotationType
     * @param method Methos
     * @param annotationType Class
     * @return boolean true if it is annotated by annotationType
     */
    public static boolean isMethodAnnotatedBy(final Method method, final Class<? extends Annotation> annotationType) {

        final Annotation [] annotations = method.getDeclaredAnnotations();

        for (final Annotation annotation : annotations) {

            if (annotation.annotationType().equals(annotationType)) {

                return true;
            }
        }

        return false;
    } // isMethodAnnotatedBy.

    /**
     * Gets the Annotation for the method, null if does not exists.
     * @param method Method
     * @param annotationType Class
     * @return Annotation
     */
    public static <T extends Annotation> T getMethodAnnotation(final Method method, final Class<T> annotationType) {

        final Annotation [] annotations = method.getDeclaredAnnotations();

        for (final Annotation annotation : annotations) {

            if (annotation.annotationType().equals(annotationType)) {

                return (T)annotation;
            }
        }

        return null;
    } // getMethodAnnotatedBy.

    /**
     * Get all methods annotated by annotationType, not looking for super class. Just the subclass
     * @param object {@link Object}
     * @param annotationType {@link Class}
     * @return Set returns an immutable set with the methods annotated by annotationType
     */
    public static Set<Method> getMethodsAnnotatedBy (final Object object,
                                                     final Class<? extends Annotation> annotationType) {

        final ImmutableSet.Builder<Method> setBuilder =
            new ImmutableSet.Builder<>();
        final Class clazz = object.getClass();

        if (null != clazz) {

            for (final Method method : clazz.getMethods()) {

                if (isMethodAnnotatedBy(method, annotationType)) {

                    setBuilder.add(method);
                }
            }
        }

        return setBuilder.build();
    } // getMethodsAnnotatedBy.

    /**
     * Scan classes annotated by "annotationType" into the package "aPackage"
     * @param aPackage {@link Package}
     * @param annotationType {@link Class} of annotation
     * @return Set of Classes
     */
    public static Set<Class<?>> scanClassAnnotatedBy(final Package aPackage, final Class<? extends Annotation> annotationType) {

        return scanClassAnnotatedBy(aPackage.getName(), annotationType);
    }


    /**
     * Scan classes annotated by "annotationType" into the package "aPackage"
     * @param aPackage {@link Package} the package to find into the classpath
     * @param classLoader {@link ClassLoader} an specific class loader to do the scanning
     * @param annotationType {@link Class} of annotation
     * @return Set of Classes
     */
    public static Set<Class<?>> scanClassAnnotatedBy(final Package aPackage, final ClassLoader classLoader, final Class<? extends Annotation> annotationType) {

        return scanClassAnnotatedBy(aPackage.getName(), classLoader, annotationType);
    }

    /**
     * Scan classes annotated by "annotationType" into the package "basePackage"
     * @param basePackage {@link String} the package to find into the classpath
     * @param annotationType {@link Class} of annotation
     * @return Set of Classes
     */
    public static Set<Class<?>> scanClassAnnotatedBy(final String basePackage, final Class<? extends Annotation> annotationType) {

        return new Reflections(basePackage).getTypesAnnotatedWith(annotationType);
    }

    /**
     * Scan classes annotated by "annotationType" into the package "basePackage"
     * @param basePackage {@link String} the package to find into the classpath
     * @param classLoader {@link ClassLoader} an specific class loader to do the scanning
     * @param annotationType {@link Class} of annotation
     * @return Set of Classes
     */
    public static Set<Class<?>> scanClassAnnotatedBy(final String basePackage, final ClassLoader classLoader,
                                              final Class<? extends Annotation> annotationType) {

        return new Reflections(basePackage, classLoader).getTypesAnnotatedWith(annotationType);
    }

} // E:O:F:AnnotationUtils,
