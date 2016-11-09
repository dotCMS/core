package com.dotmarketing.portlets.rules.business;

import com.dotcms.LicenseTestUtil;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.rules.ParameterDataGen;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.IntegrationTestInitService;

import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RulesCacheFTest {

    private final Rule.FireOn fireOn = Rule.FireOn.EVERY_PAGE;
    private RulesCache cache = CacheLocator.getRulesCache();
    private RuleDataGen ruleDataGen = new RuleDataGen();
    private ConditionGroupDataGen conditionGroupDataGen = new ConditionGroupDataGen();
    private ConditionDataGen conditionDataGen = new ConditionDataGen();
    private ParameterDataGen parameterDataGen = new ParameterDataGen();
    private List<Rule> rulesToRemove = new ArrayList<>();

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's update the rule
        rule.setName("UpdatedRuleName");
        ruleDataGen.persist(rule);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionGroupOfCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's update the condition
        group.setPriority(100);
        FactoryLocator.getRulesFactory().saveConditionGroup(group);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionOfCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's update the condition
        condition.setPriority(99);
        FactoryLocator.getRulesFactory().saveCondition(condition);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionValueOfCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).next();
        ParameterModel value = parameterDataGen.key("key").value("value").next();
        condition.addValue(value);
        condition.addValue(Conditionlet.COMPARISON_KEY, "is");
        conditionDataGen.persist(condition);

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's update the condition value
        value.setOwnerId(condition.getId());
        value.setValue("updatedValue");
        FactoryLocator.getRulesFactory().saveConditionValue(value);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionGroupOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's delete the condition
        FactoryLocator.getRulesFactory().deleteConditionGroup(group);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionsOfCachedRuleOnSameHostAreDeletedByGroup() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's delete the conditions by group
        FactoryLocator.getRulesFactory().deleteConditionsByGroup(group);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's delete the condition
        FactoryLocator.getRulesFactory().deleteCondition(condition);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionValueOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).next();
        ParameterModel value = parameterDataGen.key("key").value("value").next();
        condition.addValue(value);
        condition.addValue(Conditionlet.COMPARISON_KEY, "is");
        conditionDataGen.persist(condition);
        value.setOwnerId(condition.getId());

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's delete the condition value
        FactoryLocator.getRulesFactory().deleteConditionValue(value);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    @Test
    public void testgetRulesByParentFireOnReturnsNullWhenConditionValuesOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).next();
        ParameterModel value = parameterDataGen.ownerId(condition.getId()).key("key").value("value").next();
        condition.addValue(value);
        condition.addValue(Conditionlet.COMPARISON_KEY, "is");
        conditionDataGen.persist(condition);
        value.setOwnerId(condition.getId());

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

        // let's delete the condition value
        FactoryLocator.getRulesFactory().deleteConditionValues(condition);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByParentFireOn(rule.getParent(), fireOn));

    }

    private void addRuleToHostFireOnCache(Rule rule) {
        Set<Rule> ruleSet = new HashSet<>();
        ruleSet.add(rule);
        cache.addRulesByParentFireOn(ruleSet, rule.getParent(), fireOn);
    }

    @After
    public void tearDown() throws Exception {
        for (Rule rule : rulesToRemove) {
            ruleDataGen.remove(rule);
        }
        rulesToRemove.clear();
    }


}
