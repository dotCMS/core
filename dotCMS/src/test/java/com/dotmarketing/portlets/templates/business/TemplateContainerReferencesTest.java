package com.dotmarketing.portlets.templates.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.benmanes.caffeine.cache.Ticker;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.junit.Test;

/**
 * Unit tests for {@link TemplateContainerReferences}: the text scan that answers "which Containers
 * does this advanced Template name" without running Velocity, when the body allows it, and the
 * short-lived cache in front of the Velocity run for the bodies that do not.
 */
public class TemplateContainerReferencesTest {

    private static final long TTL_SECONDS = 60;
    private static final long MAX_ENTRIES = 100;

    private static Optional<Set<String>> scan(final String body) {
        return TemplateContainerReferences.fromLiteralCalls(body);
    }

    private static void assertAnswered(final Set<String> expected, final String body) {
        final Optional<Set<String>> found = scan(body);
        assertTrue("expected the text scan to answer for: " + body, found.isPresent());
        assertEquals(expected, found.get());
    }

    private static void assertNeedsVelocity(final String body) {
        assertFalse("expected the text scan to defer to Velocity for: " + body, scan(body).isPresent());
    }

    // ---------------------------------------------------------------------------------------------
    // Text scan: bodies it can answer
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: a body with one {@code #parseContainer} call with an id and an instance uuid
     * ExpectedResult: the id is returned
     */
    @Test
    public void testFromLiteralCalls_WhenIdAndUuidAreLiterals_ShouldReturnTheId() {
        assertAnswered(Set.of("abc-123"), "<div>#parseContainer('abc-123', '1')</div>");
    }

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: the call has only the id, or uses double quotes, or has no space after the comma
     * ExpectedResult: the id is returned in every spelling
     */
    @Test
    public void testFromLiteralCalls_WhenSpellingVaries_ShouldReturnTheId() {
        assertAnswered(Set.of("abc"), "#parseContainer('abc')");
        assertAnswered(Set.of("abc"), "#parseContainer(\"abc\")");
        assertAnswered(Set.of("abc"), "#parseContainer(\"abc\",\"1\")");
        assertAnswered(Set.of("abc"), "#parseContainer ( 'abc' ,  '1' )");
        assertAnswered(Set.of("abc"), "#parseContainer(\n    'abc',\n    '1'\n)");
    }

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: the call names a file container by path
     * ExpectedResult: the path is returned as it is written
     */
    @Test
    public void testFromLiteralCalls_WhenContainerIsAPath_ShouldReturnThePath() {
        assertAnswered(Set.of("//demo.dotcms.com/application/containers/default/"),
                "#parseContainer('//demo.dotcms.com/application/containers/default/', '1')");
        assertAnswered(Set.of("/application/containers/banner/"),
                "#parseContainer('/application/containers/banner/', '2')");
    }

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: several calls, two of them to the same container
     * ExpectedResult: every distinct container once
     */
    @Test
    public void testFromLiteralCalls_WhenSeveralCalls_ShouldReturnEachContainerOnce() {
        assertAnswered(Set.of("one", "two"),
                "#parseContainer('one','1') #parseContainer('two','1') #parseContainer('one','2')");
    }

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: bodies that name no container at all, or no body
     * ExpectedResult: an answer with no containers, not a request for Velocity: the text is enough
     */
    @Test
    public void testFromLiteralCalls_WhenNoCalls_ShouldAnswerWithNothing() {
        assertAnswered(Set.of(), "<html><body>#set($x = 1) $x</body></html>");
        assertAnswered(Set.of(), "");
        assertAnswered(Set.of(), null);
    }

    // ---------------------------------------------------------------------------------------------
    // Text scan: bodies that must go to Velocity (a wrong "none" would let a used container be deleted)
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: a body that pulls in another file, whose container calls the text cannot see
     * ExpectedResult: the scan defers to Velocity even when the body also has a literal call
     */
    @Test
    public void testFromLiteralCalls_WhenBodyIncludesAnotherFile_ShouldNeedVelocity() {
        assertNeedsVelocity("#dotParse('/application/templates/header.vtl')");
        assertNeedsVelocity("#parseContainer('abc','1') #dotParse('/application/templates/footer.vtl')");
        assertNeedsVelocity("#parse('footer.vtl')");
        assertNeedsVelocity("#parse ( \"footer.vtl\" )");
        assertNeedsVelocity("#include('footer.txt')");
        assertNeedsVelocity("#evaluate($snippet)");
        assertNeedsVelocity("#{dotParse}('/application/templates/header.vtl')");
    }

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: the call's container is not a plain literal: a variable, a concatenation or
     * a quoted string with a variable inside
     * ExpectedResult: the scan defers to Velocity, which knows the value
     */
    @Test
    public void testFromLiteralCalls_WhenContainerIsNotALiteral_ShouldNeedVelocity() {
        assertNeedsVelocity("#set($id = 'abc') #parseContainer($id, '1')");
        assertNeedsVelocity("#parseContainer(\"$containerId\", '1')");
        assertNeedsVelocity("#parseContainer('abc' + $suffix, '1')");
        assertNeedsVelocity("#parseContainer('abc', '1') #parseContainer($other, '1')");
        assertNeedsVelocity("#{parseContainer}('abc', '1')");
    }

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: the id is a literal but the instance uuid is a variable
     * ExpectedResult: the scan defers to Velocity rather than guess how the call is read
     */
    @Test
    public void testFromLiteralCalls_WhenUuidIsAVariable_ShouldNeedVelocity() {
        assertNeedsVelocity("#parseContainer('abc', $uuid)");
    }

    /**
     * Method to test: {@link TemplateContainerReferences#fromLiteralCalls(String)}
     * Given Scenario: text that only looks like the directives, such as a longer directive name
     * ExpectedResult: it is not mistaken for an include nor for a container call
     */
    @Test
    public void testFromLiteralCalls_WhenNameOnlyResemblesADirective_ShouldNotBeMisread() {
        assertAnswered(Set.of("abc"), "#parseContainer('abc','1') <p>#parseContainerNotes</p>");
        assertAnswered(Set.of(), "<a href=\"#dotParseSection\">anchor</a>");
    }

    // ---------------------------------------------------------------------------------------------
    // The cache in front of the Velocity run
    // ---------------------------------------------------------------------------------------------

    private static final class CountingHarvest implements Function<String, Collection<String>> {

        private final AtomicInteger calls = new AtomicInteger();
        private final Collection<String> result;

        private CountingHarvest(final Collection<String> result) {
            this.result = result;
        }

        @Override
        public Collection<String> apply(final String body) {
            this.calls.incrementAndGet();
            return this.result;
        }
    }

    private static final String NEEDS_VELOCITY = "#set($id = 'abc') #parseContainer($id, '1')";

    /**
     * Method to test: {@link TemplateContainerReferences#forAdvancedTemplate(String, String)}
     * Given Scenario: a body the text scan can answer
     * ExpectedResult: Velocity is never run, however often it is asked
     */
    @Test
    public void testForAdvancedTemplate_WhenBodyIsLiteral_ShouldNotRunVelocity() {
        final CountingHarvest harvest = new CountingHarvest(List.of("never"));
        final TemplateContainerReferences references =
                new TemplateContainerReferences(harvest, TTL_SECONDS, MAX_ENTRIES);

        assertEquals(Set.of("abc"), references.forAdvancedTemplate("inode-1", "#parseContainer('abc','1')"));
        assertEquals(Set.of("abc"), references.forAdvancedTemplate("inode-1", "#parseContainer('abc','1')"));

        assertEquals(0, harvest.calls.get());
    }

    /**
     * Method to test: {@link TemplateContainerReferences#forAdvancedTemplate(String, String)}
     * Given Scenario: a body that needs Velocity, asked for twice for the same template inode
     * ExpectedResult: Velocity runs once; the second answer comes from the cache and is the same
     */
    @Test
    public void testForAdvancedTemplate_WhenAskedTwice_ShouldRunVelocityOnce() {
        final CountingHarvest harvest = new CountingHarvest(List.of("abc"));
        final TemplateContainerReferences references =
                new TemplateContainerReferences(harvest, TTL_SECONDS, MAX_ENTRIES);

        final Set<String> first = references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY);
        final Set<String> second = references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY);

        assertEquals(Set.of("abc"), first);
        assertEquals(first, second);
        assertEquals(1, harvest.calls.get());
    }

    /**
     * Method to test: {@link TemplateContainerReferences#forAdvancedTemplate(String, String)}
     * Given Scenario: the same body under two inodes, as when a template is saved again
     * ExpectedResult: each inode is harvested on its own: the cache is keyed by inode
     */
    @Test
    public void testForAdvancedTemplate_WhenInodeChanges_ShouldRunVelocityAgain() {
        final CountingHarvest harvest = new CountingHarvest(List.of("abc"));
        final TemplateContainerReferences references =
                new TemplateContainerReferences(harvest, TTL_SECONDS, MAX_ENTRIES);

        references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY);
        references.forAdvancedTemplate("inode-2", NEEDS_VELOCITY);

        assertEquals(2, harvest.calls.get());
    }

    /**
     * Method to test: {@link TemplateContainerReferences#forAdvancedTemplate(String, String)}
     * Given Scenario: Velocity answers with nothing, which is also how it reports a body that
     * failed to evaluate
     * ExpectedResult: the empty answer is returned but not remembered, so the next ask tries again
     */
    @Test
    public void testForAdvancedTemplate_WhenVelocityAnswersNothing_ShouldNotRememberIt() {
        final CountingHarvest harvest = new CountingHarvest(List.of());
        final TemplateContainerReferences references =
                new TemplateContainerReferences(harvest, TTL_SECONDS, MAX_ENTRIES);

        assertTrue(references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY).isEmpty());
        assertTrue(references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY).isEmpty());

        assertEquals(2, harvest.calls.get());
    }

    /**
     * Method to test: {@link TemplateContainerReferences#forAdvancedTemplate(String, String)}
     * Given Scenario: more time than the time to live passes after the answer was cached
     * ExpectedResult: the answer is dropped and Velocity runs again, so a changed included file is
     * seen within that time
     */
    @Test
    public void testForAdvancedTemplate_WhenTimeToLiveExpires_ShouldRunVelocityAgain() {
        final AtomicLong nanos = new AtomicLong();
        final Ticker ticker = nanos::get;
        final CountingHarvest harvest = new CountingHarvest(List.of("abc"));
        final TemplateContainerReferences references =
                new TemplateContainerReferences(harvest, TTL_SECONDS, MAX_ENTRIES, ticker);

        references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY);
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(TTL_SECONDS - 1));
        references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY);
        assertEquals("still inside the time to live", 1, harvest.calls.get());

        nanos.addAndGet(TimeUnit.SECONDS.toNanos(2));
        references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY);
        assertEquals("past the time to live", 2, harvest.calls.get());
    }

    /**
     * Method to test: {@link TemplateContainerReferences#forAdvancedTemplate(String, String)}
     * Given Scenario: a time to live of zero, the setting that turns the cache off
     * ExpectedResult: Velocity runs on every ask
     */
    @Test
    public void testForAdvancedTemplate_WhenTimeToLiveIsZero_ShouldNeverCache() {
        final CountingHarvest harvest = new CountingHarvest(List.of("abc"));
        final TemplateContainerReferences references =
                new TemplateContainerReferences(harvest, 0, MAX_ENTRIES);

        assertEquals(Set.of("abc"), references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY));
        assertEquals(Set.of("abc"), references.forAdvancedTemplate("inode-1", NEEDS_VELOCITY));

        assertEquals(2, harvest.calls.get());
    }
}
