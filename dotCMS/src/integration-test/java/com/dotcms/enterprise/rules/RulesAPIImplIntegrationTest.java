package com.dotcms.enterprise.rules;

import static com.dotcms.util.CollectionsUtils.list;
import static org.jgroups.util.Util.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.actionlet.RuleActionDataGen;
import com.dotmarketing.portlets.rules.business.RulesCache;
import com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

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
     * Method to Test: {@link RulesAPIImpl#saveConditionGroup(ConditionGroup, User, boolean)}
     *                  and {@link RulesAPIImpl#saveCondition(Condition, User, boolean)}
     * When: User with permission try to create a Group Condition and a Condition
     * Should: Delete it
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveCondition() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final ConditionGroup conditionGroup = new ConditionGroupDataGen().rule(rule).next();
        addRulesPublishPermissions(role, host);

        //Saving and testing GroupCondition
        rulesAPI.saveConditionGroup(conditionGroup, user, false);

        List<Rule> allRules = rulesAPI.getAllRules(user, false);
        createConditionAndCheck(user, conditionGroup, allRules);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveConditionGroup(ConditionGroup, User, boolean)}
     *                  and {@link RulesAPIImpl#saveCondition(Condition, User, boolean)}
     * When:Try to create a Group Condition and a Condition in a page's rule with PuBLISH permission over the page
     * Should: save the rule
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveConditionInRulesPage() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .nextPersisted();

        addPermission(role, htmlPageAsset, PermissionAPI.PERMISSION_PUBLISH);
        addRulesPublishPermissions(role, host);

        final Rule rule = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        final ConditionGroup conditionGroup = new ConditionGroupDataGen().rule(rule).next();

        //Saving and testing GroupCondition
        rulesAPI.saveConditionGroup(conditionGroup, user, false);

        List<Rule> allRules = rulesAPI.getAllRules(user, false);
        createConditionAndCheck(user, conditionGroup, allRules);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveConditionGroup(ConditionGroup, User, boolean)}
     *                  and {@link RulesAPIImpl#saveCondition(Condition, User, boolean)}
     * When:Try to create a Group Condition and a Condition in a page's rule without PuBLISH permission over the page
     * Should: Throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldNotSaveConditionInRuleOnPage() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .folder(folder)
                .nextPersisted();

        addRulesPublishPermissions(role, host);
        final Rule rule = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        final ConditionGroup conditionGroup = new ConditionGroupDataGen().rule(rule).next();

        //Saving and testing GroupCondition
        rulesAPI.saveConditionGroup(conditionGroup, user, false);
    }

    private void createConditionAndCheck(
            final User user,
            final ConditionGroup conditionGroup,
            final List<Rule> rules) throws DotDataException, DotSecurityException {

        List<Rule> allRules = new ArrayList<>(rules);

        assertEquals(1, allRules.size());

        final Rule ruleFromDataBase = allRules.get(0);

        final List<ConditionGroup> groups = ruleFromDataBase.getGroups();
        assertEquals(1, groups.size());

        assertEquals(conditionGroup.getOperator(), groups.get(0).getOperator());

        //Saving and testing Condition
        final Condition condition = new ConditionDataGen().group(groups.get(0)).next();

        rulesAPI.saveCondition(condition, user, false);

        allRules = rulesAPI.getAllRules(user, false);
        final List<Condition> conditions = allRules.get(0).getGroups().get(0).getConditions();

        assertEquals(1, conditions.size());

        assertEquals(condition.getOperator(), conditions.get(0).getOperator());
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveConditionGroup(ConditionGroup, User, boolean)}
     * When: User without permission try to create a Group Condition
     * Should: Throw a DotSecurityException
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldNotSaveCondition() throws DotDataException, DotSecurityException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final ConditionGroup conditionGroup = new ConditionGroupDataGen().rule(rule).next();
        rulesAPI.saveConditionGroup(conditionGroup, user, false);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRuleAction(RuleAction, User, boolean)}
     * When: User with permission try to create a Rule's Action
     * Should: Save it
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveRuleAction() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        addRulesPublishPermissions(role, host);

        final RuleAction ruleAction = new RuleActionDataGen().rule(rule).next();

        rulesAPI.saveRuleAction(ruleAction, user, false);

        final List<Rule> allRules = rulesAPI.getAllRules(user, false);
        checkActions(ruleAction, allRules);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRuleAction(RuleAction, User, boolean)}
     * When: User with permission try to create a Rule's Action in a Rule's Page
     * Should: Save it
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveRuleActionInPage() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

         final Template template = new TemplateDataGen().nextPersisted();
         final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .nextPersisted();

        final Rule rule = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        addRulesPublishPermissions(role, host);
        this.addPermission(role, htmlPageAsset, PermissionLevel.PUBLISH.getType());
        final RuleAction ruleAction = new RuleActionDataGen().rule(rule).next();

        rulesAPI.saveRuleAction(ruleAction, user, false);

        final List<Rule> allRules = rulesAPI.getAllRules(user, false);
        checkActions(ruleAction, allRules);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRuleAction(RuleAction, User, boolean)}
     * When: User with permission try to create a Rule's Action but without PUBLISH permission over the page
     * Should: throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test (expected = DotSecurityException.class)
    public void shouldNotSaveRuleActionInRuleOnPage() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .folder(folder)
                .nextPersisted();

        final Rule rule = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        addRulesPublishPermissions(role, host);
        final RuleAction ruleAction = new RuleActionDataGen().rule(rule).next();

        rulesAPI.saveRuleAction(ruleAction, user, false);
    }

    private void checkActions(final RuleAction ruleAction, final List<Rule> allRules) {
        final List<RuleAction> ruleActions = allRules.get(0).getRuleActions();

        assertEquals(1, ruleActions.size());

        assertEquals(ruleAction.getActionDefinition(), ruleActions.get(0).getActionDefinition());
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRuleAction(RuleAction, User, boolean)}
     * When: User without permission try to create a Rule's Action
     * Should: Throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldNotSaveRuleAction() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        final RuleAction ruleAction = new RuleActionDataGen().rule(rule).next();

        rulesAPI.saveRuleAction(ruleAction, user, false);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#deleteRule(Rule, User, boolean)}
     * When: User with permission try to delete a rule
     * Should: Delete it
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldDeleteRules() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        addRulesPublishPermissions(role, host);

        assertTrue(existRule(rule));

        rulesAPI.deleteRule(rule, user, false);

        assertFalse(existRule(rule));
    }

    /**
     * Method to Test: {@link RulesAPIImpl#deleteRule(Rule, User, boolean)}
     * When: User with permission try to delete a rule's page
     * Should: Delete it
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldDeleteRulesInPage() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .nextPersisted();

        final Rule rule = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        addRulesPublishPermissions(role, host);
        this.addPermission(role, htmlPageAsset, PermissionAPI.PERMISSION_PUBLISH);

        assertTrue(existRule(rule));

        rulesAPI.deleteRule(rule, user, false);

        assertFalse(existRule(rule));

    }

    /**
     * Method to Test: {@link RulesAPIImpl#deleteRule(Rule, User, boolean)}
     * When: User with permission try to delete a rule's page without PUBLISH permission over the page
     * Should:throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void shouldNotDeleteRuleOnPage() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .folder(folder)
                .nextPersisted();

        final Rule rule = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        addRulesPublishPermissions(role, host);

        assertTrue(existRule(rule));

        rulesAPI.deleteRule(rule, user, false);

        assertFalse(existRule(rule));

    }

    /**
     * Method to Test: {@link RulesAPIImpl#deleteRule(Rule, User, boolean)}
     * When: User without permission try to delete a rule
     * Should: throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldNotDeleteRules() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).nextPersisted();

        try {
            rulesAPI.deleteRule(rule, user, false);
            throw new AssertionError("DotSecurityException Expected");
        } catch (DotSecurityException e) {
            assertTrue(existRule(rule));
        }
    }

    private boolean existRule(final Rule rule) throws DotDataException, DotSecurityException {
        final Rule ruleById = APILocator.getRulesAPI().getRuleById(rule.getId(), APILocator.systemUser(), false);
        return ruleById != null;
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(Ruleable, User, boolean)}
     * When: User with permission try to get all the rules from host
     * Should: Return all the rules from the host
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldGetRules() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule1 = new RuleDataGen().host(host).nextPersisted();
        final Rule rule2 = new RuleDataGen().host(host).nextPersisted();

        final Host anotherHost = new SiteDataGen().nextPersisted();
        final Rule rule3 = new RuleDataGen().host(anotherHost).nextPersisted();

        addRulesReadPermissions(role, host);
        final List<Rule> rules = rulesAPI.getAllRulesByParent(host, user, false);

        assertEquals(2, rules.size());

        final List<String> rulesId = rules.stream().map(rule -> rule.getId()).collect(Collectors.toList());
        assertTrue(rulesId.contains(rule1.getId()));
        assertTrue(rulesId.contains(rule2.getId()));
        assertFalse(rulesId.contains(rule3.getId()));
    }


    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(Ruleable, User, boolean)}
     * When: The anonimous user have permission over the rules and try to get the rules with respectFrontendRoles set to true
     * Should: Return all the rules from the host
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldGetRulesWithAnonymous() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule1 = new RuleDataGen().host(host).nextPersisted();
        final Rule rule2 = new RuleDataGen().host(host).nextPersisted();

        final Host anotherHost = new SiteDataGen().nextPersisted();
        final Rule rule3 = new RuleDataGen().host(anotherHost).nextPersisted();

        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();

        addPermission(role, host, PermissionAPI.PERMISSION_USE);

        addRulesReadPermissions(role, host);

        final List<Rule> rules = rulesAPI.getAllRulesByParent(host, user, true);

        assertEquals(2, rules.size());

        final List<String> rulesId = rules.stream().map(rule -> rule.getId()).collect(Collectors.toList());
        assertTrue(rulesId.contains(rule1.getId()));
        assertTrue(rulesId.contains(rule2.getId()));
        assertFalse(rulesId.contains(rule3.getId()));
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(String, User)} )}
     * When: User with permission try to get all the rules from host
     * Should: Return all the rules from the host
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldGetRulesFromHostId() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule1 = new RuleDataGen().host(host).nextPersisted();
        final Rule rule2 = new RuleDataGen().host(host).nextPersisted();

        final Host anotherHost = new SiteDataGen().nextPersisted();
        final Rule rule3 = new RuleDataGen().host(anotherHost).nextPersisted();

        addRulesReadPermissions(role, host);
        final List<Rule> rules = rulesAPI.getAllRulesByParent(host.getIdentifier(), user, false);

        assertEquals(2, rules.size());

        final List<String> rulesId = rules.stream().map(rule -> rule.getId()).collect(Collectors.toList());
        assertTrue(rulesId.contains(rule1.getId()));
        assertTrue(rulesId.contains(rule2.getId()));
        assertFalse(rulesId.contains(rule3.getId()));
    }


    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(String, User)} )}
     * When: User with permission try to get all the rules from host
     * Should: Return all the rules from the host
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldGetRulesFromPageId() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        addRulesReadPermissions(role, host);

        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .folder(folder)
                .nextPersisted();

        final Rule rule1 = new RuleDataGen().page(htmlPageAsset).nextPersisted();
        final Rule rule2 = new RuleDataGen().page(htmlPageAsset).nextPersisted();
        new RuleDataGen().host(host).nextPersisted();

        final HTMLPageAsset anotherHtmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule rule3 = new RuleDataGen().page(anotherHtmlPageAsset).nextPersisted();

        addPermissionToReadRulesFolder(role, folder);

        final List<Rule> rules = rulesAPI.getAllRulesByParent(htmlPageAsset.getIdentifier(), user, false);

        assertEquals(2, rules.size());

        final List<String> rulesId = rules.stream().map(rule -> rule.getId()).collect(Collectors.toList());
        assertTrue(rulesId.contains(rule1.getId()));
        assertTrue(rulesId.contains(rule2.getId()));
        assertFalse(rulesId.contains(rule3.getId()));
    }

    private void addPermissionToReadRulesFolder(final Role role, final Folder folder)
            throws DotDataException, DotSecurityException {

        final Permission folderReadPermission = getPermission(role, folder, PermissionAPI.PERMISSION_READ);

        final Permission folderReadRulePermission = getPermission(role, folder, PermissionAPI.PERMISSION_READ);
        folderReadRulePermission.setType(Rule.class.getName());

        APILocator.getPermissionAPI().save(list(folderReadPermission, folderReadRulePermission), folder, systemUser, false);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(Ruleable, User, boolean)}
     * When: User without permission try to get all the rules from host
     * Should: Throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldThrowDotSecurityException() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        new RuleDataGen().host(host).nextPersisted();

        try {
            rulesAPI.getAllRulesByParent(host, user, false);
            throw new AssertionError("DotSecurityException expected");
        } catch (DotSecurityException e) {
            assertEquals("User " + user.getUserId() + " does not have permissions to VIEW Rules", e.getMessage());
        }
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(String, User)}
     * When: User without permission try to get all the rules from host
     * Should: Throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test (expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionFromHostid() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        new RuleDataGen().host(host).nextPersisted();

        rulesAPI.getAllRulesByParent(host.getIdentifier(), user, false);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(String, User)}
     * When: User without permission try to get all the rules from page
     * Should: Throw a {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test (expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionFromPageid() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        new RuleDataGen().page(htmlPageAsset).nextPersisted();

        rulesAPI.getAllRulesByParent(htmlPageAsset.getIdentifier(), user, false);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(Ruleable, User, boolean)}
     * When: The parent host does not have any rules
     * Should: Return a empty list
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldReturnEmptyList() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        addRulesReadPermissions(role, host);
        final List<Rule> rules = rulesAPI.getAllRulesByParent(host, user, false);

        assertTrue( rules.isEmpty());
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(Ruleable, User, boolean)}
     * When: The parent is not a site, or a page
     * Should: Return an empty list
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void not_site_not_page_shouldReturnEmptyList()
            throws DotSecurityException, DotDataException {

        final Contentlet notSiteOrPage = TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());

        final List<Rule> rules = rulesAPI.getAllRulesByParent(notSiteOrPage, systemUser, false);
        assertTrue(rules.isEmpty());
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRule(Rule, User, boolean)}
     * When: A not admin user with right permission try to save a new rule in a Page
     * Should: Save the new rule
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveNewRuleInPage() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule rule = new RuleDataGen().page(htmlPageAsset).next();
        addRulesPublishPermissions(role, host);

        addPermission(role, htmlPageAsset, PermissionAPI.PERMISSION_PUBLISH);

        rulesAPI.saveRule(rule, user, false);

        final List<Rule> rules = rulesAPI.getAllRulesByParent(htmlPageAsset, systemUser, true);
        assertEquals(1, rules.size());

        final List<String> rulesId = rules.stream().map(pageRule -> pageRule.getId()).collect(Collectors.toList());
        assertTrue(rulesId.contains(rule.getId()));
    }

    @NotNull
    private void addPermission(
            final Role role,
            final Permissionable permissionable,
            final int permissionPublish) {

        final Permission publishPermission = getPermission(role, permissionable, permissionPublish);

        try {
            APILocator.getPermissionAPI().save(publishPermission, permissionable, systemUser, false);
        } catch (DotDataException | DotSecurityException e){
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Permission getPermission(Role role, Permissionable permissionable, int permissionPublish) {
        final Permission publishPermission = new Permission();
        publishPermission.setInode(permissionable.getPermissionId());
        publishPermission.setRoleId(role.getId());
        publishPermission.setPermission(permissionPublish);
        return publishPermission;
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRule(Rule, User, boolean)}
     * When: A user with permission try to save a rule in a page without publish permission over the page but with RULES
     *      permission over the host
     * Should: throw {@link DotSecurityException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveNewRuleInPageShouldNotWork() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        final Rule rule = new RuleDataGen().page(htmlPageAsset).next();

        addRulesPublishPermissions(role, host);

        try {
            rulesAPI.saveRule(rule, user, false);
            throw new AssertionError("DotSecurityException expected");
        } catch (DotSecurityException e){
            final List<Rule> rules = rulesAPI.getAllRulesByParent(htmlPageAsset, systemUser, true);
            assertTrue(rules.isEmpty());
        }
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

        addRulesPublishPermissions(role, host);
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

        addRulesReadPermissions(role, host);

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
        rulesCache.putActionsByRule(rule, list(ruleAction));
        rulesCache.putConditionGroupsByRule(rule, list(conditionGroup));
        rulesCache.putConditionsByGroup(conditionGroup, list(condition));

        //tries to delete the rule again
        //it should not throw a runtime exception when rule actions and conditions are obtained because
        //it will hit DB instead of cache. As the rule is not found, none delete operation is executed
        rulesAPI.deleteRule(rule, systemUser, false);
    }

    private void addRulesReadPermissions(final Role role, final Host host)
            throws DotDataException, DotSecurityException {

        final List<Permission> permissions = new ArrayList<>();
        final Permission readPermission = getPermission(role, host, PermissionAPI.PERMISSION_READ);
        readPermission.setType(Rule.class.getName());
        permissions.add(readPermission);

        permissions.add(getPermission(role, host, PermissionAPI.PERMISSION_USE));

        APILocator.getPermissionAPI().save(permissions, host, systemUser, false);
    }

    private void addRulesPublishPermissions(final Role role, final Host host)
            throws DotDataException, DotSecurityException {

        addRulesReadPermissions(role, host);

        final List<Permission> permissions = new ArrayList<>();

        final Permission publishPermission = getPermission(role, host, PermissionAPI.PERMISSION_PUBLISH);
        publishPermission.setType(Rule.class.getName());

        permissions.add(publishPermission);

        permissions.add(getPermission(role, host, PermissionAPI.PERMISSION_USE));

        APILocator.getPermissionAPI().save(permissions, host, systemUser, false);
    }

    /**
     * Method to Test: {@link RulesAPIImpl#getAllRulesByParent(Ruleable, User, boolean)}
     * When: User with permission try to get all the rules from a Page in root folder
     * Should: Return all the rules from the page checking host permission
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldGetRulesInRootFolder() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = APILocator.getFolderAPI().findFolderByPath("/", host, APILocator.systemUser(), false);
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .folder(folder)
                .nextPersisted();

        final Rule rule1 = new RuleDataGen().page(htmlPageAsset).nextPersisted();
        final Rule rule2 = new RuleDataGen().page(htmlPageAsset).nextPersisted();

        addRulesReadPermissions(role, host);
        final List<Rule> rules = rulesAPI.getAllRulesByParent(htmlPageAsset, user, false);

        assertEquals(2, rules.size());

        final List<String> rulesId = rules.stream().map(rule -> rule.getId()).collect(Collectors.toList());
        assertTrue(rulesId.contains(rule1.getId()));
        assertTrue(rulesId.contains(rule2.getId()));
    }
}
