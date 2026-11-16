package com.dotcms.rendering.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.liferay.portal.util.WebKeys;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Render-time counterpart of {@code SecureIntrospectorImplTest}: verifies that the Velocity
 * engine actually applies the restricted-class coverage when merging a template. A template
 * that receives a {@link File} or {@link Path} through the context must not be able to resolve
 * file-system methods on it, so the introspector guard is proven to be wired into the engine,
 * not just correct in isolation.
 */
public class SecureIntrospectorRenderTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Control: an ordinary value method still resolves, confirming the engine renders normally
     * and the assertions below are about the guard, not a broken setup.
     */
    @Test
    public void ordinary_value_methods_still_render() throws Exception {
        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("greeting", "Hello World");
        assertEquals("HELLO WORLD", VelocityUtil.eval("$greeting.toUpperCase()", ctx).trim());
    }

    /**
     * A {@link File} in the context must not expose its file-system path to a template: the
     * denied method does not resolve, so the real path never appears in the rendered output.
     */
    @Test
    public void file_methods_do_not_resolve_at_render_time() throws Exception {
        final File secret = File.createTempFile("dotcms-introspector-render", ".txt");
        secret.deleteOnExit();
        final String realPath = secret.getCanonicalPath();

        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("file", secret);

        final String rendered = VelocityUtil.eval("[$file.getCanonicalPath()]", ctx);
        assertFalse("File path must not be reachable from a template; rendered: " + rendered,
                rendered.contains(realPath));
    }

    /**
     * A concrete platform {@link Path} implementation (e.g. the JDK's internal Path type) must
     * also be blocked — this is the case only the hierarchy-aware check covers.
     */
    @Test
    public void concrete_path_methods_do_not_resolve_at_render_time() throws Exception {
        final File secret = File.createTempFile("dotcms-introspector-render-path", ".txt");
        secret.deleteOnExit();
        final String realPath = secret.getCanonicalPath();
        final Path path = Paths.get(realPath);
        // Guard the premise: this is a concrete platform implementation, not the Path interface.
        Assert.assertNotEquals(Path.class, path.getClass());

        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("path", path);

        final String rendered = VelocityUtil.eval("[$path.toString()][$path.toFile()]", ctx);
        assertFalse("Concrete Path methods must not be reachable from a template; rendered: "
                + rendered, rendered.contains(realPath));
    }

    /**
     * A template cannot unwrap {@code $request} or {@code $response} to reach the object the
     * dotCMS wrapper delegates to, with either method or property syntax.
     */
    @Test
    public void template_cannot_unwrap_request_or_response() throws Exception {
        final HttpServletRequest request = containerRequest();
        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());

        final String rendered = VelocityUtil.eval(
                "#if($request.getRequest())reached#{else}refused#end"
                        + "|#if($request.request)reached#{else}refused#end"
                        + "|#if($response.getResponse())reached#{else}refused#end", ctx);

        assertEquals("refused|refused|refused", rendered.trim());
    }

    /**
     * Writes made after unwrapping {@code $request} never reach the request or session underneath,
     * neither for an ordinary key nor for one only dotCMS may set.
     */
    @Test
    public void unwrapped_request_writes_do_not_reach_the_container() throws Exception {
        final HttpServletRequest request = containerRequest();
        final HttpSession session = request.getSession();
        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());

        VelocityUtil.eval("$request.getRequest().setAttribute('unwrapRequestMarker', 'set')"
                + "$request.getRequest().getSession().setAttribute('unwrapSessionMarker', 'set')"
                + "$request.getRequest().getSession().setAttribute('" + WebKeys.USER_ID + "', 'someone')"
                + "$request.getRequest().setAttribute('" + WebKeys.USER + "', 'someone')", ctx);

        assertNull(request.getAttribute("unwrapRequestMarker"));
        assertNull(session.getAttribute("unwrapSessionMarker"));
        assertNull(session.getAttribute(WebKeys.USER_ID));
        assertNull(request.getAttribute(WebKeys.USER));
    }

    /**
     * Regression: the wrapped session still reads and writes an ordinary key, and the write lands
     * in the real session.
     */
    @Test
    public void wrapped_session_still_reads_and_writes_allowed_keys() throws Exception {
        final HttpServletRequest request = containerRequest();
        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());

        final String rendered = VelocityUtil.eval(
                "$request.getSession().setAttribute('wrappedSessionKey', 'wrappedValue')"
                        + "$request.getSession().getAttribute('wrappedSessionKey')", ctx);

        assertEquals("wrappedValue", rendered.trim());
        assertEquals("wrappedValue", request.getSession().getAttribute("wrappedSessionKey"));
    }

    /**
     * Regression: the wrapped request and session still refuse a key only dotCMS may set.
     */
    @Test
    public void wrapped_request_and_session_still_refuse_reserved_keys() throws Exception {
        final HttpServletRequest request = containerRequest();
        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());

        VelocityUtil.eval("$request.getSession().setAttribute('" + WebKeys.USER_ID + "', 'someone')"
                + "$request.setAttribute('" + WebKeys.USER + "', 'someone')", ctx);

        assertNull(request.getSession().getAttribute(WebKeys.USER_ID));
        assertNull(request.getAttribute(WebKeys.USER));
    }

    /**
     * Regression: Java code still unwraps the request a template sees. A RequestDispatcher include
     * or forward walks the wrapper chain through {@link ServletRequestWrapper#getRequest()} the same
     * way {@link ServletRequestWrapper#isWrapperFor(ServletRequest)} does, so both must still reach
     * the request underneath.
     */
    @Test
    public void java_code_still_unwraps_the_template_request() throws Exception {
        final HttpServletRequest request = containerRequest();
        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());

        final ServletRequestWrapper templateRequest = (ServletRequestWrapper) ctx.get("request");
        assertSame(request, templateRequest.getRequest());
        assertTrue(templateRequest.isWrapperFor(request));
    }

    /**
     * Regression: templates still read the request's parameter map through the wrapper, by key and
     * by iterating its keys. That the container's own map class stays readable is covered by
     * {@code SecureIntrospectorImplTest}, since Tomcat's classes are not on this module's classpath.
     */
    @Test
    public void parameter_map_still_renders() throws Exception {
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("q", new String[] {"dotcms"});
        final HttpServletRequest request = new HttpServletRequestWrapper(containerRequest()) {
            @Override
            public Map<String, String[]> getParameterMap() {
                return parameters;
            }
        };
        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());

        final String rendered = VelocityUtil.eval("$request.getParameterMap().get('q').get(0)"
                + "|#foreach($key in $request.getParameterMap().keySet())$key#end", ctx);

        assertEquals("dotcms|q", rendered.trim());
    }

    /**
     * A template can read an uploaded part but cannot write it to a file or delete it.
     */
    @Test
    public void uploaded_part_cannot_be_written_or_deleted() throws Exception {
        final RecordingPart part = new RecordingPart();
        final HttpServletRequest request = new HttpServletRequestWrapper(containerRequest()) {
            @Override
            public Part getPart(final String name) {
                return "upload".equals(name) ? part : null;
            }
        };
        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());

        final String rendered = VelocityUtil.eval(
                "$request.getPart('upload').write('/tmp/dotcms-template-part')"
                        + "|$request.getPart('upload').delete()"
                        + "|$request.getPart('upload').getSubmittedFileName()", ctx);

        assertEquals("write calls; rendered: " + rendered, 0, part.writes.get());
        assertEquals("delete calls; rendered: " + rendered, 0, part.deletes.get());
        assertTrue("part name must still render; rendered: " + rendered, rendered.trim().endsWith("|photo.png"));
    }

    /** An uploaded part that counts write and delete calls instead of touching the file system. */
    public static class RecordingPart implements Part {
        final AtomicInteger writes = new AtomicInteger();
        final AtomicInteger deletes = new AtomicInteger();

        @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public String getContentType() { return "image/png"; }
        @Override public String getName() { return "upload"; }
        @Override public String getSubmittedFileName() { return "photo.png"; }
        @Override public long getSize() { return 0; }
        @Override public void write(final String fileName) { writes.incrementAndGet(); }
        @Override public void delete() { deletes.incrementAndGet(); }
        @Override public String getHeader(final String name) { return null; }
        @Override public Collection<String> getHeaders(final String name) { return List.of(); }
        @Override public Collection<String> getHeaderNames() { return List.of(); }
    }

    /**
     * A request chain standing in for what the servlet container hands dotCMS: a real session and
     * request attributes underneath the wrapper that templates receive.
     */
    private static HttpServletRequest containerRequest() {
        final HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()
                ).request()).request();
        request.getSession();
        return request;
    }
}
