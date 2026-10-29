package com.dotmarketing.portlets.rules.actionlet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotcms.filters.interceptor.saml.SamlWebInterceptor;
import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotcms.saml.DotSamlConstants;
import com.dotmarketing.filters.TimeMachineFilter;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.liferay.portal.util.WebKeys;
import java.util.List;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests which session attributes {@link SetSessionAttributeActionlet} accepts and writes.
 */
class SetSessionAttributeActionletTest {

    private static final String SESSION_KEY = "sessionKey";
    private static final String SESSION_VALUE = "sessionValue";
    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    /**
     * Session attribute names that only dotCMS itself sets, including the login-flow keys and the
     * per-site SAML user key.
     */
    static Stream<String> reservedNames() {
        return Stream.of(WebKeys.USER_ID, WebKeys.USER, WebKeys.PRINCIPAL_USER_ID,
                WebKeys.LOGIN_TO_EDIT_MODE, OAuthConstants.SESSION_NATIVE_LOGIN,
                TimeMachineFilter.TM_DATE_VAR, TimeMachineFilter.TM_LANG_VAR,
                TimeMachineFilter.TM_HOST_VAR, OAuthConstants.SESSION_STATE,
                SamlWebInterceptor.ORIGINAL_REQUEST, SITE_ID + DotSamlConstants.SAML_USER_ID);
    }

    /**
     * Given an action that names a session attribute only dotCMS sets,
     * When the action is validated, as it is when it is saved,
     * Then it is rejected with a client error.
     */
    @ParameterizedTest
    @MethodSource("reservedNames")
    void reservedSessionAttributeNameIsRejected(final String name) {
        final SetSessionAttributeActionlet actionlet = new SetSessionAttributeActionlet();

        assertThrows(InvalidRuleParameterException.class,
                () -> actionlet.doCheckValid(action(name, "value")));
    }

    /**
     * Given an action that names an ordinary session attribute,
     * When the rule runs it,
     * Then the attribute is written with the given value.
     */
    @Test
    void ordinarySessionAttributeIsWritten() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);

        validateAndRun(new SetSessionAttributeActionlet(), action("visitorSegment", "returning"), request);

        verify(session).setAttribute("visitorSegment", "returning");
    }

    private static RuleAction action(final String name, final String value) {
        final RuleAction action = new RuleAction();
        action.setParameters(List.of(new ParameterModel(SESSION_KEY, name),
                new ParameterModel(SESSION_VALUE, value)));
        return action;
    }

    /**
     * Validates the action as a save does and evaluates it; a validation error fails the test.
     */
    private static void validateAndRun(final SetSessionAttributeActionlet actionlet,
            final RuleAction action, final HttpServletRequest request) {
        actionlet.evaluate(request, null, actionlet.doCheckValid(action));
    }
}
