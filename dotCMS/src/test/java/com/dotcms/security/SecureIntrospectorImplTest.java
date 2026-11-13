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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.rendering.velocity.viewtools.VelocitySessionWrapper;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.CompanyModel;
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
     * Servlet-container internals, embedded interpreters and the alternate reflection, naming,
     * process and desktop/OS-integration APIs must be denied, including implementations reached
     * only through their type hierarchy.
     */
    @Test
    public void container_interpreter_and_alternate_reflection_types_are_denied() throws Exception {
        assertDenied(java.lang.invoke.MethodHandles.Lookup.class, "findStatic");
        assertDenied(java.lang.invoke.MethodHandle.class, "invokeWithArguments");
        assertDenied(ProcessHandle.current().getClass(), "info");
        assertDenied(javax.naming.InitialContext.class, "lookup");
        assertDenied(java.awt.Desktop.class, "open");
        assertDenied(java.awt.Desktop.class, "browse");
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
     * The company key is only for server code: a template can read a company's other values, including the key
     * digest, but not the key itself, whichever way it got hold of the company.
     */
    @Test
    public void company_key_accessors_are_denied() {
        assertDenied(Company.class, "getKey");
        assertDenied(Company.class, "getKeyObj");
        assertDenied(CompanyModel.class, "getKey");
        assertAllowed(Company.class, "getKeyDigest");
        assertAllowed(Company.class, "getEmailAddress");
        assertAllowed(Company.class, "getName");
        assertAllowed(Company.class, "getPortalURL");
    }

    /**
     * Template helpers that resolve a property path by reflection refuse a path that reads the company key at any
     * step, and still resolve every other path.
     */
    @Test
    public void property_paths_through_the_company_key_are_restricted() {
        final Company company = new Company("sample.company");
        company.setName("Sample Company");
        company.setKeyObj(new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES"));
        final Map<String, Object> holder = new HashMap<>();
        holder.put("company", company);
        holder.put("key", "a map value");

        assertTrue(SecureIntrospectorImpl.isRestrictedPropertyPath(company, "key"));
        assertTrue(SecureIntrospectorImpl.isRestrictedPropertyPath(company, "key.bytes[0]"));
        assertTrue(SecureIntrospectorImpl.isRestrictedPropertyPath(company, "keyObj.encoded[0]"));
        assertTrue(SecureIntrospectorImpl.isRestrictedPropertyPath(holder, "company.key.bytes[0]"));
        assertFalse(SecureIntrospectorImpl.isRestrictedPropertyPath(company, "name"));
        assertFalse(SecureIntrospectorImpl.isRestrictedPropertyPath(company, "keyDigest"));
        assertFalse(SecureIntrospectorImpl.isRestrictedPropertyPath(holder, "key"));
        assertFalse(SecureIntrospectorImpl.isRestrictedPropertyPath(holder, "company.name"));
    }

    /**
     * {@code upgrade} (the container constructs the handler class and switches the connection)
     * and {@code authenticate} (drives the container's authentication and writes to the response)
     * are refused on any request, including the dotCMS wrapper and other wrappers.
     */
    @Test
    public void request_upgrade_and_authenticate_are_denied() {
        assertDenied(VelocityRequestWrapper.class, "upgrade");
        assertDenied(VelocityRequestWrapper.class, "authenticate");
        assertDenied(HttpServletRequest.class, "upgrade");
        assertDenied(HttpServletRequest.class, "authenticate");
        assertDenied(TemplateRequestWrapper.class, "upgrade");
        assertDenied(TemplateRequestWrapper.class, "authenticate");
    }

    /**
     * The servlet-context, async-context, CDI and expression-language types are matched through
     * the class hierarchy, so an implementation outside every restricted package is still denied.
     * These local subclasses live in this test's package, which no prefix covers.
     */
    @Test
    public void container_service_types_are_denied_through_the_hierarchy() {
        assertDenied(TemplateServletContext.class, "getAttribute");
        assertDenied(TemplateAsyncContext.class, "getRequest");
        assertDenied(TemplateBeanManager.class, "createInstance");
        assertDenied(TemplateELContext.class, "getELResolver");
        assertDenied(TemplateExpressionFactory.class, "createValueExpression");
    }

    /** A servlet context implemented outside the container's packages. */
    private abstract static class TemplateServletContext implements ServletContext {
    }

    /** An async context implemented outside the container's packages. */
    private abstract static class TemplateAsyncContext implements javax.servlet.AsyncContext {
    }

    /** A CDI bean manager implemented outside the CDI container's packages. */
    private abstract static class TemplateBeanManager implements javax.enterprise.inject.spi.BeanManager {
    }

    /** An expression-language context implemented outside the EL packages. */
    private abstract static class TemplateELContext extends javax.el.ELContext {
    }

    /** An expression factory implemented outside the EL packages. */
    private abstract static class TemplateExpressionFactory extends javax.el.ExpressionFactory {
    }

    /**
     * The servlet context and the CDI, expression-language and JSP container objects reachable
     * from it must be denied, including implementations reached only through their hierarchy.
     */
    @Test
    public void servlet_context_and_container_service_types_are_denied() {
        assertDenied(ServletContext.class, "getAttribute");
        assertDenied(org.apache.catalina.core.ApplicationContextFacade.class, "getAttribute");
        assertDenied(javax.enterprise.inject.spi.BeanManager.class, "createInstance");
        assertDenied(org.jboss.weld.manager.BeanManagerImpl.class, "getContextId");
        assertDenied(javax.el.ExpressionFactory.class, "createValueExpression");
        assertDenied(javax.el.ELContext.class, "getELResolver");
        assertDenied(org.jboss.weld.module.web.el.WeldExpressionFactory.class, "createValueExpression");
        assertDenied(org.apache.jasper.runtime.JspApplicationContextImpl.class, "getExpressionFactory");
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

    /**
     * No method on the servlet container's own request, response or session objects may be called
     * from a template. Templates receive dotCMS's wrappers instead, which decide what a template
     * may read or change.
     */
    @Test
    public void container_request_response_and_session_objects_are_denied() throws Exception {
        assertDenied(org.apache.catalina.connector.RequestFacade.class, "getSession");
        assertDenied(org.apache.catalina.connector.RequestFacade.class, "setAttribute");
        assertDenied(org.apache.catalina.connector.ResponseFacade.class, "setHeader");
        assertDenied(org.apache.catalina.session.StandardSessionFacade.class, "setAttribute");
        // The request a forwarded page render receives from the dispatcher (package-private class).
        assertDenied(Class.forName("org.apache.catalina.core.ApplicationHttpRequest"), "getSession");
        // $request.startAsync() hands back the container's async context, whose getRequest() is raw.
        assertDenied(org.apache.catalina.core.AsyncContextImpl.class, "getRequest");
        assertDenied(Class.forName("org.apache.catalina.core.ApplicationDispatcher"), "forward");
        assertDenied(org.apache.catalina.core.ApplicationContextFacade.class, "getRequestDispatcher");
        assertDenied(org.apache.catalina.core.ApplicationPushBuilder.class, "push");
        assertDenied(org.apache.catalina.realm.GenericPrincipal.class, "getPassword");
    }

    /**
     * A template may not write an uploaded part to a file or delete it, whichever implementation
     * backs the part, and may not call setLocked on the container's parameter map.
     */
    @Test
    public void uploaded_parts_and_the_parameter_map_cannot_be_changed() {
        assertDenied(org.apache.catalina.core.ApplicationPart.class, "write");
        assertDenied(org.apache.catalina.core.ApplicationPart.class, "delete");
        assertDenied(TemplatePart.class, "write");
        assertDenied(TemplatePart.class, "delete");
        assertDenied(org.apache.catalina.util.ParameterMap.class, "setLocked");
    }

    /**
     * Container value objects a template reaches through the wrapped request, such as the parameter
     * map of a forwarded page render or an uploaded part, stay readable.
     */
    @Test
    public void container_value_objects_stay_readable() {
        assertAllowed(org.apache.catalina.util.ParameterMap.class, "get");
        assertAllowed(org.apache.catalina.util.ParameterMap.class, "keySet");
        assertAllowed(org.apache.catalina.util.ParameterMap.class, "entrySet");
        assertAllowed(org.apache.catalina.util.ParameterMap.class, "isLocked");
        assertAllowed(org.apache.catalina.core.ApplicationPart.class, "getSubmittedFileName");
        assertAllowed(org.apache.catalina.core.ApplicationPart.class, "getName");
        assertAllowed(org.apache.catalina.core.ApplicationPart.class, "getSize");
        assertAllowed(TemplatePart.class, "getSubmittedFileName");
    }

    /** An uploaded part that is not the container's own implementation. */
    private abstract static class TemplatePart implements javax.servlet.http.Part {
    }

    /**
     * A template may not unwrap a servlet request or response wrapper: the object underneath could
     * be the container's own request or another wrapper that writes straight through to it.
     */
    @Test
    public void servlet_wrapper_unwrap_accessors_are_denied() {
        assertDenied(ServletRequestWrapper.class, "getRequest");
        assertDenied(HttpServletRequestWrapper.class, "getRequest");
        assertDenied(TemplateRequestWrapper.class, "getRequest");
        assertDenied(ServletResponseWrapper.class, "getResponse");
        assertDenied(HttpServletResponseWrapper.class, "getResponse");
        assertDenied(TemplateResponseWrapper.class, "getResponse");
    }

    /**
     * The rest of a wrapper's methods stay callable, and a {@code getRequest()} on a type that is not
     * a servlet wrapper (such as the Velocity view context) is unaffected.
     */
    @Test
    public void ordinary_servlet_wrapper_methods_stay_allowed() {
        assertAllowed(TemplateRequestWrapper.class, "getParameter");
        assertAllowed(TemplateRequestWrapper.class, "getSession");
        assertAllowed(TemplateRequestWrapper.class, "getAttribute");
        assertAllowed(TemplateRequestWrapper.class, "setAttribute");
        assertAllowed(TemplateRequestWrapper.class, "getRequestURI");
        assertAllowed(TemplateResponseWrapper.class, "setHeader");
        assertAllowed(TemplateResponseWrapper.class, "sendRedirect");
        assertAllowed(org.apache.velocity.tools.view.context.ChainedContext.class, "getRequest");
        assertAllowed(org.apache.velocity.tools.view.context.ChainedContext.class, "getResponse");
    }

    /** A dotCMS-style request wrapper, standing in for the wrappers templates receive. */
    private static class TemplateRequestWrapper extends HttpServletRequestWrapper {
        TemplateRequestWrapper(final HttpServletRequest request) {
            super(request);
        }
    }

    /** A dotCMS-style response wrapper, standing in for the wrappers templates receive. */
    private static class TemplateResponseWrapper extends HttpServletResponseWrapper {
        TemplateResponseWrapper(final HttpServletResponse response) {
            super(response);
        }
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
