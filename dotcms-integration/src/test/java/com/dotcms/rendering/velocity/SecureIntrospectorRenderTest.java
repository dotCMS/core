package com.dotcms.rendering.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.rendering.velocity.util.TemplateEditableObject;
import com.dotmarketing.util.Config;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
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
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.SortTool;
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
     * A template that gets hold of the company, here through {@code $UtilMethods}, which every context carries,
     * must not read its key in any form, while the company's other values still render.
     */
    @Test
    public void company_key_does_not_render() throws Exception {
        final Company company = APILocator.getCompanyAPI().getDefaultCompany();
        assertTrue("the default company holds a key", company.getKey() != null);

        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("math", new MathTool());
        ctx.put("sorter", new SortTool());
        final String rendered = VelocityUtil.eval("#set($c = $UtilMethods.getDefaultCompany())"
                + "#set($m = $UtilMethods.toMap($c))"
                + "#set($probe = {'key': 'rO0AB'})"
                + "[$!c.key][$!c.getKey()][$!c.keyObj][$!m.key][$!m.get('key')]"
                + "[$!math.getTotal([$c], 'key.bytes[0]')][$!sorter.sort([$c, $probe], 'key')]"
                + "[$!c.emailAddress]", ctx).trim();

        // Compared without printing the output, so a failure does not write a key to the build log
        assertTrue("the template must not read the key",
                ("[][][][][][][][" + company.getEmailAddress() + "]").equals(rendered));
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
     * Template code that holds the content object the content API hands out cannot change it: not
     * with a setter, a property assignment, the field map or a binary lookup. The next reader still
     * sees the saved values.
     */
    @Test
    public void template_cannot_change_a_content_object() throws Exception {
        final Contentlet saved = newContent();
        final Contentlet content = findContent(saved.getInode());
        // The premise: the content API hands every caller the same instance.
        assertSame(content, findContent(saved.getInode()));
        final String modUser = content.getModUser();
        final String title = content.getTitle();

        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("c", content);
        VelocityUtil.eval("$c.setModUser('someone-else')"
                + "#set($c.modUser = 'someone-else')"
                + "$c.map.put('modUser', 'someone-else')"
                + "$c.getMap().put('modUser', 'someone-else')"
                + "$c.setProperty('title', 'changed-title')"
                + "$c.setStringProperty('title', 'changed-title')"
                + "$c.getMap().remove('title')"
                + "$c.getBinary('title')"
                + "$c.getBinary('modUser')", ctx);

        final Contentlet reread = findContent(saved.getInode());
        assertEquals(modUser, reread.getModUser());
        assertEquals(title, reread.getStringProperty("title"));
    }

    /**
     * Template code that holds the live container the versionable API hands out, as
     * {@code $webapi.getLiveFileAsset} does, cannot change the code the container renders.
     */
    @Test
    public void template_cannot_change_a_container_object() throws Exception {
        final Container saved = new ContainerDataGen().preLoop("ORIGINAL-PRE").code("ORIGINAL-CODE")
                .postLoop("ORIGINAL-POST").nextPersisted();
        ContainerDataGen.publish(saved);
        final Container container = (Container) findLive(saved.getIdentifier());
        assertSame(container, findLive(saved.getIdentifier()));

        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("con", container);
        VelocityUtil.eval("$con.setPreLoop('CHANGED')$con.setCode('CHANGED')$con.setPostLoop('CHANGED')"
                + "#set($con.preLoop = 'CHANGED')$con.setModUser('someone-else')", ctx);

        final Container reread = (Container) findLive(saved.getIdentifier());
        assertEquals("ORIGINAL-PRE", reread.getPreLoop());
        assertEquals("ORIGINAL-CODE", reread.getCode());
        assertEquals("ORIGINAL-POST", reread.getPostLoop());
        assertEquals(saved.getModUser(), reread.getModUser());
    }

    /**
     * Template code that holds the live template object cannot change the body it renders.
     */
    @Test
    public void template_cannot_change_a_template_object() throws Exception {
        final Template saved = new TemplateDataGen().body("ORIGINAL-BODY").nextPersisted();
        TemplateDataGen.publish(saved);
        final Template template = (Template) findLive(saved.getIdentifier());
        assertSame(template, findLive(saved.getIdentifier()));
        final String drawedBody = template.getDrawedBody();
        final String headCode = template.getHeadCode();

        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("t", template);
        VelocityUtil.eval("$t.setBody('CHANGED')$t.setDrawedBody('CHANGED')$t.setHeadCode('CHANGED')"
                + "#set($t.body = 'CHANGED')$t.setModUser('someone-else')", ctx);

        final Template reread = (Template) findLive(saved.getIdentifier());
        assertEquals("ORIGINAL-BODY", reread.getBody());
        assertEquals(drawedBody, reread.getDrawedBody());
        assertEquals(headCode, reread.getHeadCode());
        assertEquals(saved.getModUser(), reread.getModUser());
    }

    /**
     * Regression: a render for a back-end user without the Scripting Developer role still reads the
     * content object's values, by property, by getter and through its field map.
     */
    @Test
    public void template_still_reads_a_content_object() throws Exception {
        final Contentlet saved = newContent();
        final Contentlet content = findContent(saved.getInode());
        final User viewer = new UserDataGen().nextPersisted();
        final HttpServletRequest request = containerRequest();
        request.setAttribute(WebKeys.USER, viewer);

        final Context ctx = VelocityUtil.getWebContext(request, new MockHttpResponse());
        ctx.put("c", content);
        final String rendered = VelocityUtil.eval(
                "[$c.title][$c.getTitle()][$c.map.title][$c.getMap().get('title')][$c.modUser]"
                        + "#set($m = $c.map)#set($ignored = $m.put('note', 'local'))[$m.get('note')]", ctx);

        final String title = content.getTitle();
        assertEquals("[" + title + "][" + title + "][" + title + "][" + title + "]["
                + content.getModUser() + "][local]", rendered.trim());
        assertNull("a local change to the map stays local", findContent(saved.getInode()).get("note"));
    }

    /**
     * With the switch on, template code changes model objects as it did before.
     */
    @Test
    public void switch_lets_template_change_a_content_object() throws Exception {
        final Contentlet content = new Contentlet();
        content.setModUser("before");
        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("c", content);

        Config.setProperty(FeatureFlagName.FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS, true);
        try {
            VelocityUtil.eval("$c.setModUser('after')$c.map.put('note', 'set')", ctx);
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_VELOCITY_ALLOW_MODEL_MUTATORS, false);
        }

        assertEquals("after", content.getModUser());
        assertEquals("set", content.get("note"));
    }

    /**
     * Template code may change the one object an open {@link TemplateEditableObject} scope names, and only that
     * instance; the innermost scope wins, closing it restores the one before, and no scope outlives its block, even
     * when the block throws.
     */
    @Test
    public void editable_scope_allows_only_the_named_object() throws Exception {
        final Contentlet outer = new Contentlet();
        final Contentlet inner = new Contentlet();
        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("outer", outer);
        ctx.put("inner", inner);
        final String change = "$outer.setModUser('changed')$inner.setModUser('changed')";

        try (TemplateEditableObject ignored = TemplateEditableObject.open(() -> outer)) {
            VelocityUtil.eval(change, ctx);
            assertEquals("changed", outer.getModUser());
            assertNull(inner.getModUser());

            outer.setModUser("reset");
            try (TemplateEditableObject nested = TemplateEditableObject.open(() -> inner)) {
                VelocityUtil.eval(change + "$inner.map.put('note', 'set')", ctx);
            }
            assertEquals("the inner scope hides the outer one", "reset", outer.getModUser());
            assertEquals("changed", inner.getModUser());
            assertEquals("the named object's own map is handed out", "set", inner.get("note"));

            assertTrue("closing the inner scope restores the outer one", TemplateEditableObject.isEditable(outer));
        }
        assertFalse(TemplateEditableObject.isEditable(outer));

        try (TemplateEditableObject ignored = TemplateEditableObject.open(() -> outer)) {
            throw new IllegalStateException("thrown inside the scope");
        } catch (final IllegalStateException expected) {
            assertFalse("a scope never outlives its block", TemplateEditableObject.isEditable(outer));
        }
    }

    /**
     * Saves a content item of a type without Story Block fields, which the content API hands out as the instance it
     * holds in its cache.
     */
    private static Contentlet newContent() {
        final Field title = new FieldDataGen().name("title").velocityVarName("title").next();
        final ContentType type = new ContentTypeDataGen().field(title).nextPersisted();
        return new ContentletDataGen(type).setProperty("title", "Original title").nextPersisted();
    }

    /** Looks a content item up the way the content view tools do. */
    private static Contentlet findContent(final String inode) throws Exception {
        return APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
    }

    /** Looks a live version up the way {@code $webapi.getLiveFileAsset} does. */
    private static Object findLive(final String identifier) throws Exception {
        return APILocator.getVersionableAPI().findLiveVersion(
                APILocator.getIdentifierAPI().find(identifier), APILocator.systemUser(), false);
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
