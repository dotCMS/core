package com.dotcms.rest.api.v1.sites.ruleengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.actions.ActionResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.actions.RestRuleAction;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.filters.TimeMachineFilter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetRequestAttributeActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetSessionAttributeActionlet;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

/**
 * Tests that session-attribute, request-attribute and response-header rule actions with a
 * reserved name are refused when saved through the REST resource, and that such actions, when
 * already stored, are ignored when their rule fires while every other action and rule keeps
 * running.
 */
public class RuleRefusedActionsIntegrationTest extends IntegrationTestBase {

    private static final String SESSION_KEY = "sessionKey";
    private static final String SESSION_VALUE = "sessionValue";
    private static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();

    private static RulesAPI rulesAPI;
    private static User systemUser;
    private static Role role;
    private static User user;
    private static Host restSite;
    private static Host fireSite;
    private static Rule restRule;
    private static final List<Rule> createdRules = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        rulesAPI = APILocator.getRulesAPI();
        systemUser = APILocator.systemUser();

        restSite = new SiteDataGen().nextPersisted();
        fireSite = new SiteDataGen().nextPersisted();

        role = new RoleDataGen().nextPersisted();
        user = new UserDataGen().roles(role).nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), user);
        addRulesPublishPermissions(role, restSite);

        restRule = new RuleDataGen().host(restSite)
                .name("refused-actions-rest-" + System.currentTimeMillis()).next();
        rulesAPI.saveRule(restRule, user, false);
        createdRules.add(restRule);
    }

    @AfterClass
    public static void cleanUp() {
        for (final Rule rule : createdRules) {
            try {
                rulesAPI.deleteRule(rule, systemUser, false);
            } catch (final Exception e) {
                Logger.warn(RuleRefusedActionsIntegrationTest.class,
                        "Could not delete test rule " + rule.getId() + ": " + e.getMessage());
            }
        }
        createdRules.clear();
        for (final Host site : new Host[]{restSite, fireSite}) {
            if (site == null) {
                continue;
            }
            try {
                APILocator.getHostAPI().archive(site, systemUser, false);
                APILocator.getHostAPI().delete(site, systemUser, false);
            } catch (final Exception e) {
                Logger.warn(RuleRefusedActionsIntegrationTest.class,
                        "Could not delete test site " + site.getIdentifier() + ": " + e.getMessage());
            }
        }
        if (user != null) {
            UserDataGen.remove(user, true);
        }
        if (role != null) {
            RoleDataGen.remove(role, true);
        }
    }

    /**
     * Given a back-end user allowed to edit the site's rules,
     * When they create a session-attribute or request-attribute action with a reserved name,
     * Then the request answers 400 without the name in the message and nothing is stored.
     */
    @Test
    public void createActionWithReservedAttributeNameIsRefused() throws Exception {
        final int storedBefore = storedActions().size();

        for (final String name : List.of(WebKeys.USER_ID, WebKeys.USER, WebKeys.PRINCIPAL_USER_ID,
                WebKeys.LOGIN_TO_EDIT_MODE, OAuthConstants.SESSION_NATIVE_LOGIN,
                TimeMachineFilter.TM_DATE_VAR, TimeMachineFilter.TM_LANG_VAR,
                TimeMachineFilter.TM_HOST_VAR, OAuthConstants.SESSION_STATE,
                restSite.getIdentifier() + DotSamlConstants.SAML_USER_ID)) {
            assertRefused(() -> new ActionResource().add(backEndRequest(), response(),
                    restSite.getIdentifier(), restAction(SetSessionAttributeActionlet.class,
                            SESSION_KEY, name, SESSION_VALUE, "value")), name);
            assertRefused(() -> new ActionResource().add(backEndRequest(), response(),
                    restSite.getIdentifier(), restAction(SetRequestAttributeActionlet.class,
                            SetRequestAttributeActionlet.REQUEST_KEY, name,
                            SetRequestAttributeActionlet.REQUEST_VALUE, "value")), name);
        }

        assertEquals(storedBefore, storedActions().size());
    }

    /**
     * Given a stored session-attribute action with an ordinary name,
     * When it is updated to a reserved name,
     * Then the request answers 400 and the stored action keeps its ordinary name.
     */
    @Test
    public void updateActionToReservedAttributeNameIsRefusedAndLeavesItUnchanged() throws Exception {
        final String ordinaryName = "visitorSegmentUpdate" + System.currentTimeMillis();
        createThroughRest(restAction(SetSessionAttributeActionlet.class,
                SESSION_KEY, ordinaryName, SESSION_VALUE, "returning"));
        final RuleAction stored = findStoredAction(SESSION_KEY, ordinaryName)
                .orElseThrow(() -> new AssertionError("The ordinary action was not stored"));

        assertRefused(() -> new ActionResource().update(backEndRequest(), response(),
                restSite.getIdentifier(), stored.getId(), restAction(SetSessionAttributeActionlet.class,
                        SESSION_KEY, WebKeys.USER_ID, SESSION_VALUE, "returning")), WebKeys.USER_ID);

        final RuleAction reloaded = rulesAPI.getRuleActionById(stored.getId(), systemUser, false);
        assertNotNull(reloaded);
        assertEquals(ordinaryName, reloaded.getParameters().get(SESSION_KEY).getValue());
    }

    /**
     * Given session and request attribute actions with ordinary names and response-header actions
     * for common response headers,
     * When they are created through the REST resource,
     * Then each one is stored as today.
     */
    @Test
    public void ordinaryNamesAndCommonHeadersAreStored() throws Exception {
        final String suffix = String.valueOf(System.currentTimeMillis());

        createThroughRest(restAction(SetSessionAttributeActionlet.class,
                SESSION_KEY, "visitorSegment" + suffix, SESSION_VALUE, "returning"));
        assertTrue(findStoredAction(SESSION_KEY, "visitorSegment" + suffix).isPresent());

        createThroughRest(restAction(SetRequestAttributeActionlet.class,
                SetRequestAttributeActionlet.REQUEST_KEY, "campaign" + suffix,
                SetRequestAttributeActionlet.REQUEST_VALUE, "spring"));
        assertTrue(findStoredAction(SetRequestAttributeActionlet.REQUEST_KEY, "campaign" + suffix)
                .isPresent());

        for (final String header : List.of("Content-Security-Policy", "Strict-Transport-Security",
                "Permissions-Policy", "Referrer-Policy", "Access-Control-Allow-Origin")) {
            final int storedBefore = storedActions().size();
            createThroughRest(restAction(SetResponseHeaderActionlet.class,
                    SetResponseHeaderActionlet.HEADER_KEY, header,
                    SetResponseHeaderActionlet.HEADER_VALUE, "value-" + suffix));
            assertEquals(header, storedBefore + 1, storedActions().size());
        }
    }

    /**
     * Given a response-header action for a cookie-setting header, in any letter case and with
     * surrounding whitespace,
     * When it is created through the REST resource,
     * Then the request answers 400 and nothing is stored.
     */
    @Test
    public void createActionWithCookieHeaderIsRefused() throws Exception {
        final int storedBefore = storedActions().size();

        for (final String header : List.of("Set-Cookie", " set-cookie ", "Set-Cookie2", "SET-COOKIE2")) {
            assertRefused(() -> new ActionResource().add(backEndRequest(), response(),
                    restSite.getIdentifier(), restAction(SetResponseHeaderActionlet.class,
                            SetResponseHeaderActionlet.HEADER_KEY, header,
                            SetResponseHeaderActionlet.HEADER_VALUE, "name=value")), header.trim());
        }

        assertEquals(storedBefore, storedActions().size());
    }

    /**
     * Given two rules on a site, stored without validation: rule A with a reserved session action,
     * a cookie-setting header action and an ordinary request action, and rule B with an ordinary
     * session action,
     * When the site's rules fire, twice,
     * Then the reserved session attribute is neither removed nor written, no cookie-setting header
     * is set, and both ordinary actions run every time.
     */
    @Test
    public void storedRefusedActionsAreIgnoredWhenRulesFire() throws Exception {
        final Rule ruleA = storedRule("refused-actions-fire-a-");
        storeAction(ruleA, SetSessionAttributeActionlet.class, 1,
                SESSION_KEY, WebKeys.USER_ID, SESSION_VALUE, "someone-else");
        storeAction(ruleA, SetResponseHeaderActionlet.class, 2,
                SetResponseHeaderActionlet.HEADER_KEY, "Set-Cookie",
                SetResponseHeaderActionlet.HEADER_VALUE, "name=value");
        storeAction(ruleA, SetRequestAttributeActionlet.class, 3,
                SetRequestAttributeActionlet.REQUEST_KEY, "ruleAOrdinary",
                SetRequestAttributeActionlet.REQUEST_VALUE, "a");

        final Rule ruleB = storedRule("refused-actions-fire-b-");
        storeAction(ruleB, SetSessionAttributeActionlet.class, 1,
                SESSION_KEY, "ruleBOrdinary", SESSION_VALUE, "b");

        fireAndVerify();
        fireAndVerify();
    }

    /**
     * Fires the fire site's every-page rules on fresh mocks and checks that only the ordinary
     * actions ran.
     */
    private static void fireAndVerify() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);

        RulesEngine.fireRules(request, response, fireSite, Rule.FireOn.EVERY_PAGE);

        verify(session, never()).removeAttribute(WebKeys.USER_ID);
        verify(session, never()).setAttribute(eq(WebKeys.USER_ID), any());
        verify(response, never()).setHeader(argThat(name -> name.trim().equalsIgnoreCase("Set-Cookie")),
                anyString());
        verify(request).setAttribute("ruleAOrdinary", "a");
        verify(session).setAttribute("ruleBOrdinary", "b");
    }

    private static Rule storedRule(final String namePrefix) throws Exception {
        final Rule rule = new RuleDataGen().host(fireSite)
                .name(namePrefix + System.currentTimeMillis()).next();
        rulesAPI.saveRule(rule, systemUser, false);
        createdRules.add(rule);
        return rule;
    }

    /**
     * Stores an action straight through the rules API, which does not validate it, as push
     * publishing and the starter import do.
     */
    private static void storeAction(final Rule rule, final Class<? extends RuleActionlet> actionlet,
            final int priority, final String nameKey, final String name, final String valueKey,
            final String value) throws Exception {
        final RuleAction action = new RuleAction();
        action.setRuleId(rule.getId());
        action.setActionlet(actionlet.getSimpleName());
        action.setPriority(priority);
        action.setParameters(List.of(new ParameterModel(nameKey, name),
                new ParameterModel(valueKey, value)));
        rulesAPI.saveRuleAction(action, systemUser, false);
    }

    private static void createThroughRest(final RestRuleAction action) throws Exception {
        final Response response = new ActionResource().add(backEndRequest(), response(),
                restSite.getIdentifier(), action);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    /**
     * Asserts that the call answers 400 and that the message does not repeat the refused name.
     */
    private static void assertRefused(final ThrowingRunnable call, final String refusedName) {
        final WebApplicationException exception = assertThrows(WebApplicationException.class, call);
        assertEquals(BAD_REQUEST, exception.getResponse().getStatus());
        assertFalse(String.valueOf(exception.getMessage()).contains(refusedName));
    }

    private static List<RuleAction> storedActions() throws Exception {
        return rulesAPI.getRuleActionsByRule(restRule.getId(), systemUser, false);
    }

    private static Optional<RuleAction> findStoredAction(final String nameKey, final String name)
            throws Exception {
        return storedActions().stream()
                .filter(action -> action.getParameters() != null
                        && action.getParameters().get(nameKey) != null
                        && name.equals(action.getParameters().get(nameKey).getValue()))
                .findFirst();
    }

    private static RestRuleAction restAction(final Class<? extends RuleActionlet> actionlet,
            final String nameKey, final String name, final String valueKey, final String value) {
        final Map<String, ParameterModel> parameters = new HashMap<>();
        parameters.put(nameKey, new ParameterModel(nameKey, name));
        parameters.put(valueKey, new ParameterModel(valueKey, value));
        return new RestRuleAction.Builder()
                .owningRule(restRule.getId())
                .actionlet(actionlet.getSimpleName())
                .priority(1)
                .parameters(parameters)
                .build();
    }

    private static HttpServletRequest backEndRequest() {
        final HttpServletRequest request = new MockAttributeRequest(new MockHttpRequestIntegrationTest(
                "localhost", "/api/v1/sites/" + restSite.getIdentifier() + "/ruleengine/actions")
                .request()).request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private static HttpServletResponse response() {
        return new MockHttpResponse().response();
    }

    /**
     * Grants the role read, use and publish permissions on the site's rules, as
     * {@code RulesAPIImplIntegrationTest} does.
     */
    private static void addRulesPublishPermissions(final Role role, final Host host) throws Exception {
        final List<Permission> readPermissions = new ArrayList<>();
        final Permission readPermission = permission(role, host, PermissionAPI.PERMISSION_READ);
        readPermission.setType(Rule.class.getName());
        readPermissions.add(readPermission);
        readPermissions.add(permission(role, host, PermissionAPI.PERMISSION_USE));
        APILocator.getPermissionAPI().save(readPermissions, host, systemUser, false);

        final List<Permission> publishPermissions = new ArrayList<>();
        final Permission publishPermission = permission(role, host, PermissionAPI.PERMISSION_PUBLISH);
        publishPermission.setType(Rule.class.getName());
        publishPermissions.add(publishPermission);
        publishPermissions.add(permission(role, host, PermissionAPI.PERMISSION_USE));
        APILocator.getPermissionAPI().save(publishPermissions, host, systemUser, false);
    }

    private static Permission permission(final Role role, final Host host, final int permissionType) {
        final Permission permission = new Permission();
        permission.setInode(host.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(permissionType);
        return permission;
    }
}
