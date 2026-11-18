package com.dotcms.enterprise.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.actionlet.RuleActionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang.SerializationUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests of the Rules permission checks of {@link RulesAPIImpl} on every kind of rule
 * parent: a live or archived site or page, an experiment, and a parent that resolves to nothing.
 */
public class RulesAPIImplParentChecksIntegrationTest {

    private static final int RULES_READ_WRITE_PUBLISH = PermissionAPI.PERMISSION_READ
            | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH;

    private static User systemUser;
    private static RulesAPI rulesAPI;
    private static ContentletAPI contentletAPI;
    private static ExperimentsAPI experimentsAPI;

    private static final List<String> createdRuleIds = new ArrayList<>();
    private static final List<String> createdExperimentIds = new ArrayList<>();
    private static final List<Contentlet> createdContentlets = new ArrayList<>();
    private static final List<Host> createdSites = new ArrayList<>();
    private static final List<User> createdUsers = new ArrayList<>();
    private static final List<Role> createdRoles = new ArrayList<>();
    private static final List<Template> createdTemplates = new ArrayList<>();

    /** A call on the Rules API that is expected to be refused or allowed. */
    @FunctionalInterface
    private interface RulesCall {
        void run() throws Exception;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.systemUser();
        rulesAPI = APILocator.getRulesAPI();
        contentletAPI = APILocator.getContentletAPI();
        experimentsAPI = APILocator.getExperimentsAPI();
    }

    @AfterClass
    public static void cleanUp() {
        for (final String experimentId : createdExperimentIds) {
            try {
                experimentsAPI.forceDelete(experimentId, systemUser);
            } catch (final Exception e) {
                Logger.debug(RulesAPIImplParentChecksIntegrationTest.class,
                        "Unable to remove test experiment " + experimentId + ": " + e.getMessage());
            }
        }

        for (final String ruleId : createdRuleIds) {
            try {
                final Rule rule = rulesAPI.getRuleById(ruleId, systemUser, false);
                if (rule != null) {
                    rulesAPI.deleteRule(rule, systemUser, false);
                }
            } catch (final Exception e) {
                Logger.debug(RulesAPIImplParentChecksIntegrationTest.class,
                        "Unable to remove test rule " + ruleId + ": " + e.getMessage());
            }
        }

        createdContentlets.forEach(contentlet -> ContentletDataGen.destroy(contentlet, true));
        createdTemplates.forEach(TemplateDataGen::remove);
        createdUsers.forEach(user -> UserDataGen.remove(user, true));
        createdRoles.forEach(role -> RoleDataGen.remove(role, true));

        for (final Host site : createdSites) {
            try {
                final Host current = APILocator.getHostAPI().find(site.getIdentifier(), systemUser, false);
                if (current != null) {
                    if (!current.isArchived()) {
                        APILocator.getHostAPI().archive(current, systemUser, false);
                    }
                    APILocator.getHostAPI().delete(current, systemUser, false);
                }
            } catch (final Exception e) {
                Logger.debug(RulesAPIImplParentChecksIntegrationTest.class,
                        "Unable to remove test site " + site.getIdentifier() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Method to test: rule, condition group, condition and action reads and writes of
     * {@link RulesAPIImpl}
     * When: the rule's parent is an archived site and the user has no Rules permission on it
     * Should: refuse every call with a {@link DotSecurityException}
     */
    @Test
    public void archivedSiteRuleRequiresRulesPermission() throws Exception {
        final Host site = newSite();
        final Rule rule = persistRule(new RuleDataGen().host(site));
        final ConditionGroup group = new ConditionGroupDataGen().rule(rule).nextPersisted();
        APILocator.getHostAPI().archive(site, systemUser, false);

        final User user = newUser(newRole());

        assertAllRefused(user, new RuleDataGen().host(site).next(), rule, group);
    }

    /**
     * Method to test: rule, condition group, condition and action reads and writes of
     * {@link RulesAPIImpl}
     * When: the rule's parent is an archived page and the user has no Rules permission on its site
     * Should: refuse every call with a {@link DotSecurityException}
     */
    @Test
    public void archivedPageRuleRequiresRulesPermission() throws Exception {
        final Host site = newSite();
        final HTMLPageAsset page = newPage(site);
        final Rule rule = persistRule(new RuleDataGen().page(page));
        final ConditionGroup group = new ConditionGroupDataGen().rule(rule).nextPersisted();
        ContentletDataGen.archive(page);

        final User user = newUser(newRole());

        assertAllRefused(user, new RuleDataGen().page(page).next(), rule, group);
    }

    /**
     * Method to test: rule, condition group, condition and action reads and writes of
     * {@link RulesAPIImpl}
     * When: the rule's parent is an archived site and the user has Rules PUBLISH on it
     * Should: allow every call
     */
    @Test
    public void archivedSiteRuleAllowedWithRulesPermission() throws Exception {
        final Host site = newSite();
        final Rule rule = persistRule(new RuleDataGen().host(site));
        final ConditionGroup group = new ConditionGroupDataGen().rule(rule).nextPersisted();
        APILocator.getHostAPI().archive(site, systemUser, false);

        final Role role = newRole();
        grantRules(role, site, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        assertAllAllowed(user, new RuleDataGen().host(site).next(), rule, group);
    }

    /**
     * Method to test: rule, condition group, condition and action reads and writes of
     * {@link RulesAPIImpl}
     * When: the rule's parent is an archived page and the user has Rules PUBLISH on its site and
     * PUBLISH on the page
     * Should: allow every call
     */
    @Test
    public void archivedPageRuleAllowedWithRulesPermission() throws Exception {
        final Host site = newSite();
        final HTMLPageAsset page = newPage(site);
        final Rule rule = persistRule(new RuleDataGen().page(page));
        final ConditionGroup group = new ConditionGroupDataGen().rule(rule).nextPersisted();
        ContentletDataGen.archive(page);

        final Role role = newRole();
        grantRules(role, site, RULES_READ_WRITE_PUBLISH);
        grant(role, page, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        assertAllAllowed(user, new RuleDataGen().page(page).next(), rule, group);
    }

    /**
     * Method to test: rule, condition group and action reads and writes of {@link RulesAPIImpl}
     * When: the rule's parent is an experiment and the user has Rules PUBLISH on the site but only
     * READ on the experiment's page
     * Should: refuse every call with a {@link DotSecurityException}
     */
    @Test
    public void experimentRuleRequiresPageEdit() throws Exception {
        final Host site = newSite();
        final HTMLPageAsset page = newPage(site);
        final Experiment experiment = newExperiment(page);
        final Rule rule = persistRuleWithParent(experiment.id().orElseThrow());
        final ConditionGroup group = new ConditionGroupDataGen().rule(rule).nextPersisted();

        final Role role = newRole();
        grantRules(role, site, RULES_READ_WRITE_PUBLISH);
        grant(role, page, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        final User user = newUser(role);

        assertRefused("getRuleById", () -> rulesAPI.getRuleById(rule.getId(), user, false));
        assertRefused("getAllRulesByParent",
                () -> rulesAPI.getAllRulesByParent(experiment, user, false));
        assertRefused("saveConditionGroup", () -> rulesAPI.saveConditionGroup(
                new ConditionGroupDataGen().rule(rule).next(), user, false));
        assertRefused("saveCondition", () -> rulesAPI.saveCondition(
                new ConditionDataGen().group(group).next(), user, false));
        assertRefused("saveRuleAction", () -> rulesAPI.saveRuleAction(
                new RuleActionDataGen().rule(rule).next(), user, false));
        assertRefused("deleteRule", () -> rulesAPI.deleteRule(rule, user, false));

        assertNotNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /**
     * Method to test: rule, condition group and action reads and writes of {@link RulesAPIImpl}
     * When: the rule's parent is an experiment and the user has EDIT on the experiment's page but
     * no Rules permission anywhere
     * Should: allow every call
     */
    @Test
    public void experimentRuleAllowedWithPageEdit() throws Exception {
        final Host site = newSite();
        final HTMLPageAsset page = newPage(site);
        final Experiment experiment = newExperiment(page);
        final Rule rule = persistRuleWithParent(experiment.id().orElseThrow());
        final ConditionGroup group = new ConditionGroupDataGen().rule(rule).nextPersisted();

        final Role role = newRole();
        grant(role, page, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT);
        final User user = newUser(role);

        assertEquals(rule.getId(), rulesAPI.getRuleById(rule.getId(), user, false).getId());
        assertEquals(1, rulesAPI.getAllRulesByParent(experiment, user, false).size());
        rulesAPI.saveConditionGroup(new ConditionGroupDataGen().rule(rule).next(), user, false);
        rulesAPI.saveCondition(new ConditionDataGen().group(group).next(), user, false);
        rulesAPI.saveRuleAction(new RuleActionDataGen().rule(rule).next(), user, false);
        rulesAPI.deleteRule(rule, user, false);

        assertNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /**
     * Method to test: {@link RulesAPIImpl#getRuleById(String, User, boolean)},
     * {@link RulesAPIImpl#getAllRulesByParent(com.dotmarketing.business.Ruleable, User, boolean)}
     * and {@link com.dotcms.experiments.business.ExperimentsAPIImpl#getRule(Experiment)}
     * When: the rule's parent is an experiment and the caller is the system user
     * Should: allow every call
     */
    @Test
    public void experimentRuleAllowedForSystemUser() throws Exception {
        final Host site = newSite();
        final HTMLPageAsset page = newPage(site);
        final Experiment experiment = newExperiment(page);
        final Rule rule = persistRuleWithParent(experiment.id().orElseThrow());

        assertNotNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
        assertEquals(1, rulesAPI.getAllRulesByParent(experiment, systemUser, false).size());

        final Optional<Rule> experimentRule = experimentsAPI.getRule(experiment);
        assertTrue(experimentRule.isPresent());
        assertEquals(rule.getId(), experimentRule.get().getId());
    }

    /**
     * Method to test: rule reads and writes of {@link RulesAPIImpl}
     * When: the rule's parent resolves to no site, page or experiment and the caller is a CMS
     * administrator
     * Should: refuse every call with a {@link DotSecurityException}; the system user is allowed
     */
    @Test
    public void unresolvedParentIsRefused() throws Exception {
        final Rule rule = persistRuleWithParent(UUIDGenerator.generateUuid());
        final ConditionGroup group = new ConditionGroupDataGen().rule(rule).nextPersisted();

        final User admin = newUser(APILocator.getRoleAPI().loadCMSAdminRole());

        assertRefused("getRuleById", () -> rulesAPI.getRuleById(rule.getId(), admin, false));
        assertRefused("saveConditionGroup", () -> rulesAPI.saveConditionGroup(
                new ConditionGroupDataGen().rule(rule).next(), admin, false));
        assertRefused("saveCondition", () -> rulesAPI.saveCondition(
                new ConditionDataGen().group(group).next(), admin, false));
        assertRefused("saveRuleAction", () -> rulesAPI.saveRuleAction(
                new RuleActionDataGen().rule(rule).next(), admin, false));
        assertRefused("deleteRule", () -> rulesAPI.deleteRule(rule, admin, false));

        assertNotNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /**
     * Method to test: {@link RulesAPIImpl#getRuleById(String, User, boolean)} and
     * {@link RulesAPIImpl#deleteRule(Rule, User, boolean)}
     * When: the rule's parent is a contentlet that is neither a site nor a page
     * Should: refuse the calls with a {@link DotSecurityException}; the system user is allowed
     */
    @Test
    public void nonSiteOrPageParentIsRefused() throws Exception {
        final Contentlet content = TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        createdContentlets.add(content);
        final Rule rule = persistRuleWithParent(content.getIdentifier());

        final User admin = newUser(APILocator.getRoleAPI().loadCMSAdminRole());

        assertRefused("getRuleById", () -> rulesAPI.getRuleById(rule.getId(), admin, false));
        assertRefused("deleteRule", () -> rulesAPI.deleteRule(rule, admin, false));

        assertNotNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /**
     * Method to test: {@link ExperimentsAPI#save(Experiment, User)} and
     * {@link ExperimentsAPI#deleteTargetingCondition(String, String, User)}
     * When: a page editor (EDIT on the page, no Rules permission) creates an experiment with a
     * targeting condition, adds a second one and deletes the first
     * Should: every step succeeds and the experiment rule is still found by the system user
     */
    @Test
    public void pageEditorManagesExperimentTargeting() throws Exception {
        final Host site = newSite();
        final Template template = new TemplateDataGen().host(site).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(site, template).nextPersisted();
        createdContentlets.add(page);

        final Role role = newRole();
        grant(role, page, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT);
        grant(role, site, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                PermissionAPI.PERMISSION_EDIT);
        final User editor = newUser(role);

        final Experiment newExperiment = Experiment.builder()
                .from(new ExperimentDataGen().page(page).next())
                .id(UUIDGenerator.generateUuid())
                .targetingConditions(List.of(targetingCondition("first")))
                .build();
        createdExperimentIds.add(newExperiment.id().orElseThrow());

        Experiment saved = experimentsAPI.save(newExperiment, editor);
        assertEquals(1, saved.targetingConditions().orElseThrow().size());

        final List<TargetingCondition> conditions =
                new ArrayList<>(saved.targetingConditions().orElseThrow());
        conditions.add(targetingCondition("second"));
        saved = experimentsAPI.save(saved.withTargetingConditions(conditions), editor);
        assertEquals(2, saved.targetingConditions().orElseThrow().size());

        final String firstConditionId =
                saved.targetingConditions().orElseThrow().get(0).id().orElseThrow();
        saved = experimentsAPI.deleteTargetingCondition(saved.id().orElseThrow(),
                firstConditionId, editor);
        assertEquals(1, saved.targetingConditions().orElseThrow().size());

        final Optional<Rule> experimentRule = experimentsAPI.getRule(saved);
        assertTrue(experimentRule.isPresent());
        createdRuleIds.add(experimentRule.get().getId());
        assertEquals(saved.id().orElseThrow(), experimentRule.get().getParent());
        assertEquals(1, rulesAPI.getAllRulesByParent(saved, systemUser, false).size());
    }

    /**
     * Method to test: {@link ContentletAPI#destroy(Contentlet, User, boolean)}
     * When: a user allowed to destroy an archived page that has rules, holding Rules READ but not
     * Rules PUBLISH on its site, destroys the page
     * Should: the page is destroyed and its rules are removed with it
     */
    @Test
    public void destroyingArchivedPageRemovesItsRules() throws Exception {
        final Host site = newSite();
        final HTMLPageAsset page = newPage(site);
        final Rule rule = persistRule(new RuleDataGen().page(page));
        new ConditionGroupDataGen().rule(rule).nextPersisted();
        ContentletDataGen.archive(page);

        final Role role = newRole();
        grantRules(role, site, PermissionAPI.PERMISSION_READ);
        grant(role, page, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        final Contentlet archivedPage =
                contentletAPI.findContentletByIdentifierAnyLanguage(page.getIdentifier(), true);
        archivedPage.setProperty(Contentlet.DISABLE_WORKFLOW, true);
        contentletAPI.destroy(archivedPage, user, false);

        assertNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /**
     * Method to test: {@link ExperimentsAPI#save(Experiment, User)}
     * When: a page editor who may view the rules of another site saves the experiment with a
     * targeting condition whose id belongs to that site's rule
     * Should: refuse the save and leave the other rule's condition unchanged, in the cache and in
     * the database
     */
    @Test
    public void experimentTargetingOnlyUpdatesTheExperimentsOwnConditions() throws Exception {
        final Host otherSite = newSite();
        final Rule otherRule = persistRule(new RuleDataGen().host(otherSite));
        final ConditionGroup otherGroup = new ConditionGroupDataGen().rule(otherRule).nextPersisted();
        final Condition otherCondition = new ConditionDataGen().group(otherGroup).nextPersisted();
        final String conditionletBefore = otherCondition.getConditionletId();

        final Host site = newSite();
        final Template template = new TemplateDataGen().host(site).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(site, template).nextPersisted();
        createdContentlets.add(page);

        final Role role = newRole();
        grant(role, page, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT);
        grant(role, site, PermissionableType.TEMPLATE_LAYOUTS.getCanonicalName(),
                PermissionAPI.PERMISSION_EDIT);
        grantRules(role, otherSite, PermissionAPI.PERMISSION_READ);
        final User editor = newUser(role);

        final Experiment newExperiment = Experiment.builder()
                .from(new ExperimentDataGen().page(page).next())
                .id(UUIDGenerator.generateUuid())
                .targetingConditions(List.of(targetingCondition("first")))
                .build();
        createdExperimentIds.add(newExperiment.id().orElseThrow());
        final Experiment saved = experimentsAPI.save(newExperiment, editor);

        final TargetingCondition otherRulesCondition = TargetingCondition.builder()
                .from(targetingCondition("changed"))
                .id(otherCondition.getId())
                .build();
        try {
            experimentsAPI.save(saved.withTargetingConditions(List.of(otherRulesCondition)), editor);
            fail("save should be refused");
        } catch (final IllegalArgumentException | DotStateException expected) {
            // refused
        }

        assertEquals(conditionletBefore,
                rulesAPI.getConditionById(otherCondition.getId(), systemUser, false).getConditionletId());
        CacheLocator.getRulesCache().removeCondition(otherCondition);
        assertEquals(conditionletBefore,
                rulesAPI.getConditionById(otherCondition.getId(), systemUser, false).getConditionletId());
    }

    /**
     * Method to test: {@link RulesAPIImpl#saveRuleAction} and {@link RulesAPIImpl#saveCondition}
     * When: a user who may only view the rules of one site, and may publish the rules of another,
     * saves an action and a condition of the first site's rule under the second site's rule
     * Should: refuse both saves and leave the action and the condition where they were
     */
    @Test
    public void movingAnActionOrConditionRequiresPublishOnItsCurrentRule() throws Exception {
        final Host viewOnlySite = newSite();
        final Host publishSite = newSite();
        final Rule viewOnlyRule = persistRule(new RuleDataGen().host(viewOnlySite));
        final Rule publishRule = persistRule(new RuleDataGen().host(publishSite));
        final ConditionGroup viewOnlyGroup = new ConditionGroupDataGen().rule(viewOnlyRule).nextPersisted();
        final ConditionGroup publishGroup = new ConditionGroupDataGen().rule(publishRule).nextPersisted();
        final RuleAction action = new RuleActionDataGen().rule(viewOnlyRule).nextPersisted();
        final Condition condition = new ConditionDataGen().group(viewOnlyGroup).nextPersisted();

        final Role role = newRole();
        grantRules(role, viewOnlySite, PermissionAPI.PERMISSION_READ);
        grantRules(role, publishSite, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        final RuleAction movedAction = (RuleAction) SerializationUtils.clone(action);
        movedAction.setRuleId(publishRule.getId());
        assertRefused("saveRuleAction", () -> rulesAPI.saveRuleAction(movedAction, user, false));

        final Condition movedCondition = (Condition) SerializationUtils.clone(condition);
        movedCondition.setConditionGroup(publishGroup.getId());
        assertRefused("saveCondition", () -> rulesAPI.saveCondition(movedCondition, user, false));

        assertEquals(viewOnlyRule.getId(),
                rulesAPI.getRuleActionById(action.getId(), systemUser, false).getRuleId());
        assertEquals(viewOnlyGroup.getId(),
                rulesAPI.getConditionById(condition.getId(), systemUser, false).getConditionGroup());
    }

    /**
     * Method to test: {@link RulesAPIImpl#saveRuleAction} and {@link RulesAPIImpl#saveCondition}
     * When: a user who may publish the rules of both sites moves an action and a condition from one
     * site's rule to the other's
     * Should: allow both moves
     */
    @Test
    public void movingAnActionOrConditionIsAllowedWithPublishOnBothRules() throws Exception {
        final Host firstSite = newSite();
        final Host secondSite = newSite();
        final Rule firstRule = persistRule(new RuleDataGen().host(firstSite));
        final Rule secondRule = persistRule(new RuleDataGen().host(secondSite));
        final ConditionGroup firstGroup = new ConditionGroupDataGen().rule(firstRule).nextPersisted();
        final ConditionGroup secondGroup = new ConditionGroupDataGen().rule(secondRule).nextPersisted();
        final RuleAction action = new RuleActionDataGen().rule(firstRule).nextPersisted();
        final Condition condition = new ConditionDataGen().group(firstGroup).nextPersisted();

        final Role role = newRole();
        grantRules(role, firstSite, RULES_READ_WRITE_PUBLISH);
        grantRules(role, secondSite, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        final RuleAction movedAction = (RuleAction) SerializationUtils.clone(action);
        movedAction.setRuleId(secondRule.getId());
        rulesAPI.saveRuleAction(movedAction, user, false);

        final Condition movedCondition = (Condition) SerializationUtils.clone(condition);
        movedCondition.setConditionGroup(secondGroup.getId());
        rulesAPI.saveCondition(movedCondition, user, false);

        assertEquals(secondRule.getId(),
                rulesAPI.getRuleActionById(action.getId(), systemUser, false).getRuleId());
        assertEquals(secondGroup.getId(),
                rulesAPI.getConditionById(condition.getId(), systemUser, false).getConditionGroup());
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.HostAPI#delete(Host, User, boolean)}
     * When: a user allowed to delete an archived site that has rules, holding Rules READ but not
     * Rules PUBLISH on it, deletes the site
     * Should: the site is deleted and its rules are removed with it
     */
    @Test
    public void deletingArchivedSiteRemovesItsRules() throws Exception {
        final Host site = newSite();
        final Rule rule = persistRule(new RuleDataGen().host(site));
        new ConditionGroupDataGen().rule(rule).nextPersisted();
        APILocator.getHostAPI().archive(site, systemUser, false);

        final Role role = newRole();
        grantRules(role, site, PermissionAPI.PERMISSION_READ);
        grant(role, site, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        APILocator.getHostAPI().delete(
                APILocator.getHostAPI().find(site.getIdentifier(), systemUser, false), user, false);

        assertNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /**
     * Method to test: {@link RulesAPIImpl#saveConditionGroup}
     * When: a user who may only view the rules of one site, and may publish the rules of another,
     * saves a condition group of the first site's rule under the second site's rule
     * Should: refuse the save and leave the group where it was
     */
    @Test
    public void movingAConditionGroupRequiresPublishOnItsCurrentRule() throws Exception {
        final Host viewOnlySite = newSite();
        final Host publishSite = newSite();
        final Rule viewOnlyRule = persistRule(new RuleDataGen().host(viewOnlySite));
        final Rule publishRule = persistRule(new RuleDataGen().host(publishSite));
        final ConditionGroup viewOnlyGroup = new ConditionGroupDataGen().rule(viewOnlyRule).nextPersisted();

        final Role role = newRole();
        grantRules(role, viewOnlySite, PermissionAPI.PERMISSION_READ);
        grantRules(role, publishSite, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        final ConditionGroup movedGroup = (ConditionGroup) SerializationUtils.clone(viewOnlyGroup);
        movedGroup.setRuleId(publishRule.getId());
        assertRefused("saveConditionGroup", () -> rulesAPI.saveConditionGroup(movedGroup, user, false));

        assertEquals(viewOnlyRule.getId(),
                rulesAPI.getConditionGroupById(viewOnlyGroup.getId(), systemUser, false).getRuleId());
    }

    /**
     * Method to test: {@link RulesAPIImpl#saveConditionValue}
     * When: a user who may only view the rules of one site, and may publish the rules of another,
     * saves a condition value of the first site's condition under the second site's condition
     * Should: refuse the save and leave the value where it was
     */
    @Test
    public void movingAConditionValueRequiresPublishOnItsCurrentRule() throws Exception {
        final Host viewOnlySite = newSite();
        final Host publishSite = newSite();
        final Rule viewOnlyRule = persistRule(new RuleDataGen().host(viewOnlySite));
        final Rule publishRule = persistRule(new RuleDataGen().host(publishSite));
        final ConditionGroup viewOnlyGroup = new ConditionGroupDataGen().rule(viewOnlyRule).nextPersisted();
        final ConditionGroup publishGroup = new ConditionGroupDataGen().rule(publishRule).nextPersisted();
        final Condition viewOnlyCondition = new ConditionDataGen().group(viewOnlyGroup).nextPersisted();
        final Condition publishCondition = new ConditionDataGen().group(publishGroup).nextPersisted();
        final ParameterModel value = persistValue(viewOnlyCondition);

        final Role role = newRole();
        grantRules(role, viewOnlySite, PermissionAPI.PERMISSION_READ);
        grantRules(role, publishSite, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        final ParameterModel movedValue = new ParameterModel(value);
        movedValue.setOwnerId(publishCondition.getId());
        assertRefused("saveConditionValue", () -> rulesAPI.saveConditionValue(movedValue, user, false));

        assertEquals(viewOnlyCondition.getId(),
                rulesAPI.getConditionValueById(value.getId(), systemUser, false).getOwnerId());
    }

    /**
     * Method to test: {@link RulesAPIImpl#saveConditionGroup} and
     * {@link RulesAPIImpl#saveConditionValue}
     * When: a user who may publish the rules of both sites moves a condition group and a condition
     * value from one site's rule to the other's
     * Should: allow both moves
     */
    @Test
    public void movingAConditionGroupOrValueIsAllowedWithPublishOnBothRules() throws Exception {
        final Host firstSite = newSite();
        final Host secondSite = newSite();
        final Rule firstRule = persistRule(new RuleDataGen().host(firstSite));
        final Rule secondRule = persistRule(new RuleDataGen().host(secondSite));
        final ConditionGroup firstGroup = new ConditionGroupDataGen().rule(firstRule).nextPersisted();
        final ConditionGroup secondGroup = new ConditionGroupDataGen().rule(secondRule).nextPersisted();
        final Condition firstCondition = new ConditionDataGen().group(firstGroup).nextPersisted();
        final Condition secondCondition = new ConditionDataGen().group(secondGroup).nextPersisted();
        final ParameterModel value = persistValue(firstCondition);

        final Role role = newRole();
        grantRules(role, firstSite, RULES_READ_WRITE_PUBLISH);
        grantRules(role, secondSite, RULES_READ_WRITE_PUBLISH);
        final User user = newUser(role);

        final ConditionGroup movedGroup = (ConditionGroup) SerializationUtils.clone(firstGroup);
        movedGroup.setRuleId(secondRule.getId());
        rulesAPI.saveConditionGroup(movedGroup, user, false);

        final ParameterModel movedValue = new ParameterModel(value);
        movedValue.setOwnerId(secondCondition.getId());
        rulesAPI.saveConditionValue(movedValue, user, false);

        assertEquals(secondRule.getId(),
                rulesAPI.getConditionGroupById(firstGroup.getId(), systemUser, false).getRuleId());
        assertEquals(secondCondition.getId(),
                rulesAPI.getConditionValueById(value.getId(), systemUser, false).getOwnerId());
    }

    // ---- assertions ----

    /**
     * Asserts that the user is refused on every rule read and write on the parent of the given
     * rule.
     */
    private static void assertAllRefused(final User user, final Rule newRule, final Rule rule,
            final ConditionGroup group) throws Exception {
        assertRefused("saveRule", () -> rulesAPI.saveRule(newRule, user, false));
        assertRefused("getRuleById", () -> rulesAPI.getRuleById(rule.getId(), user, false));
        assertRefused("getAllRulesByParent",
                () -> rulesAPI.getAllRulesByParent(rule.getParent(), user, false));
        assertRefused("saveConditionGroup", () -> rulesAPI.saveConditionGroup(
                new ConditionGroupDataGen().rule(rule).next(), user, false));
        assertRefused("saveCondition", () -> rulesAPI.saveCondition(
                new ConditionDataGen().group(group).next(), user, false));
        assertRefused("saveRuleAction", () -> rulesAPI.saveRuleAction(
                new RuleActionDataGen().rule(rule).next(), user, false));
        assertRefused("deleteRule", () -> rulesAPI.deleteRule(rule, user, false));

        assertNotNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /**
     * Asserts that the user is allowed on every rule read and write on the parent of the given
     * rule; the rule is deleted at the end.
     */
    private static void assertAllAllowed(final User user, final Rule newRule, final Rule rule,
            final ConditionGroup group) throws Exception {
        rulesAPI.saveRule(newRule, user, false);
        createdRuleIds.add(newRule.getId());
        assertEquals(rule.getId(), rulesAPI.getRuleById(rule.getId(), user, false).getId());
        assertTrue(rulesAPI.getAllRulesByParent(rule.getParent(), user, false).stream()
                .anyMatch(listed -> listed.getId().equals(rule.getId())));
        rulesAPI.saveConditionGroup(new ConditionGroupDataGen().rule(rule).next(), user, false);
        rulesAPI.saveCondition(new ConditionDataGen().group(group).next(), user, false);
        rulesAPI.saveRuleAction(new RuleActionDataGen().rule(rule).next(), user, false);
        rulesAPI.deleteRule(rule, user, false);

        assertNull(rulesAPI.getRuleById(rule.getId(), systemUser, false));
    }

    /** Asserts that the call is refused with a {@link DotSecurityException}. */
    private static void assertRefused(final String callName, final RulesCall call)
            throws Exception {
        try {
            call.run();
        } catch (final DotSecurityException expected) {
            return;
        }
        fail(callName + " should be refused");
    }

    // ---- fixtures ----

    /** Creates a site that is removed after the class. */
    private static Host newSite() {
        final Host site = new SiteDataGen().nextPersisted();
        createdSites.add(site);
        return site;
    }

    /** Creates a page in the root folder of the given site. */
    private static HTMLPageAsset newPage(final Host site) {
        final Template template = new TemplateDataGen().nextPersisted();
        createdTemplates.add(template);
        final HTMLPageAsset page = new HTMLPageDataGen(site, template).nextPersisted();
        createdContentlets.add(page);
        return page;
    }

    /** Persists, as the system user, an experiment on the given page. */
    private static Experiment newExperiment(final HTMLPageAsset page) {
        final Experiment experiment = new ExperimentDataGen().page(page).nextPersisted();
        createdExperimentIds.add(experiment.id().orElseThrow());
        return experiment;
    }

    /** Persists, as the system user, the rule built by the given generator. */
    private static Rule persistRule(final RuleDataGen ruleDataGen) {
        final Rule rule = ruleDataGen.nextPersisted();
        createdRuleIds.add(rule.getId());
        return rule;
    }

    /** Persists, as the system user and without parent validation, a rule with the given parent. */
    private static Rule persistRuleWithParent(final String parentId)
            throws DotDataException, DotSecurityException {
        final Rule rule = new Rule();
        rule.setName("rule-" + UUIDGenerator.generateUuid());
        rule.setParent(parentId);
        rule.setFireOn(Rule.FireOn.EVERY_PAGE);
        rule.setEnabled(true);
        rulesAPI.saveRuleNoParentCheck(rule, systemUser, false);
        createdRuleIds.add(rule.getId());
        return rule;
    }

    /** Persists, as the system user, a value of the given condition. */
    private static ParameterModel persistValue(final Condition condition)
            throws DotDataException, DotSecurityException {
        final ParameterModel value = new ParameterModel("comparison", "is");
        value.setOwnerId(condition.getId());
        rulesAPI.saveConditionValue(value, systemUser, false);
        return value;
    }

    /** Builds a targeting condition on a request attribute with the given value. */
    private static TargetingCondition targetingCondition(final String value) {
        return TargetingCondition.builder()
                .conditionKey("RequestAttributeConditionlet")
                .values(Map.of("comparison", "is",
                        "request-attribute", "testing-attribute",
                        "request-attribute-value", value))
                .operator(LogicalOperator.AND)
                .build();
    }

    private static Role newRole() {
        final Role role = new RoleDataGen().nextPersisted();
        createdRoles.add(role);
        return role;
    }

    /** Creates a back-end user holding the given role, as rule authors are. */
    private static User newUser(final Role role) throws DotDataException {
        final User user = new UserDataGen()
                .roles(role, APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        createdUsers.add(user);
        return user;
    }

    /** Grants the given Rules permission on a site (plus READ on the site itself). */
    private static void grantRules(final Role role, final Host site, final int rulesPermission)
            throws DotDataException, DotSecurityException {
        grant(role, site, Rule.class.getName(), rulesPermission);
        grant(role, site, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
    }

    private static void grant(final Role role, final Permissionable permissionable,
            final String type, final int permission)
            throws DotDataException, DotSecurityException {
        APILocator.getPermissionAPI().save(
                new Permission(type, permissionable.getPermissionId(), role.getId(), permission,
                        true),
                permissionable, systemUser, false);
        CacheLocator.getPermissionCache().clearCache();
        CacheLocator.getPermissionCache().flushShortTermCache();
    }
}
