package com.dotcms.rest.api.v1.sites.ruleengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.RestRule;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.RuleResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.actions.ActionResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.ConditionGroupResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.ConditionResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.ConditionValueResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.RestCondition;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.RestConditionGroup;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.RestConditionValue;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen;
import com.dotmarketing.portlets.rules.conditionlet.UsersCountryConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.UUIDGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that the Rules Engine REST resources ({@link RuleResource}, {@link ConditionGroupResource},
 * {@link ConditionResource} and {@link ConditionValueResource}) require a back-end user, answering
 * exactly as {@link ActionResource} does for front-end and anonymous callers, while back-end users
 * keep the answers they get today: success when they hold Rules permission on the site and 403 when
 * they do not.
 */
public class RuleResourcesAccessIntegrationTest {

    private static final int UNAUTHORIZED = Response.Status.UNAUTHORIZED.getStatusCode();
    private static final int FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static User systemUser;
    private static RulesAPI rulesAPI;

    private static ActionResource actionResource;
    private static RuleResource ruleResource;
    private static ConditionGroupResource conditionGroupResource;
    private static ConditionResource conditionResource;
    private static ConditionValueResource conditionValueResource;

    /** Site where {@link #rulesRole} holds Rules READ and PUBLISH. */
    private static Host authorSite;
    /** Ordinary site where none of the test users hold any permission. */
    private static Host otherSite;

    private static Role rulesRole;

    /** Back-end user holding Rules PUBLISH on {@link #authorSite}. */
    private static User rulesAuthor;
    /** Front-end user (no back-end role) holding the same Rules PUBLISH on {@link #authorSite}. */
    private static User frontEndRulesUser;
    /** Front-end user with no other role. */
    private static User frontEndUser;
    /** Back-end user without any Rules permission. */
    private static User backEndUser;

    private static Rule conditionRule;
    private static ConditionGroup conditionGroup;
    private static Condition condition;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.systemUser();
        rulesAPI = APILocator.getRulesAPI();

        actionResource = new ActionResource();
        ruleResource = new RuleResource();
        conditionGroupResource = new ConditionGroupResource();
        conditionResource = new ConditionResource();
        conditionValueResource = new ConditionValueResource();

        authorSite = new SiteDataGen().nextPersisted();
        otherSite = new SiteDataGen().nextPersisted();

        rulesRole = new RoleDataGen().nextPersisted();
        addRulesPublishPermissions(rulesRole, authorSite);

        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final Role backEndRole = roleAPI.loadBackEndUserRole();
        final Role frontEndRole = roleAPI.loadFrontEndUserRole();

        rulesAuthor = newUser(rulesRole, backEndRole);
        frontEndRulesUser = newUser(rulesRole, frontEndRole);
        frontEndUser = newUser(frontEndRole);
        backEndUser = newUser(backEndRole);

        conditionRule = new RuleDataGen().host(authorSite).nextPersisted();
        conditionGroup = new ConditionGroupDataGen().rule(conditionRule).nextPersisted();
        condition = new ConditionDataGen().group(conditionGroup).nextPersisted();
    }

    @AfterClass
    public static void cleanUp() {
        for (final Host site : new Host[]{authorSite, otherSite}) {
            if (site == null) {
                continue;
            }
            try {
                for (final Rule rule : rulesAPI.getAllRulesByParent(site.getIdentifier(), systemUser, false)) {
                    rulesAPI.deleteRule(rule, systemUser, false);
                }
                APILocator.getHostAPI().archive(site, systemUser, false);
                APILocator.getHostAPI().delete(site, systemUser, false);
            } catch (final Exception e) {
                Logger.warn(RuleResourcesAccessIntegrationTest.class,
                        "Could not remove test site " + site.getIdentifier() + ": " + e.getMessage());
            }
        }
        for (final User user : new User[]{rulesAuthor, frontEndRulesUser, frontEndUser, backEndUser}) {
            if (user != null) {
                UserDataGen.remove(user, true);
            }
        }
        if (rulesRole != null) {
            RoleDataGen.remove(rulesRole, true);
        }
    }

    // ==================== RuleResource ====================

    /**
     * Method to test: {@link RuleResource#list} and {@link RuleResource#add}
     * Given scenario: a front-end user (no back-end role) that holds Rules PUBLISH on the site
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void ruleResource_frontEndUserIsRejected() {
        assertRulesResourceRefuses(frontEndRulesUser);
    }

    /**
     * Method to test: {@link RuleResource#list} and {@link RuleResource#add}
     * Given scenario: a request without any authenticated user
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void ruleResource_anonymousCallerIsRejected() {
        assertRulesResourceRefuses(null);
    }

    /**
     * Method to test: {@link RuleResource#add}, {@link RuleResource#list}, {@link RuleResource#self},
     * {@link RuleResource#update} and {@link RuleResource#remove}
     * Given scenario: a back-end user holding Rules PUBLISH on the site
     * Expected result: the rule is created, listed, read, renamed and deleted as today
     */
    @Test
    public void ruleResource_backEndRulesAuthorManagesRules() throws Exception {
        final String siteId = authorSite.getIdentifier();
        final String ruleName = "rule-access-" + UUIDGenerator.generateUuid();

        final Response created = ruleResource.add(requestFor(rulesAuthor), response(), siteId,
                newRestRule(ruleName));
        assertEquals(Response.Status.OK.getStatusCode(), created.getStatus());
        final String ruleId = readId(created);

        final Map<String, RestRule> listed = ruleResource.list(requestFor(rulesAuthor), response(), siteId);
        assertTrue("The new rule must be listed for its site", listed.containsKey(ruleId));

        final RestRule fetched = ruleResource.self(requestFor(rulesAuthor), response(), siteId, ruleId);
        assertEquals(ruleName, fetched.name);

        final String updatedName = ruleName + "-updated";
        ruleResource.update(requestFor(rulesAuthor), response(), siteId, ruleId,
                new RestRule.Builder().from(fetched).name(updatedName).build());
        assertEquals(updatedName,
                ruleResource.self(requestFor(rulesAuthor), response(), siteId, ruleId).name);

        final Response removed = ruleResource.remove(requestFor(rulesAuthor), response(), siteId, ruleId);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), removed.getStatus());
        assertNull(rulesAPI.getRuleById(ruleId, systemUser, false));
    }

    /**
     * Method to test: {@link RuleResource#add}
     * Given scenario: a back-end user without any Rules permission creates a rule on a site
     * Expected result: {@link ForbiddenException} (403), as today
     */
    @Test
    public void ruleResource_backEndUserWithoutRulesPermissionIsForbidden() {
        final ForbiddenException forbidden = assertThrows(ForbiddenException.class,
                () -> ruleResource.add(requestFor(backEndUser), response(), authorSite.getIdentifier(),
                        newRestRule("rule-access-" + UUIDGenerator.generateUuid())));
        assertEquals(FORBIDDEN, forbidden.getResponse().getStatus());
    }

    /**
     * Method to test: {@link RuleResource#add}
     * Given scenario: a rules author creates a rule on a site where they hold no permission
     * Expected result: {@link ForbiddenException} (403), as today
     */
    @Test
    public void ruleResource_rulesAuthorIsForbiddenOnSiteWithoutPermission() {
        final ForbiddenException forbidden = assertThrows(ForbiddenException.class,
                () -> ruleResource.add(requestFor(rulesAuthor), response(), otherSite.getIdentifier(),
                        newRestRule("rule-access-" + UUIDGenerator.generateUuid())));
        assertEquals(FORBIDDEN, forbidden.getResponse().getStatus());
    }

    /**
     * Method to test: {@link RuleResource#add}
     * Given scenario: a front-end user creates a rule on an ordinary site
     * Expected result: refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void ruleResource_frontEndUserIsRejectedOnOrdinarySite() {
        final String siteId = otherSite.getIdentifier();
        final RestRule body = newRestRule("rule-access-" + UUIDGenerator.generateUuid());
        assertRejectedAsActionResourceDoes(frontEndUser, siteId,
                request -> ruleResource.add(request, response(), siteId, body));
    }

    // ==================== ConditionGroupResource ====================

    /**
     * Method to test: {@link ConditionGroupResource#list} and {@link ConditionGroupResource#add}
     * Given scenario: a front-end user (no back-end role) that holds Rules PUBLISH on the site
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void conditionGroupResource_frontEndUserIsRejected() {
        assertConditionGroupResourceRefuses(frontEndRulesUser);
    }

    /**
     * Method to test: {@link ConditionGroupResource#list} and {@link ConditionGroupResource#add}
     * Given scenario: a request without any authenticated user
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void conditionGroupResource_anonymousCallerIsRejected() {
        assertConditionGroupResourceRefuses(null);
    }

    // ==================== ConditionResource ====================

    /**
     * Method to test: {@link ConditionResource#self} and {@link ConditionResource#add}
     * Given scenario: a front-end user (no back-end role) that holds Rules PUBLISH on the site
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void conditionResource_frontEndUserIsRejected() {
        assertConditionResourceRefuses(frontEndRulesUser);
    }

    /**
     * Method to test: {@link ConditionResource#self} and {@link ConditionResource#add}
     * Given scenario: a request without any authenticated user
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void conditionResource_anonymousCallerIsRejected() {
        assertConditionResourceRefuses(null);
    }

    // ==================== ConditionValueResource ====================

    /**
     * Method to test: {@link ConditionValueResource#list} and {@link ConditionValueResource#add}
     * Given scenario: a front-end user (no back-end role) that holds Rules PUBLISH on the site
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void conditionValueResource_frontEndUserIsRejected() {
        assertConditionValueResourceRefuses(frontEndRulesUser);
    }

    /**
     * Method to test: {@link ConditionValueResource#list} and {@link ConditionValueResource#add}
     * Given scenario: a request without any authenticated user
     * Expected result: both calls are refused with the same 401 {@link ActionResource} gives
     */
    @Test
    public void conditionValueResource_anonymousCallerIsRejected() {
        assertConditionValueResourceRefuses(null);
    }

    // ==================== Condition resources, back-end author ====================

    /**
     * Method to test: {@link ConditionGroupResource#list}, {@link ConditionResource#self} and
     * {@link ConditionValueResource#list}
     * Given scenario: a back-end user holding Rules PUBLISH on the site reads existing condition data
     * Expected result: every call answers 200 with the stored data, as today
     */
    @Test
    public void conditionResources_backEndRulesAuthorReadsConditions() throws Exception {
        final String siteId = authorSite.getIdentifier();

        final Response groups = conditionGroupResource.list(requestFor(rulesAuthor), response(), siteId,
                conditionRule.getId());
        assertEquals(Response.Status.OK.getStatusCode(), groups.getStatus());
        assertTrue("The rule's condition group must be listed",
                ((Map<?, ?>) groups.getEntity()).containsKey(conditionGroup.getId()));

        final Response fetchedCondition = conditionResource.self(requestFor(rulesAuthor), response(), siteId,
                condition.getId());
        assertEquals(Response.Status.OK.getStatusCode(), fetchedCondition.getStatus());
        assertNotNull(fetchedCondition.getEntity());

        final Response values = conditionValueResource.list(requestFor(rulesAuthor), response(), siteId,
                condition.getId());
        assertEquals(Response.Status.OK.getStatusCode(), values.getStatus());
        assertNotNull(values.getEntity());
    }

    // ==================== helpers ====================

    /** A call to a Rules Engine resource made with the given request. */
    @FunctionalInterface
    private interface ResourceCall {
        void invoke(HttpServletRequest request) throws Exception;
    }

    private static void assertRulesResourceRefuses(final User caller) {
        final String siteId = authorSite.getIdentifier();
        final RestRule body = newRestRule("rule-access-" + UUIDGenerator.generateUuid());

        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> ruleResource.list(request, response(), siteId));
        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> ruleResource.add(request, response(), siteId, body));
    }

    private static void assertConditionGroupResourceRefuses(final User caller) {
        final String siteId = authorSite.getIdentifier();
        final RestConditionGroup body = new RestConditionGroup.Builder()
                .operator(LogicalOperator.AND.name())
                .build();

        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> conditionGroupResource.list(request, response(), siteId, conditionRule.getId()));
        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> conditionGroupResource.add(request, response(), siteId, conditionRule.getId(), body));
    }

    private static void assertConditionResourceRefuses(final User caller) {
        final String siteId = authorSite.getIdentifier();
        final RestCondition body = new RestCondition.Builder()
                .owningGroup(conditionGroup.getId())
                .conditionlet(UsersCountryConditionlet.class.getSimpleName())
                .operator(LogicalOperator.AND.name())
                .build();

        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> conditionResource.self(request, response(), siteId, condition.getId()));
        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> conditionResource.add(request, response(), siteId, body));
    }

    private static void assertConditionValueResourceRefuses(final User caller) {
        final String siteId = authorSite.getIdentifier();
        final RestConditionValue body = new RestConditionValue.Builder()
                .key("isoCode")
                .value("US")
                .priority(0)
                .build();

        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> conditionValueResource.list(request, response(), siteId, condition.getId()));
        assertRejectedAsActionResourceDoes(caller, siteId,
                request -> conditionValueResource.add(request, response(), siteId, condition.getId(), body));
    }

    /**
     * Calls {@link ActionResource#self} as the reference and the given resource call with the same
     * caller, and asserts both are refused with a 401 {@link SecurityException}.
     *
     * @param caller the user set on the request, or {@code null} for an anonymous request
     * @param siteId the site id used for the reference call
     * @param call   the resource call under test
     */
    private static void assertRejectedAsActionResourceDoes(final User caller, final String siteId,
            final ResourceCall call) {

        final SecurityException reference = assertThrows(SecurityException.class,
                () -> actionResource.self(requestFor(caller), response(), siteId,
                        UUIDGenerator.generateUuid()));
        assertEquals(UNAUTHORIZED, reference.getResponse().getStatus());

        final SecurityException actual = assertThrows(SecurityException.class,
                () -> call.invoke(requestFor(caller)));
        assertEquals(reference.getResponse().getStatus(), actual.getResponse().getStatus());
    }

    private static RestRule newRestRule(final String name) {
        return new RestRule.Builder()
                .name(name)
                .fireOn(Rule.FireOn.EVERY_PAGE.name())
                .enabled(true)
                .build();
    }

    private static String readId(final Response created) throws Exception {
        return OBJECT_MAPPER.readTree((String) created.getEntity()).get("id").asText();
    }

    /**
     * Builds a fresh request; the given user is set as the logged-in user, or none when
     * {@code null} (anonymous caller).
     */
    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        if (user != null) {
            request.setAttribute(WebKeys.USER, user);
        }
        return request;
    }

    private static HttpServletResponse response() {
        return new MockHttpResponse().response();
    }

    private static User newUser(final Role... roles) {
        return new UserDataGen()
                .emailAddress("rule-access-" + UUIDGenerator.generateUuid() + "@dotcms.com")
                .roles(roles)
                .nextPersisted();
    }

    private static void addRulesPublishPermissions(final Role role, final Host site)
            throws DotDataException, DotSecurityException {

        // Same two-step grant as RulesAPIImplIntegrationTest: Rules READ first, then Rules PUBLISH.
        saveRulesPermission(role, site, PermissionAPI.PERMISSION_READ);
        saveRulesPermission(role, site, PermissionAPI.PERMISSION_PUBLISH);
    }

    private static void saveRulesPermission(final Role role, final Host site, final int permissionType)
            throws DotDataException, DotSecurityException {

        final List<Permission> permissions = new ArrayList<>();

        final Permission rulesPermission = permission(role, site, permissionType);
        rulesPermission.setType(Rule.class.getName());
        permissions.add(rulesPermission);

        permissions.add(permission(role, site, PermissionAPI.PERMISSION_USE));

        APILocator.getPermissionAPI().save(permissions, site, systemUser, false);
    }

    private static Permission permission(final Role role, final Host site, final int permissionType) {
        final Permission permission = new Permission();
        permission.setInode(site.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(permissionType);
        return permission;
    }
}
