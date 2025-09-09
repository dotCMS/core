package com.dotmarketing.osgi;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.felix.framework.OSGISystem;
import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

public class GenericBundleActivatorIntegrationTest {

    /**
     * Sets up OSGI and makes sure the framework has started
     *
     * @throws Exception
     */
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        // ✅ Ensure OSGI framework is fully initialized
        Framework framework = OSGIUtil.getInstance().initializeFramework();

        // ✅ Wait for framework to be active
        long timeout = System.currentTimeMillis() + 30000; // 30 seconds
        while (framework.getState() != Framework.ACTIVE && System.currentTimeMillis() < timeout) {
            Thread.sleep(100);
        }

        if (framework.getState() != Framework.ACTIVE) {
            throw new IllegalStateException("OSGI Framework failed to start within 30 seconds");
        }

        // ✅ Give bundles additional time to load
        Thread.sleep(2000);
    }
    /**
     * Method to test: {@link OSGIUtil#initializeFramework()}
     * Given Scenario: The OSGI framework is initialized multiple times
     * Expected Result: The framework is initialized successfully only once
     */
    @Test
    public void test_framework_inits_properly() {
        Framework framework = OSGIUtil.getInstance().initializeFramework();
        Framework framework2 = OSGIUtil.getInstance().initializeFramework();

        assertEquals(framework.getState(), Framework.ACTIVE);
        assertEquals(framework, framework2);

    }

    /**
     * Method to test: {@link BundleContext#getBundles()}
     * Given Scenario: OSGI inits and bundles are requested
     * Expected Result: Default bundles are installed successfully
     */
    @Test
    public void test_osgi_inits_properly() {
        final BundleContext context = HostActivator.instance().getBundleContext();

        // ✅ Wait for bundles to be loaded with Awaitility
        await()
                .atMost(15, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .untilAsserted(() -> {
                    Bundle[] bundles = context.getBundles();
                    assertNotNull("Bundles array should not be null", bundles);
                    assertTrue(
                            String.format("Expected at least 4 bundles, but found %d",
                                    bundles.length),
                            bundles.length >= 4
                    );
                });
    }

    /**
     * Method to test: {@link BundleContext#getBundles()}
     * Given Scenario: SAML bundle initialization is validated
     * Expected Result: SAML bundle is running successfully
     */
    @Test
    @Ignore
    public void test_dotsaml_inits_properly() {
        BundleContext context = HostActivator.instance().getBundleContext();

        Bundle[] bundles = context.getBundles();
        Optional<Bundle> dotSAML = Arrays.asList(bundles)
                .stream()
                .filter(b -> b.getSymbolicName().equals("com.dotcms.samlbundle"))
                .findFirst();


        assert (dotSAML.isPresent());
        assertEquals(Bundle.ACTIVE, dotSAML.get().getState());

    }

    /**
     * Method to test: {@link BundleContext#getBundles()}
     * Given Scenario: Tika bundle initialization is validated
     * Expected Result: Tika bundle is running successfully
     */
    @Test
    public void test_tika_inits_properly() {
        Bundle[] bundles = OSGISystem.getInstance().getBundles();

        Optional<Bundle> dotTika = Arrays.asList(bundles)
                .stream()
                .filter(b -> b.getSymbolicName().equals("com.dotcms.tika"))
                .findFirst();

        assert (dotTika.isPresent());
        assertEquals(dotTika.get().getState(), Bundle.ACTIVE);

    }

    /**
     * Method to test: {@link GenericBundleActivator#overrideClasses(BundleContext)}
     * Given Scenario: A new class is being injected in the classloader through a plugin
     * Expected Result: The class is injected in the classloader successfully
     */
    @Test
    public void test_class_injection() throws Exception {

        ByteBuddyAgent.install();
        final BundleContext context = Mockito.spy(HostActivator.instance().getBundleContext());
        final OverrideActivator activator = Mockito.spy(new OverrideActivator());

        final URL url = Thread.currentThread().getContextClassLoader()
                .getResource("osgi/plugin/DummyPlugin-1.0.jar");
        final URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url});

        Mockito.doReturn(urlClassLoader).when(activator).getBundleClassloader();
        Mockito.doReturn(new Bundle[]{}).when(context).getBundles();
        activator.start(context);

        final Class myClass = Class.forName("my.dummy.plugin.MyCustomClass", true, urlClassLoader);
        final Method method = ClassLoader.class
                .getDeclaredMethod("findLoadedClass", new Class[]{String.class});
        method.setAccessible(true);
        Object result = method.invoke(activator.getWebAppClassloader(), myClass.getName());

        //Before injection
        assertNull(result);

        activator.overrideClass(myClass);

        //After injection
        result = method.invoke(activator.getWebAppClassloader(), myClass.getName());
        assertNotNull(result);
        assertTrue(result.toString().contains("my.dummy.plugin.MyCustomClass"));

    }


    /**
     * Method to test: {@link GenericBundleActivator#overrideClasses(BundleContext)}
     * Given Scenario: An existing class is overloaded through a plugin
     * Expected Result: The new version of the class is injected in the classloader successfully
     */
    @Test
    public void test_class_overloading() throws Exception {

        ByteBuddyAgent.install();
        final BundleContext context = Mockito.spy(HostActivator.instance().getBundleContext());
        final OverrideActivator activator = Mockito.spy(new OverrideActivator());

        final URL url = Thread.currentThread().getContextClassLoader()
                .getResource("osgi/plugin/DummyPlugin-1.0.jar");
        final URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url});

        Mockito.doReturn(urlClassLoader).when(activator).getBundleClassloader();
        Mockito.doReturn(new Bundle[]{}).when(context).getBundles();
        activator.start(context);

        MyDummyClass instance = new MyDummyClass();
        //Before injection
        assertEquals("MyDummyClass", instance.classLoaded());

        //After injection
        final Class myPluginClass = Class.forName("com.dotmarketing.osgi.MyDummyClass", true, urlClassLoader);
        activator.overrideClass(myPluginClass);

        final Class myInjectedClass = Class.forName("com.dotmarketing.osgi.MyDummyClass", true, activator.getWebAppClassloader());
        Object newInstance = myInjectedClass.getDeclaredConstructor().newInstance();

        Method method = myInjectedClass.getMethod("classLoaded");
        String result = (String) method.invoke(newInstance);

        assertEquals("MyClassReloaded", result);

    }

    private class OverrideActivator extends GenericBundleActivator {

        @Override
        public void start(BundleContext bundleContext) throws Exception {

            // Initializing services...
            initializeServices(bundleContext);
        }

        @Override
        public void stop(BundleContext bundleContext) throws Exception {
            unregisterConditionlets();
        }
    }

    @SuppressWarnings("unused")
    public static class MyDummyClass {
        public String classLoaded(){
            return "MyDummyClass";
        }
    }
}
