package com.dotmarketing.portlets.rules.actionlet;

import static com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionlet.HEADER_KEY;
import static com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionlet.HEADER_VALUE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.RuleAction;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests which response headers {@link SetResponseHeaderActionlet} accepts and sets.
 */
class SetResponseHeaderActionletTest {

    /**
     * Given an action that sets a cookie-setting header,
     * When the action is validated, as it is when it is saved,
     * Then it is rejected with a client error.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Set-Cookie", "Set-Cookie2"})
    void cookieHeaderIsRejected(final String header) {
        assertThrows(InvalidRuleParameterException.class,
                () -> new SetResponseHeaderActionlet().doCheckValid(action(header, "name=value")));
    }

    /**
     * Given a cookie-setting header name written in another letter case,
     * When the action is validated,
     * Then it is rejected, since header names are case-insensitive.
     */
    @Test
    void cookieHeaderNameIsMatchedIgnoringCase() {
        assertThrows(InvalidRuleParameterException.class,
                () -> new SetResponseHeaderActionlet().doCheckValid(action("set-cookie", "name=value")));
    }

    /**
     * Given a cookie-setting header name with surrounding whitespace,
     * When the action is validated,
     * Then it is rejected.
     */
    @Test
    void cookieHeaderNameIsMatchedAfterTrimming() {
        assertThrows(InvalidRuleParameterException.class,
                () -> new SetResponseHeaderActionlet().doCheckValid(action(" Set-Cookie ", "name=value")));
    }

    /**
     * Given an action that sets a content security policy, as the starter site's rules do,
     * When the rule runs it,
     * Then the header is set with the given value.
     */
    @Test
    void contentSecurityPolicyHeaderIsSet() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        validateAndRun(action("Content-Security-Policy", "default-src 'self'"), response);

        verify(response).setHeader("Content-Security-Policy", "default-src 'self'");
    }

    /**
     * Given an action that sets a cross-origin header,
     * When the rule runs it,
     * Then the header is set: only cookie-setting headers are refused.
     */
    @Test
    void crossOriginHeaderIsSet() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        validateAndRun(action("Access-Control-Allow-Origin", "https://example.com"), response);

        verify(response).setHeader("Access-Control-Allow-Origin", "https://example.com");
    }

    /**
     * Given a header name with characters outside the HTTP token set, such as a colon, a space, a
     * line break or a non-ASCII letter,
     * When the action is validated,
     * Then it is rejected with a client error.
     */
    @ParameterizedTest
    @ValueSource(strings = {"X-Header:value", "X-Header Name", "X-First\r\nX-Second", "X-H\u00ebader"})
    void headerNameThatIsNotAnHttpTokenIsRejected(final String header) {
        assertThrows(InvalidRuleParameterException.class,
                () -> new SetResponseHeaderActionlet().doCheckValid(action(header, "value")));
    }

    /**
     * Given an ordinary header name with surrounding whitespace,
     * When the rule runs the action,
     * Then the header is set under the name without the whitespace.
     */
    @Test
    void paddedHeaderNameIsSetWithoutWhitespace() {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        validateAndRun(action(" X-Frame-Options ", "SAMEORIGIN"), response);

        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
    }

    /**
     * Given header names that only resemble the cookie-setting header,
     * When the rule runs the action,
     * Then the header is set: only the exact cookie-setting names are refused.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Set-Cookie3", "X-Set-Cookie"})
    void similarHeaderNameIsSet(final String header) {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        validateAndRun(action(header, "value"), response);

        verify(response).setHeader(header, "value");
    }

    private static RuleAction action(final String header, final String value) {
        final RuleAction action = new RuleAction();
        action.setParameters(List.of(new ParameterModel(HEADER_KEY, header),
                new ParameterModel(HEADER_VALUE, value)));
        return action;
    }

    /**
     * Validates the action as a save does and evaluates it; a validation error fails the test.
     */
    private static void validateAndRun(final RuleAction action, final HttpServletResponse response) {
        final SetResponseHeaderActionlet actionlet = new SetResponseHeaderActionlet();
        actionlet.evaluate(null, response, actionlet.doCheckValid(action));
    }
}
