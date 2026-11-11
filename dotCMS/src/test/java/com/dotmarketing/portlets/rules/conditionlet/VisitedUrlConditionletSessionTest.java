package com.dotmarketing.portlets.rules.conditionlet;

import static com.dotmarketing.portlets.rules.conditionlet.Conditionlet.COMPARISON_KEY;
import static com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;

@RunWith(DataProviderRunner.class)
public class VisitedUrlConditionletSessionTest extends UnitTestBase {

    /** Covers matching and nonmatching URLs before and after history has been initialized. */
    @DataProvider
    public static Object[][] comparisons() {
        return new Object[][] {
                {IS.getId(), "/current", false, true},
                {IS_NOT.getId(), "/current", false, false},
                {IS.getId(), "/other", false, false},
                {IS_NOT.getId(), "/other", false, true},
                {IS.getId(), "/current", true, true},
                {IS_NOT.getId(), "/current", true, false},
                {IS.getId(), "/previous", true, true},
                {IS_NOT.getId(), "/previous", true, false}
        };
    }

    /** Keeps current-URL matching independent of the removed session listener. */
    @Test
    @UseDataProvider("comparisons")
    public void testEvaluateWithExistingSession(final String comparison, final String pattern,
            final boolean hasHistory, final boolean expected) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestURI()).thenReturn("/current");
        if (hasHistory) {
            final Clickstream history = new Clickstream();
            final ClickstreamRequest previous = new ClickstreamRequest();
            previous.setRequestURI("/previous");
            history.addClickstreamRequest(previous);
            when(session.getAttribute("clickstream")).thenReturn(history);
        }
        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn("site-id");
        final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
        when(hostWebAPI.getCurrentHost(request)).thenReturn(host);

        try (MockedStatic<WebAPILocator> locator = mockStatic(WebAPILocator.class)) {
            locator.when(WebAPILocator::getHostWebAPI).thenReturn(hostWebAPI);
            final VisitedUrlConditionlet conditionlet = new VisitedUrlConditionlet();
            final Map<String, ParameterModel> parameters = Map.of(
                    COMPARISON_KEY, new ParameterModel(COMPARISON_KEY, comparison),
                    PATTERN_URL_INPUT_KEY, new ParameterModel(PATTERN_URL_INPUT_KEY, pattern));

            assertEquals(expected, conditionlet.evaluate(request, response, "index",
                    conditionlet.instanceFrom(parameters)));
            if (hasHistory) {
                verify(session, never()).setAttribute(eq("clickstream"), any());
            } else {
                verify(session).setAttribute(eq("clickstream"), any(Clickstream.class));
            }
        }
    }
}
