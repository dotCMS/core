package com.dotcms.rendering.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import java.util.List;
import java.util.Map;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * End-to-end coverage for reading Java {@code record} components from VTL through the real dotCMS
 * Velocity engine.
 *
 * <p>A record's canonical accessor is named after the component ({@code id()}), while Velocity's
 * property resolution only ever looked for {@code getId()} / {@code getid()} / {@code Map.get} /
 * {@code get("id")} / {@code isId()}. A reference that resolves to nothing is not an error in
 * Velocity — it renders as literal template text — so before
 * {@link org.apache.velocity.runtime.parser.node.RecordComponentExecutor} a template reading a
 * record printed {@code $rec.id} into the page, silently. That is why the assertions here check
 * rendered output rather than the introspection result: the unit test
 * {@code RecordComponentExecutorTest} covers the resolution chain, this one covers what a page
 * actually shows.</p>
 *
 * <p>References are written <strong>non-quiet</strong> ({@code $rec.id}, never {@code $!{rec.id}}) on
 * purpose. Quiet notation renders an unresolved reference as the empty string, which would let a
 * broken accessor pass an assertion that only checks for absence.</p>
 *
 * @author Fabrizio Araya
 */
public class RecordComponentRenderingTest {

    /** Idiomatic record: components are read as {@code $rec.id} / {@code $rec.title}. */
    public record Article(String id, String title, int views, List<String> tags) {}

    /** A record nested inside another, to walk {@code $rec.author.name}. */
    public record Author(String name) {}

    /** Composite record, for the nested-walk case. */
    public record Post(String title, Author author) {}

    /** The bean-named shape already shipped in the neutral search layer ({@code SearchHit}). */
    public record BeanNamedHit(String getId, String getIndex) {}

    /** Not a record, but exposes a no-argument {@code id()} method. */
    public static final class LooksLikeARecord {

        public String id() {
            return "must-not-resolve";
        }
    }

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private Context context(final String name, final Object value) {
        final Context ctx = new VelocityContext();
        ctx.put(name, value);
        return ctx;
    }

    /**
     * Method to test: {@link VelocityUtil#eval(String, Context)} over a record reference.
     * Given scenario: a template reads every component of a record with canonical accessors.
     * Expected result: the values are rendered. Before the fix the output was the literal text
     * {@code $article.id | $article.title | $article.views}.
     */
    @Test
    public void test_canonicalRecordComponents_render() throws Exception {
        final Article article = new Article("abc-123", "Modern Java", 42, List.of("java", "records"));

        final String output = VelocityUtil.eval(
                "$article.id | $article.title | $article.views",
                context("article", article));

        assertEquals("abc-123 | Modern Java | 42", output.trim());
        assertFalse("no reference may survive as literal text", output.contains("$article"));
    }

    /**
     * Method to test: {@link VelocityUtil#eval(String, Context)} over a collection component.
     * Given scenario: a {@code #foreach} walks a {@code List} held by a record component.
     * Expected result: the loop runs and emits every element.
     */
    @Test
    public void test_foreachOverRecordCollectionComponent_renders() throws Exception {
        final Article article = new Article("abc-123", "Modern Java", 42, List.of("java", "records"));

        final String output = VelocityUtil.eval(
                "#foreach($tag in $article.tags)[$tag]#end",
                context("article", article));

        assertEquals("[java][records]", output.trim());
    }

    /**
     * Method to test: {@link VelocityUtil#eval(String, Context)} over nested records.
     * Given scenario: a record component is itself a record, walked as {@code $post.author.name}.
     * Expected result: the walk resolves at both levels.
     */
    @Test
    public void test_nestedRecordWalk_renders() throws Exception {
        final Post post = new Post("Hello", new Author("Fabrizio"));

        final String output = VelocityUtil.eval("$post.author.name", context("post", post));

        assertEquals("Fabrizio", output.trim());
    }

    /**
     * Method to test: {@link VelocityUtil#eval(String, Context)} over a non-record.
     * Given scenario: a plain class exposing a no-argument {@code id()} method.
     * Expected result: the reference still does NOT resolve and renders as literal text. This is the
     * guardrail of the change, asserted end-to-end: resolution was widened for records only, not for
     * every no-argument method, so no existing template changes meaning.
     */
    @Test
    public void test_nonRecordNoArgMethod_stillRendersAsLiteralText() throws Exception {
        final String output = VelocityUtil.eval("$obj.id", context("obj", new LooksLikeARecord()));

        assertEquals("$obj.id", output.trim());
        assertFalse(output.contains("must-not-resolve"));
    }

    /**
     * Method to test: {@link VelocityUtil#eval(String, Context)} over a bean-named record.
     * Given scenario: a record whose components are named {@code getId} / {@code getIndex}, the
     * workaround the neutral search layer adopted so its hits could be read from VTL.
     * Expected result: {@code $hit.id} keeps rendering exactly as before, through the bean-getter
     * path. Already-shipped records are untouched by this change.
     */
    @Test
    public void test_beanNamedRecord_rendersUnchanged() throws Exception {
        final String output = VelocityUtil.eval(
                "$hit.id | $hit.index",
                context("hit", new BeanNamedHit("abc-123", "live_index")));

        assertEquals("abc-123 | live_index", output.trim());
    }

    /**
     * Method to test: {@link VelocityUtil#eval(String, Context)} over a {@code Map}.
     * Given scenario: a map reference, which is how most dotCMS content reaches templates.
     * Expected result: key lookup renders unchanged.
     */
    @Test
    public void test_mapKeyLookup_rendersUnchanged() throws Exception {
        final String output = VelocityUtil.eval(
                "$content.title",
                context("content", Map.of("title", "From a map")));

        assertEquals("From a map", output.trim());
    }

    /**
     * Method to test: {@link VelocityUtil#eval(String, Context)} for an unknown component.
     * Given scenario: the template reads a component the record does not declare.
     * Expected result: Velocity's existing behaviour for unresolved references is preserved — literal
     * text, not an exception.
     */
    @Test
    public void test_unknownComponent_rendersAsLiteralText() throws Exception {
        final Article article = new Article("abc-123", "Modern Java", 42, List.of());

        final String output = VelocityUtil.eval("$article.nope", context("article", article));

        assertTrue(output.trim().contains("$article.nope"));
    }
}
