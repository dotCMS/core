package com.dotcms.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.rendering.velocity.viewtools.VelocitySessionWrapper;
import com.dotmarketing.util.Config;
import org.apache.velocity.util.introspection.SecureIntrospectorImpl;
import org.junit.Test;

/**
 * Contract test for the Velocity {@link SecureIntrospectorImpl} restricted-class coverage.
 *
 * <p>The introspector gates every method call a Velocity template may perform. It is
 * constructed with the same restricted class/package lists that ship in
 * {@code org/apache/velocity/runtime/defaults/velocity.properties}. Those lists are
 * exact-string matched, so the code-level check must independently deny method access on
 * the file, IO, and network-resource type families (including their concrete platform
 * implementations, reached by type hierarchy) that the string lists do not enumerate.
 *
 * <p>This test pins that contract: the file/IO/network-resource families are denied, the
 * reflection/system families stay denied, and the common value types stay allowed. It is a
 * plain unit test (no Velocity engine or services required) mirroring the sibling
 * {@code SstiGetUrlReproTest}.
 */
public class SecureIntrospectorImplTest {

    /**
     * The class list exactly as shipped in velocity.properties today. It intentionally does
     * NOT enumerate the file/IO/network-resource families — proving the denial below comes
     * from the code-level (hierarchy-aware) check, not from these strings.
     */
    private static final String[] SHIPPED_BAD_CLASSES = {
            "java.lang.Class", "java.lang.ClassLoader",
            "org.apache.velocity.app.VelocityEngine", "org.apache.velocity.runtime.RuntimeInstance",
            "org.apache.velocity.util.introspection.SecureUberspector",
            "org.apache.velocity.util.introspection.SecureUberspectorImpl",
            "java.lang.Compiler", "java.lang.InheritableThreadLocal", "java.lang.Package",
            "java.lang.Process", "java.lang.Runtime", "java.lang.RuntimePermission",
            "java.lang.SecurityManager", "java.lang.System", "java.lang.Thread",
            "java.lang.ThreadGroup", "java.lang.ThreadLocal", "java.lang.ProcessBuilder",
            "java.lang.Reflect", "javax.management.MBeanServer", "java.net.Socket",
            "javax.script.ScriptEngine", "javax.script.ScriptEngineManager"
    };

    private static final String[] SHIPPED_BAD_PACKAGES = { "java.lang.reflect" };

    private SecureIntrospectorImpl introspector() {
        return new SecureIntrospectorImpl(SHIPPED_BAD_CLASSES, SHIPPED_BAD_PACKAGES);
    }

    private void assertDenied(final Class<?> clazz, final String method) {
        assertFalse("Introspector must deny " + clazz.getName() + "#" + method,
                introspector().checkObjectExecutePermission(clazz, method));
    }

    private void assertAllowed(final Class<?> clazz, final String method) {
        assertTrue("Introspector must allow " + clazz.getName() + "#" + method,
                introspector().checkObjectExecutePermission(clazz, method));
    }

    /**
     * The file, IO, and network-resource type families must be denied — including a concrete
     * {@link Path} implementation resolved from the platform file system, which the exact-string
     * lists never name and only a hierarchy-aware check can cover.
     */
    @Test
    public void file_io_and_network_resource_families_are_denied() {
        assertDenied(File.class, "toPath");
        assertDenied(File.class, "getCanonicalPath");

        assertDenied(Path.class, "toFile");
        // Concrete platform Path implementation (e.g. sun.nio.fs.UnixPath) — reached only via hierarchy.
        final Class<?> concretePath = Paths.get("x").getClass();
        assertDenied(concretePath, "toFile");

        assertDenied(java.io.RandomAccessFile.class, "readFully");
        assertDenied(java.io.RandomAccessFile.class, "write");

        assertDenied(InputStream.class, "readAllBytes");
        assertDenied(OutputStream.class, "write");
        assertDenied(Reader.class, "read");
        assertDenied(Writer.class, "write");

        assertDenied(Files.class, "readAllBytes");
        assertDenied(Paths.class, "get");

        assertDenied(URL.class, "openStream");
        assertDenied(URI.class, "toURL");
    }

    /**
     * Regression guard: the reflection/system families that were already restricted must stay
     * denied (this change only extends coverage; it must not weaken the existing sandbox).
     */
    @Test
    public void reflection_and_system_families_stay_denied() {
        assertDenied(Runtime.class, "exec");
        assertDenied(Class.class, "forName");
        assertDenied(ClassLoader.class, "loadClass");
        assertDenied(Thread.class, "start");
        assertDenied(ProcessBuilder.class, "start");
        assertDenied(java.lang.reflect.Method.class, "invoke");
    }

    /**
     * The common value types and ordinary collection types templates rely on must stay allowed;
     * the added restriction must not shadow the existing fast-path allows.
     */
    @Test
    public void common_value_and_ordinary_types_stay_allowed() {
        assertAllowed(String.class, "length");
        assertAllowed(Integer.class, "intValue");   // Number subtype
        assertAllowed(Boolean.class, "booleanValue");
        assertAllowed(Class.class, "getName");       // explicitly allowed fast-path
        assertAllowed(ArrayList.class, "add");
        assertAllowed(HashMap.class, "get");
    }

    /**
     * Defense-in-depth: the shipped restricted-class list should also enumerate the
     * exact-matchable network-resource and file-system utility types, so the configuration
     * documents the intent alongside the existing restricted classes. The code-level check
     * remains the enforcement floor; this keeps the config in sync with it.
     */
    @Test
    public void restricted_class_config_enumerates_the_exact_resource_types() throws Exception {
        final java.util.Properties props = new java.util.Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(
                "org/apache/velocity/runtime/defaults/velocity.properties")) {
            assertTrue("velocity.properties must be present on the classpath", in != null);
            props.load(in);
        }
        final String restrictedClasses = String.join(",", props.getProperty(
                "introspector.restrict.classes", ""));
        // Properties.load keeps only the last value for a repeated key, so re-read raw lines.
        final java.util.List<String> configuredClasses = new java.util.ArrayList<>();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(
                        "org/apache/velocity/runtime/defaults/velocity.properties")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String trimmed = line.trim();
                if (trimmed.startsWith("introspector.restrict.classes")) {
                    final int eq = trimmed.indexOf('=');
                    if (eq > -1) {
                        configuredClasses.add(trimmed.substring(eq + 1).trim());
                    }
                }
            }
        }
        for (final String expected : new String[]{
                "java.net.URL", "java.net.URI", "java.nio.file.Files", "java.nio.file.Paths"}) {
            assertTrue("velocity.properties introspector.restrict.classes must enumerate "
                    + expected, configuredClasses.contains(expected)
                    || restrictedClasses.contains(expected));
        }
    }

    /**
     * The operator-configurable layer denies a whole package by prefix (so deployments can push
     * the sandbox toward a stricter stance without a code change), while an allow-list entry
     * carves a specific class back out. Defaults are empty, so this only takes effect when
     * configured.
     */
    @Test
    public void configurable_packages_are_denied_with_allowlist_carveout() {
        final SecureIntrospectorImpl sandbox = introspector();
        try {
            // Without configuration, an ordinary type is allowed.
            assertAllowed(java.time.LocalDate.class, "getYear");

            // Configuring a restricted package prefix denies the package and its subpackages.
            Config.setProperty(SecureIntrospectorImpl.RESTRICT_PACKAGES_PROP, "java.time");
            assertFalse("configured restricted package must be denied",
                    sandbox.checkObjectExecutePermission(java.time.LocalDate.class, "getYear"));
            assertFalse("subpackage of a configured restricted package must be denied (prefix match)",
                    sandbox.checkObjectExecutePermission(java.time.chrono.IsoChronology.class, "getId"));

            // An allow-list entry carves one class back out of the configurable denial.
            Config.setProperty(SecureIntrospectorImpl.ALLOW_CLASSES_PROP, "java.time.LocalDate");
            assertTrue("allow-listed class must be permitted despite the package restriction",
                    sandbox.checkObjectExecutePermission(java.time.LocalDate.class, "getYear"));
            assertFalse("a non-allow-listed class in the restricted package stays denied",
                    sandbox.checkObjectExecutePermission(java.time.chrono.IsoChronology.class, "getId"));

            // The configurable layer never overrides the code-level floor.
            assertDenied(File.class, "getCanonicalPath");
        } finally {
            Config.setProperty(SecureIntrospectorImpl.RESTRICT_PACKAGES_PROP, "");
            Config.setProperty(SecureIntrospectorImpl.ALLOW_CLASSES_PROP, "");
        }
    }

    /**
     * Servlet-container internals, embedded interpreters and the alternate reflection, naming
     * and process APIs must be denied, including implementations reached only through their
     * type hierarchy.
     */
    @Test
    public void container_interpreter_and_alternate_reflection_types_are_denied() throws Exception {
        assertDenied(java.lang.invoke.MethodHandles.Lookup.class, "findStatic");
        assertDenied(java.lang.invoke.MethodHandle.class, "invokeWithArguments");
        assertDenied(ProcessHandle.current().getClass(), "info");
        assertDenied(javax.naming.InitialContext.class, "lookup");
        assertDeniedIfPresent("bsh.Interpreter", "eval");
        assertDeniedIfPresent("org.apache.tomcat.SimpleInstanceManager", "newInstance");
        assertDeniedIfPresent("org.apache.catalina.core.DefaultInstanceManager", "newInstance");
    }

    /**
     * The runtime-type check must not catch ordinary template objects.
     */
    @Test
    public void ordinary_types_are_not_runtime_restricted() {
        assertAllowed(HashMap.class, "get");
        assertAllowed(ArrayList.class, "size");
    }

    /**
     * Templates may not unwrap the request or response handed to them, nor reach the servlet
     * context or async context through a request or session.
     */
    @Test
    public void request_and_session_cannot_be_unwrapped_to_container_objects() {
        assertDenied(VelocityRequestWrapper.class, "getRequest");
        assertDenied(HttpServletRequestWrapper.class, "getRequest");
        assertDenied(HttpServletResponseWrapper.class, "getResponse");
        assertDenied(HttpServletRequest.class, "getServletContext");
        assertDenied(VelocityRequestWrapper.class, "getServletContext");
        assertDenied(VelocityRequestWrapper.class, "getAsyncContext");
        assertDenied(VelocityRequestWrapper.class, "startAsync");
        assertDenied(HttpSession.class, "getServletContext");
        assertDenied(VelocitySessionWrapper.class, "getServletContext");
        assertDeniedIfPresent("org.apache.catalina.connector.RequestFacade", "getServletContext");
        assertDeniedIfPresent("org.apache.catalina.session.StandardSessionFacade", "getServletContext");
    }

    /**
     * The servlet context and the CDI, expression-language and JSP container objects reachable
     * from it must be denied, including implementations reached only through their hierarchy.
     */
    @Test
    public void servlet_context_and_container_service_types_are_denied() {
        assertDenied(ServletContext.class, "getAttribute");
        assertDeniedIfPresent("org.apache.catalina.core.ApplicationContextFacade", "getAttribute");
        assertDeniedIfPresent("javax.enterprise.inject.spi.BeanManager", "createInstance");
        assertDeniedIfPresent("org.jboss.weld.manager.BeanManagerImpl", "getContextId");
        assertDeniedIfPresent("javax.el.ExpressionFactory", "createValueExpression");
        assertDeniedIfPresent("javax.el.ELContext", "getELResolver");
        assertDeniedIfPresent("org.jboss.weld.module.web.el.WeldExpressionFactory", "createValueExpression");
        assertDeniedIfPresent("org.apache.jasper.runtime.JspApplicationContextImpl", "getExpressionFactory");
    }

    /**
     * Ordinary use of the request, response and session from templates keeps working.
     */
    @Test
    public void ordinary_request_response_and_session_calls_are_allowed() {
        assertAllowed(VelocityRequestWrapper.class, "getParameter");
        assertAllowed(VelocityRequestWrapper.class, "getAttribute");
        assertAllowed(VelocityRequestWrapper.class, "setAttribute");
        assertAllowed(VelocityRequestWrapper.class, "getSession");
        assertAllowed(VelocityRequestWrapper.class, "getRequestURI");
        assertAllowed(VelocityRequestWrapper.class, "getHeader");
        assertAllowed(VelocitySessionWrapper.class, "getAttribute");
        assertAllowed(VelocitySessionWrapper.class, "setAttribute");
        assertAllowed(VelocitySessionWrapper.class, "getId");
        assertAllowed(HttpServletResponse.class, "setHeader");
        assertAllowed(HttpServletResponse.class, "sendRedirect");
    }

    private void assertDeniedIfPresent(final String className, final String method) {
        final Class<?> clazz;
        try {
            clazz = Class.forName(className, false, getClass().getClassLoader());
        } catch (final ClassNotFoundException | LinkageError e) {
            return; // not on this test classpath; the prefix check is covered by the JDK types above
        }
        assertDenied(clazz, method);
    }
}
