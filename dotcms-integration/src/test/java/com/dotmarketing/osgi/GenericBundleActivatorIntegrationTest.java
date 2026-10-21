package com.dotmarketing.osgi;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.osgi.OSGIConstants;
import com.dotcms.saml.DotIdentityProviderConfigurationFactoryImpl;
import com.dotcms.saml.DotLoggerMessageObserver;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlServiceBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.VelocityUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.framework.OSGISystem;
import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
     * Verifies that the packaged SAML bundle starts, exposes the core service interface, and has
     * the same release version as the packaged Tika bundle.
     */
    @Test
    public void test_dotsaml_inits_properly() {
        assertNotNull(samlServiceBuilder());
        final OSGISystem system = OSGISystem.getInstance();
        final Bundle tika = system.findBundle("com.dotcms.tika");
        assertNotNull("Packaged Tika bundle", tika);
        assertEquals("SAML must be released with core, like Tika", tika.getVersion(),
                system.findBundle(OSGIConstants.BUNDLE_NAME_DOTCMS_SAML).getVersion());
    }

    /**
     * Sends a signed HTTP-POST fixture through the installed bundle, then repeats it with a fresh
     * request-state cookie so rejection must come from the real database-backed replay check.
     *
     * @throws Exception if the fixtures or replay table cannot be accessed
     */
    @Test
    public void test_dotsaml_accepts_signed_response_only_once() throws Exception {
        final SamlServiceBuilder builder = samlServiceBuilder();
        final SamlConfigurationService configuration = builder.buildSamlConfigurationService();
        configuration.initService(Collections.emptyMap());
        final MessageObserver observer = Mockito.spy(new DotLoggerMessageObserver());
        final Map<Object, Object> services = samlRegistry("com.dotcms.saml.utils.InstanceUtil", "instanceMap");
        final Map<Object, Object> handlers = samlRegistry("com.dotcms.saml.service.handler.AssertionResolverHandlerFactory",
                "assertionResolverHandlerInstancesMap");
        final Map<Object, Object> previousServices = new HashMap<>(services);
        final Map<Object, Object> previousHandlers = new HashMap<>(handlers);

        final IdentityProviderConfiguration idp = Mockito.mock(IdentityProviderConfiguration.class);
        Mockito.when(idp.isEnabled()).thenReturn(true);
        Mockito.when(idp.getId()).thenReturn("bundle-it");
        Mockito.when(idp.getIdpName()).thenReturn("Installed bundle test IdP");
        Mockito.when(idp.getSpIssuerURL()).thenReturn("https://saml-it.example/sp");
        Mockito.when(idp.getSpEndpointHostname()).thenReturn("saml-it.example");
        Mockito.when(idp.getSignatureValidationType()).thenReturn("assertion");
        Mockito.when(idp.getIdPMetadataFile()).thenReturn(IOUtils.toString(
                getClass().getResource("/osgi/saml/idp-metadata.xml"), StandardCharsets.UTF_8).toCharArray());

        final String assertion = IOUtils.toString(getClass().getResource("/osgi/saml/assertion.xml"),
                StandardCharsets.UTF_8);
        final String response = """
                <samlp:Response xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                    ID="_saml_bundle_it_response" Version="2.0" IssueInstant="%s"
                    Destination="https://saml-it.example/dotsaml/login/bundle-it"
                    InResponseTo="_saml_bundle_it_request">
                  <saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">https://saml-it.example/idp</saml:Issuer>
                  <samlp:Status><samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/></samlp:Status>
                  %s
                </samlp:Response>
                """.formatted(Instant.now(), assertion);
        final String encoded = Base64.getEncoder().encodeToString(response.getBytes(StandardCharsets.UTF_8));

        deleteSamlFixtureReplayRecord();
        try {
            final SamlAuthenticationService authentication = builder.buildAuthenticationService(
                    new DotIdentityProviderConfigurationFactoryImpl(APILocator.getAppsAPI(), APILocator.getHostAPI()),
                    VelocityUtil.getEngine(), observer, configuration);
            authentication.initService(Collections.emptyMap());
            assertEquals("saml-bundle-it@example.com", authentication.resolveAllAttributes(
                    samlPost(encoded), Mockito.mock(HttpServletResponse.class), idp).get("nameID"));
            assertTrue("Replaying a signed assertion must not authenticate", authentication.resolveAllAttributes(
                    samlPost(encoded), Mockito.mock(HttpServletResponse.class), idp).isEmpty());
            final ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
            Mockito.verify(observer).updateError(Mockito.anyString(), Mockito.anyString(), error.capture());
            assertEquals("Rejected SAML Response for IdP 'Installed bundle test IdP': "
                            + "The SAML Assertion '_saml_bundle_it_assertion' has already been used",
                    error.getValue().getMessage());
        } finally {
            services.clear();
            services.putAll(previousServices);
            handlers.clear();
            handlers.putAll(previousHandlers);
            deleteSamlFixtureReplayRecord();
        }
    }

    /**
     * Accesses a bundle-private registry solely to restore its original contents after the test;
     * plugin implementation classes must not be added to the integration-test classpath.
     *
     * @param className the class in the installed SAML bundle
     * @param fieldName the static registry field
     * @return the mutable registry whose contents the builder changes
     * @throws ReflectiveOperationException if the registry cannot be accessed
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Object> samlRegistry(final String className, final String fieldName)
            throws ReflectiveOperationException {
        final Class<?> type = OSGISystem.getInstance().findBundle(OSGIConstants.BUNDLE_NAME_DOTCMS_SAML)
                .loadClass(className);
        final Field registry = type.getDeclaredField(fieldName);
        registry.setAccessible(true);
        return (Map<Object, Object>) registry.get(null);
    }

    /** Returns the real builder registered by the active, packaged system bundle. */
    private static SamlServiceBuilder samlServiceBuilder() {
        final OSGISystem system = OSGISystem.getInstance();
        system.initializeFramework();
        final Bundle saml = system.findBundle(OSGIConstants.BUNDLE_NAME_DOTCMS_SAML);
        assertNotNull("Packaged SAML bundle", saml);
        assertEquals("SAML bundle must start", Bundle.ACTIVE, saml.getState());
        final SamlServiceBuilder builder = system.getService(SamlServiceBuilder.class,
                OSGIConstants.BUNDLE_NAME_DOTCMS_SAML);
        assertNotNull("SAML must expose the core SamlServiceBuilder interface", builder);
        return builder;
    }

    /** Creates a fresh browser request so a consumed cookie cannot mask assertion replay. */
    private static HttpServletRequest samlPost(final String encodedResponse) {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getParameter("SAMLResponse")).thenReturn(encodedResponse);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("__Host-dotsaml_req_bundle-it",
                "_saml_bundle_it_request:" + Instant.now().getEpochSecond())});
        return request;
    }

    /** Deletes only this fixture's replay record, including after a failed or interrupted run. */
    private static void deleteSamlFixtureReplayRecord() throws Exception {
        try (Connection connection = DbConnectionFactory.getDataSource().getConnection();
                ResultSet table = connection.getMetaData().getTables(null, null,
                        "dotsaml_assertion_replay", new String[]{"TABLE"})) {
            connection.setAutoCommit(true);
            if (table.next()) {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM dotsaml_assertion_replay WHERE assertion_key = ?")) {
                    delete.setString(1, DigestUtils.sha256Hex(
                            "https://saml-it.example/idp|_saml_bundle_it_assertion"));
                    delete.executeUpdate();
                }
            }
        }
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
