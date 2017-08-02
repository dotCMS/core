package com.dotcms.util;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Scanned group for {@link ClassPathUtils#scanClasses(Class, Class, ClassLoader, String...)} method
 * @author jsanca
 */
public class ScannedGroupBean implements Serializable {

    private final Annotation annotation;
    private final Class clazz;

    public ScannedGroupBean(final Annotation annotation, final Class clazz) {
        this.annotation = annotation;
        this.clazz = clazz;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public Class getClazz() {
        return clazz;
    }
} // E:O:F:ScannedGroupBean.
