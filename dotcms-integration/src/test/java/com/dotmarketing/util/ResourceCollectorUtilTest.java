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

    /**
     * Test Java version extraction from a JAR file
     */
    @Test
    public void test_getJavaVersion() {
        // Test with fragment JAR
        final File fragmentJarFile = new File("./src/main/resources/osgi-bundle/com.dotcms.actionlet.fragment-0.2.jar");
        if (fragmentJarFile.exists()) {
            final ResourceCollectorUtil.JavaVersionInfo versionInfo =
                    ResourceCollectorUtil.getJavaVersion(fragmentJarFile);

            Assert.assertNotNull("JavaVersionInfo should not be null", versionInfo);
            Assert.assertNotNull("Java version string should not be null", versionInfo.getJavaVersion());
            Assert.assertFalse("Java version should not be Unknown", "Unknown".equals(versionInfo.getJavaVersion()));

            // Should have class major version (most reliable method)
            if (versionInfo.getClassMajorVersion() != null) {
                Assert.assertTrue("Class major version should be >= 45",
                        versionInfo.getClassMajorVersion() >= 45);
                System.out.println("Fragment JAR Java version: " + versionInfo.getJavaVersion() +
                        " (class version: " + versionInfo.getClassMajorVersion() + ")");
            }
        }

        // Test with bundle JAR
        final File bundleJarFile = new File("./src/main/resources/osgi-bundle/com.dotcms.actionlet-0.2.jar");
        if (bundleJarFile.exists()) {
            final ResourceCollectorUtil.JavaVersionInfo versionInfo =
                    ResourceCollectorUtil.getJavaVersion(bundleJarFile);

            Assert.assertNotNull("JavaVersionInfo should not be null", versionInfo);
            Assert.assertNotNull("Java version string should not be null", versionInfo.getJavaVersion());

            System.out.println("Bundle JAR Java version: " + versionInfo.getJavaVersion() +
                    " (class version: " + versionInfo.getClassMajorVersion() + ")" +
                    " (multi-release: " + versionInfo.isMultiRelease() + ")");
        }
    }

    /**
     * Test Java version detection with null/non-existent file
     */
    @Test
    public void test_getJavaVersion_nullFile() {
        final ResourceCollectorUtil.JavaVersionInfo versionInfo =
                ResourceCollectorUtil.getJavaVersion(null);

        Assert.assertNotNull("JavaVersionInfo should not be null even for null file", versionInfo);
        Assert.assertNull("Class major version should be null", versionInfo.getClassMajorVersion());
        Assert.assertFalse("Should not be multi-release", versionInfo.isMultiRelease());
    }

    /**
     * Test Java version detection with non-existent file
     */
    @Test
    public void test_getJavaVersion_nonExistentFile() {
        final File nonExistentFile = new File("/tmp/non-existent-file-" + System.currentTimeMillis() + ".jar");
        final ResourceCollectorUtil.JavaVersionInfo versionInfo =
                ResourceCollectorUtil.getJavaVersion(nonExistentFile);

        Assert.assertNotNull("JavaVersionInfo should not be null", versionInfo);
        Assert.assertNull("Class major version should be null for non-existent file", versionInfo.getClassMajorVersion());
    }

    /**
     * Test Java version detection with dotcdn plugin
     * This validates the fix for OSGi bundle Java version extraction
     */
    @Test
    public void test_getJavaVersion_dotcdnPlugin() {
        final File dotcdnJar = new File("/Users/stevebolton/git/com.dotcms.dotcdn/jars/22.10/com.dotcms.dotcdn-1.0.jar");
        if (dotcdnJar.exists()) {
            final ResourceCollectorUtil.JavaVersionInfo versionInfo =
                    ResourceCollectorUtil.getJavaVersion(dotcdnJar);

            Assert.assertNotNull("JavaVersionInfo should not be null", versionInfo);
            Assert.assertNotNull("Manifest version should not be null", versionInfo.getManifestVersion());
            Assert.assertNotNull("Class major version should not be null", versionInfo.getClassMajorVersion());
            Assert.assertNotNull("Java version should not be null", versionInfo.getJavaVersion());

            // This plugin was compiled with Java 11 (manifest) but targets Java 8 bytecode (class version 52)
            Assert.assertTrue("Manifest version should contain 11",
                    versionInfo.getManifestVersion().contains("11"));
            Assert.assertEquals("Class major version should be 52 (Java 8)", Integer.valueOf(52), versionInfo.getClassMajorVersion());
            Assert.assertEquals("Java version should be Java 8", "Java 8", versionInfo.getJavaVersion());
            Assert.assertFalse("Should not be multi-release", versionInfo.isMultiRelease());
            Assert.assertTrue("Should be compatible with Java 21", versionInfo.isCompatibleWithJava21());
            Assert.assertFalse("Should not require Java 11+ (targets Java 8 bytecode)", versionInfo.requiresJava11OrHigher());

            System.out.println("dotcdn Plugin Java version: " + versionInfo.getJavaVersion() +
                    " (manifest: " + versionInfo.getManifestVersion() + ")" +
                    " (class version: " + versionInfo.getClassMajorVersion() + ")" +
                    " (multi-release: " + versionInfo.isMultiRelease() + ")");
        } else {
            System.out.println("Skipping dotcdn plugin test - file not found: " + dotcdnJar.getAbsolutePath());
        }
    }

    /**
     * Test Maven metadata extraction
     * Validates extraction of isBuiltWithMaven flag and dotcms-core dependency version
     */
    @Test
    public void test_getMavenInfo() {
        // Test with dotcdn plugin - it doesn't have Maven metadata
        final File dotcdnJar = new File("/Users/stevebolton/git/com.dotcms.dotcdn/jars/22.10/com.dotcms.dotcdn-1.0.jar");
        if (dotcdnJar.exists()) {
            final ResourceCollectorUtil.MavenInfo mavenInfo =
                    ResourceCollectorUtil.getMavenInfo(dotcdnJar);

            Assert.assertNotNull("MavenInfo should not be null", mavenInfo);

            if (mavenInfo.isBuiltWithMaven()) {
                System.out.println("dotcdn Plugin was built with Maven");
                if (mavenInfo.getDotcmsCoreDependencyVersion() != null) {
                    System.out.println("  dotcms-core dependency: " + mavenInfo.getDotcmsCoreDependencyVersion());
                }
            } else {
                System.out.println("dotcdn plugin was not built with Maven");
            }
        } else {
            System.out.println("Skipping Maven info test - file not found: " + dotcdnJar.getAbsolutePath());
        }
    }

    /**
     * Test Maven metadata extraction with null file
     */
    @Test
    public void test_getMavenInfo_nullFile() {
        final ResourceCollectorUtil.MavenInfo mavenInfo =
                ResourceCollectorUtil.getMavenInfo(null);

        Assert.assertNotNull("MavenInfo should not be null even for null file", mavenInfo);
        Assert.assertFalse("Should not be built with Maven", mavenInfo.isBuiltWithMaven());
        Assert.assertNull("DotcmsCoreDependencyVersion should be null", mavenInfo.getDotcmsCoreDependencyVersion());
    }

    /**
     * Test Maven metadata extraction with non-Maven JAR
     */
    @Test
    public void test_getMavenInfo_nonMavenJar() {
        // Test with bundle JAR that doesn't have Maven metadata
        final File bundleJarFile = new File("./src/main/resources/osgi-bundle/com.dotcms.actionlet-0.2.jar");
        if (bundleJarFile.exists()) {
            final ResourceCollectorUtil.MavenInfo mavenInfo =
                    ResourceCollectorUtil.getMavenInfo(bundleJarFile);

            Assert.assertNotNull("MavenInfo should not be null", mavenInfo);
            Assert.assertFalse("Should not be built with Maven", mavenInfo.isBuiltWithMaven());
        }
    }


}
