package com.dotcms.util;

import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseFilter;
import org.junit.Test;

import javax.inject.Singleton;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link ClassPathUtils} Test
 * @author jsanca
 */

public class ClassPathUtilsTest  {

    /**
     * Testing the new Instance
     *
     */
    @Test
    public void scanClassesTest()  {

        Collection<Class> classes = ClassPathUtils.scanClasses(Thread.currentThread().getContextClassLoader(),
                "com.dotcms.exception");

        assertNotNull(classes);
        assertTrue(classes.contains(ReflectionUtils.getClassFor("com.dotcms.exception.BaseInternationalizationException")));
        assertTrue(classes.contains(ReflectionUtils.getClassFor("com.dotcms.exception.BaseRuntimeInternationalizationException")));
        assertTrue(classes.contains(ReflectionUtils.getClassFor("com.dotcms.exception.InternationalizationExceptionSupport")));
    }

    @Test
    public void scanClassesAnnotationTest()  {

        final Collection<ScannedGroupBean> classes = ClassPathUtils.scanClasses(Singleton.class, Thread.currentThread().getContextClassLoader(),
                "com.dotcms.rest.annotation");

        assertNotNull(classes);

        boolean hasHeaderFilter = false;
        boolean hasNotRequestFilterCommand = true;

        for (ScannedGroupBean scannedGroupBean : classes) {

            if (scannedGroupBean.getClazz().getName().equals("com.dotcms.rest.annotation.HeaderFilter")) {

                hasHeaderFilter = true;
            }

            if (!scannedGroupBean.getClazz().getName().equals("com.dotcms.rest.annotation.RequestFilterCommand")) {

                hasNotRequestFilterCommand = false;
            }
        }

        assertTrue(hasHeaderFilter);
        assertFalse(hasNotRequestFilterCommand);
    }

    @Test
    public void scanClassesAnnotationAndInterfaceTest()  {

        final Collection<ScannedGroupBean> classes = ClassPathUtils.scanClasses(Singleton.class,
                ContainerResponseFilter.class, Thread.currentThread().getContextClassLoader(),
                "com.dotcms.rest.annotation");

        assertNotNull(classes);

        boolean hasHeaderFilter = false;
        boolean hasNotRequestFilterCommand = true;

        for (ScannedGroupBean scannedGroupBean : classes) {

            if (scannedGroupBean.getClazz().getName().equals("com.dotcms.rest.annotation.HeaderFilter")) {

                hasHeaderFilter = true;
            }

            if (!scannedGroupBean.getClazz().getName().equals("com.dotcms.rest.annotation.RequestFilterCommand")) {

                hasNotRequestFilterCommand = false;
            }
        }

        assertTrue(hasHeaderFilter);
        assertFalse(hasNotRequestFilterCommand);
    }

}


