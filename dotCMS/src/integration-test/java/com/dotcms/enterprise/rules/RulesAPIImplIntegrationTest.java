package com.dotcms.enterprise.rules;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.actionlet.RuleActionDataGen;
import com.dotmarketing.portlets.rules.business.RulesCache;
import com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test of {@link RulesAPIImpl}
 */
public class RulesAPIImplIntegrationTest {

    private static User systemUser;
    private static RulesAPI rulesAPI;
    private static RulesCache rulesCache;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        rulesCache = CacheLocator.getRulesCache();
        rulesAPI   = APILocator.getRulesAPI();
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRule(Rule, User, boolean)}
     * When: A not admin user with right permission try to save a new rule
     * Should: Save the new rule
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveNewRule() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).next();

        this.addPermission(role, host, true);
        rulesAPI.saveRule(rule, user, false);

        final List<Rule> rules = rulesAPI.getAllRulesByParent(host, systemUser, true);
        assertEquals(1, rules.size());
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRule(Rule, User, boolean)}
     * When: A not admin user without permission try to save a new rule
     * Should: Not save the new rule
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldNotSaveNewRule() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).next();

        this.addPermission(role, host, false);

        try {
            rulesAPI.saveRule(rule, user, false);
            throw new AssertionError("DotSecurityException expected");
        } catch(DotSecurityException e) {
            //expected//
        }

        final List<Rule> rules = rulesAPI.getAllRulesByParent(host, systemUser, true);
        assertTrue(rules.isEmpty());
    }

    /**
     * Method to Test: {@link RulesAPIImpl#deleteRule(Rule, User, boolean)}
     * Case: Happy path
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testDeleteRuleShouldPass() throws DotSecurityException, DotDataException {
        final long currentTime = System.currentTimeMillis();
        final Rule rule  = new RuleDataGen().name("testDeleteRule" + currentTime).nextPersisted();
        final RuleAction ruleAction = new RuleActionDataGen().ruleId(rule.getId()).nextPersisted();
        final ConditionGroup conditionGroup = new ConditionGroupDataGen().ruleId(rule.getId()).nextPersisted();
        final Condition condition = new ConditionDataGen().groupId(conditionGroup.getId()).nextPersisted();

        rulesAPI.deleteRule(rule, systemUser, false);

        rulesCache.removeRule(rule);
        rulesCache.removeAction(ruleAction);
        rulesCache.removeCondition(condition);
        rulesCache.removeConditionGroup(conditionGroup);

        assertNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
        assertNull(rulesAPI.getRuleActionById(ruleAction.getId(), systemUser, false));
        assertNull(rulesAPI.getConditionById(condition.getId(), systemUser, false));
        assertNull(rulesAPI.getConditionGroupById(conditionGroup.getId(), systemUser, false));


    }

    /**
     * Method to Test: {@link RulesAPIImpl#deleteRule(Rule, User, boolean)}
     * When: a rule is deleted, the delete operation over its dependencies should not fail because of a dirty cache
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testDeleteRuleShouldNotFailWhenCacheIsDirty() throws DotSecurityException, DotDataException {
        final long currentTime = System.currentTimeMillis();

        //creates rule
        final Rule rule  = new RuleDataGen().name("testDeleteRule" + currentTime).nextPersisted();

        //creates rule dependencies
        final RuleAction ruleAction = new RuleActionDataGen().ruleId(rule.getId()).nextPersisted();

        final ConditionGroup conditionGroup = new ConditionGroupDataGen().ruleId(rule.getId()).nextPersisted();

        final Condition condition = new ConditionDataGen().groupId(conditionGroup.getId()).nextPersisted();

        //deletes rule and dependencies
        rulesAPI.deleteRule(rule, systemUser, false);

        //verifies that the rule and its dependencies were deleted
        assertNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
        assertNull(rulesAPI.getRuleActionById(ruleAction.getId(), systemUser, false));
        assertNull(rulesAPI.getConditionById(condition.getId(), systemUser, false));
        assertNull(rulesAPI.getConditionGroupById(conditionGroup.getId(), systemUser, false));

        //setting a fake cache
        rulesCache.addRule(rule);
        rulesCache.putActionsByRule(rule, CollectionsUtils.list(ruleAction));
        rulesCache.putConditionGroupsByRule(rule, CollectionsUtils.list(conditionGroup));
        rulesCache.putConditionsByGroup(conditionGroup, CollectionsUtils.list(condition));

        //tries to delete the rule again
        //it should not throw a runtime exception when rule actions and conditions are obtained because
        //it will hit DB instead of cache. As the rule is not found, none delete operation is executed
        rulesAPI.deleteRule(rule, systemUser, false);
    }

    private void addPermission(final Role role, final Host host, final boolean notAddPublishPermission)
            throws DotDataException, DotSecurityException {

        final List<Permission> permissions = new ArrayList<>();
        final Permission readPermission = new Permission();
        readPermission.setInode(host.getPermissionId());
        readPermission.setRoleId(role.getId());
        readPermission.setPermission(PermissionAPI.PERMISSION_READ);
        permissions.add(readPermission);

        if (notAddPublishPermission) {
            final Permission publishPermission = new Permission();
            publishPermission.setInode(host.getPermissionId());
            publishPermission.setRoleId(role.getId());
            publishPermission.setPermission(PermissionAPI.PERMISSION_PUBLISH);
            publishPermission.setType(Rule.class.getName());

            permissions.add(publishPermission);
        }

        APILocator.getPermissionAPI().save(permissions, host, systemUser, false);
    }
}
