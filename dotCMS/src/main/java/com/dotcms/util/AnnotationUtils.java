package com.dotcms.util;

import com.dotcms.repackage.org.apache.commons.collections.keyvalue.MultiKey;
import com.dotcms.repackage.org.hibernate.validator.internal.util.ConcurrentReferenceHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utils for Annotations
 * @author jsanca
 */
public class AnnotationUtils {

    private static final Map<MultiKey, String []> attributesAnnotatedByCacheMap =
            new ConcurrentReferenceHashMap<>(256);

    /**
     * Determinate if the bean is annotated by specified annotation
     * @param bean Object
     * @param annotationType Class
     * @return boolean
     */
    public boolean isBeanAnnotatedBy (Object bean, Class<? extends Annotation> annotationType) {

        return this.isBeanAnnotatedBy(bean.getClass(), annotationType);
    } // isBeanAnnotatedBy.

    /**
     * Determinate if the bean is annotated by specified annotation
     * @param beanClass Class
     * @param annotationType Class
     * @return boolean
     */
    public boolean isBeanAnnotatedBy (Class beanClass, Class<? extends Annotation> annotationType) {

        for (Annotation annotation : Arrays.asList(beanClass.getDeclaredAnnotations())) {
            if (annotation.annotationType().equals(annotationType)) {
                return true;
            }
        }
        return false;
    } // isBeanAnnotatedBy.

    /**
     * Get the Attributes names annotated by annotationType
     * @param bean
     * @param annotationType
     * @return array of string with the attr names
     */
    public String [] getAttributesAnnotatedBy (Object bean, Class<? extends Annotation> annotationType) {

        return this.getAttributesAnnotatedBy(bean.getClass(), annotationType);
    } // getAttributesAnnotatedBy.

    /**
     * Get the Attributes names annotated by annotationType
     * @param beanClass
     * @param annotationType
     * @return array of string with the attr names
     */
    public String [] getAttributesAnnotatedBy (Class beanClass, Class<? extends Annotation> annotationType) {

        final MultiKey multiKey =
                new MultiKey(beanClass, annotationType);
        String [] attributesAnnotated = null;

        if (attributesAnnotatedByCacheMap.containsKey(multiKey)) {

            attributesAnnotated =
                    attributesAnnotatedByCacheMap.get(multiKey);
        } else {

            final List<String> fieldList = new ArrayList<String>();
            for (Field field : beanClass.getDeclaredFields()) {

                if (this.isFieldAnnotatedBy(field, annotationType)) {

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
     * @param field
     * @param annotationType
     * @return boolean true if it is annotated by annotationType
     */
    public boolean isFieldAnnotatedBy (Field field, Class<? extends Annotation> annotationType) {

        final Annotation [] annotations = field.getDeclaredAnnotations();

        for (Annotation annotation : annotations) {

            if (annotation.annotationType().equals(annotationType)) {

                return true;
            }
        }

        return false;
    } // isFieldAnnotatedBy.

    public boolean isMethodAnnotatedBy(Method method, Class<? extends Annotation> annotationType) {

        final Annotation [] annotations = method.getDeclaredAnnotations();

        for (Annotation annotation : annotations) {

            if (annotation.annotationType().equals(annotationType)) {

                return true;
            }
        }

        return false;
    }

    public Annotation getMethodAnnotatedBy(Method method, Class<? extends Annotation> annotationType) {

        final Annotation [] annotations = method.getDeclaredAnnotations();

        for (Annotation annotation : annotations) {

            if (annotation.annotationType().equals(annotationType)) {

                return annotation;
            }
        }

        return null;
    }

} // E:O:F:AnnotationUtils,
