package com.dotmarketing.util;

import java.io.File;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

public class ResourceCollectorUtilTest {

    /**
     * Get the packages for a fragment
     */
    @Test
    public void test_getPackages_fragment() {

        final File fragmentJarFile = new File("./src/main/resources/osgi-bundle/com.dotcms.actionlet.fragment-0.2.jar");
        if (fragmentJarFile.exists()) {

            final Collection<String> exportPackages = ResourceCollectorUtil.getPackages(fragmentJarFile);
            Assert.assertNotNull(exportPackages);
            Assert.assertTrue(exportPackages.size()>0);
            Assert.assertEquals(8, exportPackages.size());
            Assert.assertTrue(exportPackages.contains("com.dotmarketing.portlets.workflows.model;version=0"));
        }
    }

    /**
     * Get the packages for a bundle
     */
    @Test
    public void test_getPackages_bundle() {

        final File fragmentJarFile = new File("./src/main/resources/osgi-bundle/com.dotcms.actionlet-0.2.jar");
        if (fragmentJarFile.exists()) {

            final Collection<String> importPackages = ResourceCollectorUtil.getPackages(fragmentJarFile);
            Assert.assertNotNull(importPackages);
            Assert.assertTrue(importPackages.size()>0);
            Assert.assertEquals(8, importPackages.size());
            Assert.assertTrue(importPackages.contains("com.dotmarketing.portlets.workflows.model;version=0"));
        }
    }
    
    
    
    /**
     * Get the packages for a bundle
     */
    @Test
    public void test_getPackages_bundle_version_range() {

        
        final String packageString ="com.liferay.portal.model,com.liferay.portal.util,io.vavr;version=\"[0.10,1)\",javax.servlet.http;version=\"[3.1,4)\",org.apache.velocity,org.apache.velocity.app,org.apache.velocity.context,org.apache.velocity.exception,org.apache.velocity.runtime,org.apache.velocity.runtime.directive,org.apache.velocity.runtime.parser.node,org.osgi.framework;version=\"[1.8,2)\"";
        
        Collection<String> importPackages = ResourceCollectorUtil.getPackages(packageString);
        Assert.assertNotNull(importPackages);
        Assert.assertTrue(importPackages.size()>0);
        Assert.assertEquals(12, importPackages.size());
        Assert.assertTrue(importPackages.contains("io.vavr;version=0.10"));
        Assert.assertTrue(importPackages.contains("org.osgi.framework;version=1.8"));

    }
    
    
    
}
