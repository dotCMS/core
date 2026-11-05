package com.dotcms.rendering.velocity.viewtools.content.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.TimeMachineUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Verifies that {@link ContentUtils#addDefaultsToQuery(String, boolean, HttpServletRequest)} puts
 * the clauses it adds before the caller's query, so that each of them stands on its own whatever
 * the caller's query ends with. A clause added after a query that ends in an unbalanced escape
 * would lose the space that separates the two and be folded into the caller's last term.
 */
public class ContentUtilsAddDefaultsToQueryTest {

    private static final long LANGUAGE_ID = 1L;

    /**
     * Collapses runs of whitespace so an assertion is about the clauses and their order, not about
     * how many spaces the method happens to put between them.
     */
    private static String clauses(final String query) {
        return query.trim().replaceAll("\\s+", " ");
    }

    /**
     * The class builds a singleton of itself when it is loaded, and that singleton reaches for the
     * {@link ContentletAPI} bean, which no unit test has. Forcing the class to load here, with
     * that one call stubbed out, lets every test below call the method under test directly.
     * <p>
     * This works only while no other test in the same Surefire fork loads {@code ContentUtils}
     * first: a class whose initializer threw stays unusable for the life of the JVM, and every
     * case here would then die with {@code NoClassDefFoundError}. Nothing else under
     * {@code dotCMS/src/test} references the class today. The lasting fix is to resolve the
     * {@link ContentletAPI} lazily in the production class rather than in its initializer.
     */
    @BeforeClass
    public static void loadContentUtils() throws Exception {
        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getContentletAPI)
                    .thenReturn(Mockito.mock(ContentletAPI.class));
            Class.forName(ContentUtils.class.getName(), true,
                    ContentUtilsAddDefaultsToQueryTest.class.getClassLoader());
        }
    }

    /**
     * Runs the method with the language and Time Machine collaborators stubbed out.
     *
     * @param query the caller's query
     * @param editOrPreviewMode whether to default to working rather than live content
     * @param timeMachineRunning whether the Time Machine is running
     * @return the completed query
     */
    private static String addDefaults(final String query, final boolean editOrPreviewMode,
            final boolean timeMachineRunning) {

        final Language language = Mockito.mock(Language.class);
        Mockito.when(language.getId()).thenReturn(LANGUAGE_ID);

        final LanguageWebAPI languageWebAPI = Mockito.mock(LanguageWebAPI.class);
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(languageWebAPI.getLanguage(request)).thenReturn(language);

        try (MockedStatic<WebAPILocator> webAPILocator = Mockito.mockStatic(WebAPILocator.class);
                MockedStatic<TimeMachineUtil> timeMachine =
                        Mockito.mockStatic(TimeMachineUtil.class)) {

            webAPILocator.when(WebAPILocator::getLanguageWebAPI).thenReturn(languageWebAPI);
            timeMachine.when(TimeMachineUtil::isRunning).thenReturn(timeMachineRunning);

            return ContentUtils.addDefaultsToQuery(query, editOrPreviewMode, request);
        }
    }

    @Test
    public void test_query_stating_every_default_is_returned_untouched() {
        final String query = "+languageId:1 +live:true +deleted:false +contentType:Blog";
        assertEquals(query, addDefaults(query, false, false));
    }

    @Test
    public void test_query_stating_every_default_is_returned_untouched_without_collaborators() {
        // No clause is missing, so neither the language nor the Time Machine is consulted and the
        // method can run with nothing stubbed out.
        final String query = "+languageId:1 +working:true +deleted:false +contentType:Blog";
        assertEquals(query, ContentUtils.addDefaultsToQuery(query, true, null));
    }

    @Test
    public void test_defaults_are_placed_before_the_caller_query() {
        final String result = addDefaults("+contentType:Blog", false, false);

        assertEquals("+languageId:1 +live:true +deleted:false +contentType:Blog", clauses(result));
        assertTrue("the caller's query must come last", result.endsWith("+contentType:Blog"));
    }

    /**
     * The method has no backslash handling of its own, and none is wanted: putting the defaults
     * first is what makes them safe from whatever the caller's query ends with. This case records
     * that, so a later change that appends them again is caught here and not only in an
     * integration test.
     */
    @Test
    public void test_caller_query_ending_in_a_backslash_keeps_every_default_separate() {
        final String result = addDefaults("+contentType:Blog x\\", false, false);

        assertEquals("+languageId:1 +live:true +deleted:false +contentType:Blog x\\",
                clauses(result));
        assertTrue("the trailing backslash must stay at the end of the query",
                result.endsWith("x\\"));
    }

    @Test
    public void test_edit_mode_defaults_to_working_content() {
        assertTrue(addDefaults("+contentType:Blog", true, false).contains("+working:true"));
    }

    @Test
    public void test_edit_mode_with_the_time_machine_running_defaults_to_live_content() {
        final String result = addDefaults("+contentType:Blog", true, true);

        assertTrue(result.contains("+live:true"));
        assertFalse("the Time Machine reads live content", result.contains("+working:true"));
    }

    @Test
    public void test_null_query_yields_only_the_defaults() {
        assertEquals("+languageId:1 +live:true +deleted:false",
                clauses(addDefaults(null, false, false)));
    }

    @Test
    public void test_a_default_the_query_already_states_is_left_to_the_query() {
        final String result = addDefaults("+languageId:2 +contentType:Blog", false, false);

        assertTrue("the language the caller stated is kept", result.contains("+languageId:2"));
        assertFalse("no second language clause is added", result.contains("+languageId:1"));
    }
}
