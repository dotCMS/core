package com.dotcms.util;

import com.dotcms.util.annotationtest.TestActionlet1;
import com.dotcms.util.annotationtest.TestActionlet2;
import com.dotcms.util.annotationtest.TestJsonSerializer;
import com.dotcms.util.annotationtest.TestResource;
import com.dotmarketing.portlets.workflows.actionlet.Actionlet;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Path;
import java.util.Set;

public class AnnotationUtilsTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_scanClassAnnotatedBy_Actionlet_found(){

        final Set<Class<?>> annotatedClasses = AnnotationUtils.scanClassAnnotatedBy("com.dotcms.util.annotationtest", Actionlet.class);
        Assert.assertNotNull(annotatedClasses);
        Assert.assertEquals(2, annotatedClasses.size());
        Assert.assertTrue(annotatedClasses.contains(TestActionlet1.class));
        Assert.assertTrue(annotatedClasses.contains(TestActionlet2.class));
    }

    @Test
    public void test_scanClassAnnotatedBy_JsonSerializer_found(){

        final Set<Class<?>> annotatedClasses = AnnotationUtils.scanClassAnnotatedBy("com.dotcms.util.annotationtest", JsonSerialize.class);
        Assert.assertNotNull(annotatedClasses);
        Assert.assertEquals(1, annotatedClasses.size());
        Assert.assertTrue(annotatedClasses.contains(TestJsonSerializer.class));
    }

    @Test
    public void test_scanClassAnnotatedBy_Path_found(){

        final Set<Class<?>> annotatedClasses = AnnotationUtils.scanClassAnnotatedBy("com.dotcms.util.annotationtest", Path.class);
        Assert.assertNotNull(annotatedClasses);
        Assert.assertEquals(1, annotatedClasses.size());
        Assert.assertTrue(annotatedClasses.contains(TestResource.class));
    }

    @Test
    public void test_scanClassAnnotatedBy_not_found(){

        final Set<Class<?>> annotatedClasses = AnnotationUtils.scanClassAnnotatedBy("com.dotcms.util.annotationtest", JsonDeserialize.class);
        Assert.assertNotNull(annotatedClasses);
        Assert.assertEquals(0, annotatedClasses.size());
    }

    @Test
    public void test_scanClassAnnotatedBy_not_valid_package(){

        final Set<Class<?>> annotatedClasses = AnnotationUtils.scanClassAnnotatedBy("com.dotcms.util.xxxx", JsonDeserialize.class);
        Assert.assertNotNull(annotatedClasses);
        Assert.assertEquals(0, annotatedClasses.size());
    }

}
