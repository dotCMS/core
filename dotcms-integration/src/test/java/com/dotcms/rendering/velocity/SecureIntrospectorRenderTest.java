package com.dotcms.rendering.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
}
