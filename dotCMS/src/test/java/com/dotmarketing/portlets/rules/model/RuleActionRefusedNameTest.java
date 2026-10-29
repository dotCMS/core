package com.dotmarketing.portlets.rules.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.saml.DotSamlConstants;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetRequestAttributeActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetSessionAttributeActionlet;
import com.dotmarketing.portlets.rules.actionlet.StopProcessingActionlet;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

/**
 * Tests how a {@link RuleAction} handles a stored action whose actionlet refuses one of its
 * parameter values: the action is validated without an error and ignored when the rule fires,
 * while actions with ordinary names keep running.
 */
class RuleActionRefusedNameTest {

    private static final String SESSION_KEY = "sessionKey";
    private static final String SESSION_VALUE = "sessionValue";
    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    /**
     * Given a stored session-attribute action with a reserved name,
     * When the rule validates and fires it,
     * Then validation does not throw, the action reports false and the session is not touched.
     */
    @Test
    void reservedSessionAttributeActionIsIgnored() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        final RuleAction action = action(new SetSessionAttributeActionlet(),
                SESSION_KEY, WebKeys.USER_ID, SESSION_VALUE, "value");

        assertDoesNotThrow(action::checkValid);

        assertFalse(action.evaluate(request, null));
        verifyNoInteractions(request);
        verifyNoInteractions(session);
    }

    /**
     * Given a stored request-attribute action with a reserved name,
     * When the rule validates and fires it,
     * Then validation does not throw, the action reports false and the request is not touched.
     */
    @Test
    void reservedRequestAttributeActionIsIgnored() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final RuleAction action = action(new SetRequestAttributeActionlet(),
                SetRequestAttributeActionlet.REQUEST_KEY, WebKeys.USER,
                SetRequestAttributeActionlet.REQUEST_VALUE, "value");

        assertDoesNotThrow(action::checkValid);

        assertFalse(action.evaluate(request, null));
        verifyNoInteractions(request);
    }

    /**
     * Given a stored response-header action for the cookie-setting header,
     * When the rule validates and fires it,
     * Then validation does not throw, the action reports false and the response is not touched.
     */
    @Test
    void reservedResponseHeaderActionIsIgnored() {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final RuleAction action = action(new SetResponseHeaderActionlet(),
                SetResponseHeaderActionlet.HEADER_KEY, "Set-Cookie",
                SetResponseHeaderActionlet.HEADER_VALUE, "name=value");

        assertDoesNotThrow(action::checkValid);

        assertFalse(action.evaluate(null, response));
        verifyNoInteractions(response);
    }

    /**
     * Given a stored response-header action whose name has characters outside the HTTP token set,
     * When the rule validates and fires it,
     * Then validation does not throw, the action reports false and the response is not touched.
     */
    @Test
    void responseHeaderActionWithNonTokenNameIsIgnored() {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final RuleAction action = action(new SetResponseHeaderActionlet(),
                SetResponseHeaderActionlet.HEADER_KEY, "X-Header Name",
                SetResponseHeaderActionlet.HEADER_VALUE, "value");

        assertDoesNotThrow(action::checkValid);

        assertFalse(action.evaluate(null, response));
        verifyNoInteractions(response);
    }

    /**
     * Given a stored session-attribute action named after a site's SAML user key,
     * When the rule validates and fires it,
     * Then validation does not throw, the action reports false and the session is not touched.
     */
    @Test
    void siteSamlUserSessionAttributeActionIsIgnored() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        final RuleAction action = action(new SetSessionAttributeActionlet(),
                SESSION_KEY, SITE_ID + DotSamlConstants.SAML_USER_ID, SESSION_VALUE, "value");

        assertDoesNotThrow(action::checkValid);

        assertFalse(action.evaluate(request, null));
        verifyNoInteractions(request);
        verifyNoInteractions(session);
    }

    /**
     * Given a stored session-attribute action named after a site's SAML user key,
     * When it is asked for the refused name and validated,
     * Then the refusal and its warning name the SAML user key, never the stored text with the
     * site id in front of it.
     */
    @Test
    void siteSamlUserKeyIsReportedAsTheKey() {
        assertEquals(Optional.of(DotSamlConstants.SAML_USER_ID), new SetSessionAttributeActionlet()
                .getRefusedName(parameters(SESSION_KEY, SITE_ID + DotSamlConstants.SAML_USER_ID)));

        final Logger ruleActionLogger = (Logger) LogManager.getLogger(RuleAction.class);
        final Level previousLevel = ruleActionLogger.getLevel();
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        ruleActionLogger.addAppender(appender);
        ruleActionLogger.setLevel(Level.WARN);
        try {
            final RuleAction action = action(new SetSessionAttributeActionlet(),
                    SESSION_KEY, SITE_ID + DotSamlConstants.SAML_USER_ID, SESSION_VALUE, "value");
            action.setActionlet("SetSessionAttributeActionlet-" + UUID.randomUUID());

            action.checkValid();

            final List<String> warnings = appender.messages(Level.WARN);
            assertEquals(1, warnings.size(), () -> "warnings: " + warnings);
            assertTrue(warnings.get(0).contains(DotSamlConstants.SAML_USER_ID));
            assertFalse(warnings.get(0).contains(SITE_ID));
        } finally {
            ruleActionLogger.removeAppender(appender);
            ruleActionLogger.setLevel(previousLevel);
            appender.stop();
        }
    }

    /**
     * Given a session-attribute action with an ordinary name,
     * When the rule validates and fires it,
     * Then the attribute is written with the given value.
     */
    @Test
    void ordinarySessionAttributeActionRuns() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        final RuleAction action = action(new SetSessionAttributeActionlet(),
                SESSION_KEY, "visitorSegment", SESSION_VALUE, "returning");

        action.checkValid();

        assertTrue(action.evaluate(request, null));
        verify(session).setAttribute("visitorSegment", "returning");
    }

    /**
     * Given a stored action with a reserved name,
     * When it is validated more than once, as it is on every request,
     * Then no validation throws and the action stays ignored.
     */
    @Test
    void refusedActionValidatedTwiceDoesNotThrow() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final RuleAction action = action(new SetRequestAttributeActionlet(),
                SetRequestAttributeActionlet.REQUEST_KEY, WebKeys.PRINCIPAL_USER_ID,
                SetRequestAttributeActionlet.REQUEST_VALUE, "value");

        assertDoesNotThrow(action::checkValid);
        assertDoesNotThrow(action::checkValid);

        assertFalse(action.evaluate(request, null));
        verifyNoInteractions(request);
    }

    /**
     * Given a refused action whose name is changed to an ordinary one,
     * When it is validated again and fired,
     * Then it runs and writes the attribute.
     */
    @Test
    void refusedActionRunsAfterItsNameBecomesOrdinary() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final RuleAction action = action(new SetRequestAttributeActionlet(),
                SetRequestAttributeActionlet.REQUEST_KEY, WebKeys.USER_ID,
                SetRequestAttributeActionlet.REQUEST_VALUE, "value");
        action.checkValid();
        assertFalse(action.evaluate(request, null));

        action.setParameters(List.of(
                new ParameterModel(SetRequestAttributeActionlet.REQUEST_KEY, "visitorSegment"),
                new ParameterModel(SetRequestAttributeActionlet.REQUEST_VALUE, "returning")));
        action.checkValid();

        assertTrue(action.evaluate(request, null));
        verify(request).setAttribute("visitorSegment", "returning");
    }

    /**
     * Given an action with an ordinary name that has already run,
     * When its name is changed to a reserved one and it is validated again,
     * Then it is ignored from then on.
     */
    @Test
    void ordinaryActionIsIgnoredAfterItsNameBecomesReserved() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final RuleAction action = action(new SetRequestAttributeActionlet(),
                SetRequestAttributeActionlet.REQUEST_KEY, "visitorSegment",
                SetRequestAttributeActionlet.REQUEST_VALUE, "returning");
        action.checkValid();
        assertTrue(action.evaluate(request, null));

        action.setParameters(List.of(
                new ParameterModel(SetRequestAttributeActionlet.REQUEST_KEY, WebKeys.USER_ID),
                new ParameterModel(SetRequestAttributeActionlet.REQUEST_VALUE, "returning")));
        action.checkValid();

        assertFalse(action.evaluate(request, null));
        verify(request, times(1)).setAttribute(anyString(), any());
        verify(request, never()).setAttribute(eq(WebKeys.USER_ID), any());
    }

    /**
     * Given stored actions refused for the same name,
     * When they are validated several times, as on every request,
     * Then one warning is logged for that name, naming the reserved header and never the stored
     * text or the value.
     */
    @Test
    void refusalIsLoggedOncePerName() {
        final Logger ruleActionLogger = (Logger) LogManager.getLogger(RuleAction.class);
        final Level previousLevel = ruleActionLogger.getLevel();
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        ruleActionLogger.addAppender(appender);
        ruleActionLogger.setLevel(Level.WARN);
        try {
            final String actionletId = "SetResponseHeaderActionlet-" + UUID.randomUUID();
            final RuleAction first = action(new SetResponseHeaderActionlet(),
                    SetResponseHeaderActionlet.HEADER_KEY, " set-cookie ",
                    SetResponseHeaderActionlet.HEADER_VALUE, "stored-value");
            first.setActionlet(actionletId);
            final RuleAction second = action(new SetResponseHeaderActionlet(),
                    SetResponseHeaderActionlet.HEADER_KEY, "SET-COOKIE",
                    SetResponseHeaderActionlet.HEADER_VALUE, "stored-value");
            second.setActionlet(actionletId);

            first.checkValid();
            first.checkValid();
            second.checkValid();

            final List<String> warnings = appender.messages(Level.WARN);
            assertEquals(1, warnings.size(), () -> "warnings: " + warnings);
            assertTrue(warnings.get(0).contains("Set-Cookie"));
            assertFalse(warnings.get(0).contains(" set-cookie "));
            assertFalse(warnings.get(0).contains("stored-value"));
        } finally {
            ruleActionLogger.removeAppender(appender);
            ruleActionLogger.setLevel(previousLevel);
            appender.stop();
        }
    }

    /**
     * Given session, request and response-header actionlets,
     * When they are asked for the refused name of their parameters,
     * Then a reserved name is reported as its canonical constant and ordinary names as empty.
     */
    @Test
    void refusedNameIsReportedAsCanonicalConstant() {
        assertEquals(Optional.of(WebKeys.USER_ID), new SetSessionAttributeActionlet()
                .getRefusedName(parameters(SESSION_KEY, WebKeys.USER_ID)));
        assertEquals(Optional.empty(), new SetSessionAttributeActionlet()
                .getRefusedName(parameters(SESSION_KEY, "visitorSegment")));
        assertEquals(Optional.of(WebKeys.USER), new SetRequestAttributeActionlet()
                .getRefusedName(parameters(SetRequestAttributeActionlet.REQUEST_KEY, WebKeys.USER)));
        assertEquals(Optional.empty(), new SetRequestAttributeActionlet()
                .getRefusedName(parameters(SetRequestAttributeActionlet.REQUEST_KEY, "visitorSegment")));
        assertEquals(Optional.of("Set-Cookie"), new SetResponseHeaderActionlet()
                .getRefusedName(parameters(SetResponseHeaderActionlet.HEADER_KEY, " set-cookie ")));
        assertEquals(Optional.of("Set-Cookie2"), new SetResponseHeaderActionlet()
                .getRefusedName(parameters(SetResponseHeaderActionlet.HEADER_KEY, "SET-COOKIE2")));
        assertEquals(Optional.empty(), new SetResponseHeaderActionlet()
                .getRefusedName(parameters(SetResponseHeaderActionlet.HEADER_KEY, "Content-Security-Policy")));
    }

    /**
     * Given actionlet parameters without the name parameter or with a null name,
     * When the actionlets are asked for the refused name,
     * Then nothing is reported and nothing throws.
     */
    @Test
    void refusedNameIsEmptyWhenTheNameIsMissing() {
        assertEquals(Optional.empty(), new SetSessionAttributeActionlet().getRefusedName(Map.of()));
        assertEquals(Optional.empty(), new SetRequestAttributeActionlet()
                .getRefusedName(parameters(SetRequestAttributeActionlet.REQUEST_KEY, null)));
        assertEquals(Optional.empty(), new SetResponseHeaderActionlet()
                .getRefusedName(parameters(SetResponseHeaderActionlet.HEADER_KEY, null)));
        assertEquals(Optional.empty(), new SetSessionAttributeActionlet().getRefusedName(null));
        assertEquals(Optional.empty(), new SetResponseHeaderActionlet().getRefusedName(null));
        assertEquals(Optional.empty(), new SetResponseHeaderActionlet().getRefusedName(Map.of()));
    }

    /**
     * Given a response-header action whose name is only whitespace,
     * When it is asked for the refused name,
     * Then nothing is reported, so the existing required-name check answers for it.
     */
    @Test
    void blankHeaderNameIsNotReportedAsRefused() {
        assertEquals(Optional.empty(), new SetResponseHeaderActionlet()
                .getRefusedName(parameters(SetResponseHeaderActionlet.HEADER_KEY, "   ")));
    }

    /**
     * Given an actionlet that does not refuse any name,
     * When it is asked for the refused name,
     * Then nothing is reported, so its actions validate and run as before.
     */
    @Test
    void actionletThatRefusesNothingReportsNoRefusedName() {
        assertEquals(Optional.empty(), new StopProcessingActionlet()
                .getRefusedName(parameters(StopProcessingActionlet.RETURN_CODE, "404")));
    }

    private static Map<String, ParameterModel> parameters(final String key, final String value) {
        return Map.of(key, new ParameterModel(key, value));
    }

    /**
     * Builds an action bound to the given actionlet, so no actionlet lookup is needed.
     */
    private static RuleAction action(final RuleActionlet<?> actionlet, final String nameKey,
            final String name, final String valueKey, final String value) {
        final RuleAction action = new RuleAction() {
            @Override
            public RuleActionlet getActionDefinition() {
                return actionlet;
            }
        };
        action.setActionlet(actionlet.getId());
        action.setParameters(List.of(new ParameterModel(nameKey, name),
                new ParameterModel(valueKey, value)));
        return action;
    }

    /**
     * Keeps the level and text of every event logged while it is attached.
     */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<Level> levels = new ArrayList<>();
        private final List<String> texts = new ArrayList<>();

        private CapturingAppender() {
            super("RuleActionRefusedNameTest-appender", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public synchronized void append(final LogEvent event) {
            levels.add(event.getLevel());
            texts.add(event.getMessage().getFormattedMessage());
        }

        private synchronized List<String> messages(final Level level) {
            final List<String> result = new ArrayList<>();
            for (int i = 0; i < levels.size(); i++) {
                if (levels.get(i) == level) {
                    result.add(texts.get(i));
                }
            }
            return result;
        }
    }
}
