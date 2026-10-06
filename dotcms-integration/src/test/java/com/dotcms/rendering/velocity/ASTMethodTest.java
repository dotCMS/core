package com.dotcms.rendering.velocity;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Renders method calls through Velocity to lock in the behavior of
 * {@link org.apache.velocity.runtime.parser.node.ASTMethod} after the zero-arg
 * allocation optimization (zero-arg calls reuse the shared empty {@code Object[]}
 * instead of allocating {@code new Object[0]} on every render).
 *
 * <p>The point of these tests is behavior-preservation: zero-arg, single-arg and
 * multi-arg invocations must still resolve and render identically, and repeated
 * invocation of a cached executor must stay correct.</p>
 */
public class ASTMethodTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Zero-arg method calls exercise the shared {@code ArrayUtils.EMPTY_OBJECT_ARRAY}
     * path. They must still resolve and render correctly.
     */
    @Test
    public void zero_arg_method_calls_render_correctly() throws Exception {
        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("str", "Hello World");

        Assert.assertEquals("HELLO WORLD", VelocityUtil.eval("$str.toUpperCase()", ctx).trim());
        Assert.assertEquals("hello world", VelocityUtil.eval("$str.toLowerCase()", ctx).trim());
        Assert.assertEquals("11", VelocityUtil.eval("$str.length()", ctx).trim());
    }

    /**
     * Method calls with arguments still allocate their {@code Object[]} and must
     * resolve/render correctly (the optimization only short-circuits the zero-arg case).
     */
    @Test
    public void method_calls_with_args_render_correctly() throws Exception {
        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("str", "Hello World");

        Assert.assertEquals("Hello", VelocityUtil.eval("$str.substring(0, 5)", ctx).trim());
        Assert.assertEquals("true", VelocityUtil.eval("$str.startsWith(\"Hello\")", ctx).trim());
        Assert.assertEquals("Jello World", VelocityUtil.eval("$str.replace(\"H\", \"J\")", ctx).trim());
    }

    /**
     * The resolved executor is cached per node/class; repeatedly invoking a zero-arg
     * method against the shared empty array must remain stable (no cross-call corruption).
     */
    @Test
    public void repeated_zero_arg_invocation_is_stable() throws Exception {
        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("str", "abc");

        for (int i = 0; i < 100; i++) {
            Assert.assertEquals("ABC", VelocityUtil.eval("$str.toUpperCase()", ctx).trim());
        }
    }

    /**
     * Mixing zero-arg and arg calls in a single template render must not let the
     * shared empty array leak into the arg path (guards the re-entrancy concern).
     */
    @Test
    public void mixed_zero_arg_and_arg_calls_in_one_render() throws Exception {
        final Context ctx = VelocityUtil.getBasicContext();
        ctx.put("str", "Hello World");

        // nested: a zero-arg call feeding an arg-bearing call, plus another zero-arg
        final String out = VelocityUtil.eval(
                "$str.toUpperCase().substring(0, $str.indexOf(\" \"))", ctx).trim();
        Assert.assertEquals("HELLO", out);
    }
}
