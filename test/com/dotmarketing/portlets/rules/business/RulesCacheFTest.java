package com.dotmarketing.portlets.rules.business;

import com.dotcms.repackage.org.junit.After;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionValueDataGen;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.portlets.rules.model.Rule;

import java.util.*;

import static com.dotcms.repackage.org.junit.Assert.assertNotNull;
import static com.dotcms.repackage.org.junit.Assert.assertNull;

public class RulesCacheFTest {

    private final Rule.FireOn fireOn = Rule.FireOn.EVERY_PAGE;
    private RulesCache cache = CacheLocator.getRulesCache();
    private RuleDataGen ruleDataGen = new RuleDataGen();
    private ConditionGroupDataGen conditionGroupDataGen = new ConditionGroupDataGen();
    private ConditionDataGen conditionDataGen = new ConditionDataGen();
    private ConditionValueDataGen conditionValueDataGen = new ConditionValueDataGen();
    private List<Rule> rulesToRemove = new ArrayList<>();

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's update the rule
        rule.setName("UpdatedRuleName");
        ruleDataGen.persist(rule);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionGroupOfCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's update the condition
        group.setPriority(100);
        FactoryLocator.getRulesFactory().saveConditionGroup(group);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionOfCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's update the condition
        condition.setName("UpdatedConditionName");
        FactoryLocator.getRulesFactory().saveCondition(condition);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionValueOfCachedRuleOnSameHostIsUpdated() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).next();
        ConditionValue value = conditionValueDataGen.key("key").value("value").next();
        condition.addConditionValue(value);
        conditionDataGen.persist(condition);
        value.setConditionId(condition.getId());

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's update the condition value
        value.setValue("updatedValue");
        FactoryLocator.getRulesFactory().saveConditionValue(value);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionGroupOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's delete the condition
        FactoryLocator.getRulesFactory().deleteConditionGroup(group);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionsOfCachedRuleOnSameHostAreDeletedByGroup() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's delete the conditions by group
        FactoryLocator.getRulesFactory().deleteConditionsByGroup(group);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).nextPersisted();

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's delete the condition
        FactoryLocator.getRulesFactory().deleteCondition(condition);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionValueOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).next();
        ConditionValue value = conditionValueDataGen.key("key").value("value").next();
        condition.addConditionValue(value);
        conditionDataGen.persist(condition);
        value.setConditionId(condition.getId());

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's delete the condition value
        FactoryLocator.getRulesFactory().deleteConditionValue(value);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    @Test
    public void testGetRulesByHostFireOnReturnsNullWhenConditionValuesOfCachedRuleOnSameHostIsDeleted() throws Exception {

        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        Condition condition = conditionDataGen.groupId(group.getId()).next();
        ConditionValue value = conditionValueDataGen.key("key").value("value").next();
        condition.addConditionValue(value);
        conditionDataGen.persist(condition);

        // let's add the rule to the cache
        addRuleToHostFireOnCache(rule);

        // we should get a set with our rule
        assertNotNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

        // let's delete the condition value
        FactoryLocator.getRulesFactory().deleteConditionValues(condition);

        // let's check the cache returns null after the update
        assertNull(cache.getRulesByHostFireOn(rule.getHost(), fireOn));

    }

    private void addRuleToHostFireOnCache(Rule rule) {
        Set<Rule> ruleSet = new HashSet<>();
        ruleSet.add(rule);
        cache.addRulesByHostFireOn(ruleSet, rule.getHost(), fireOn);
    }

    @After
    public void tearDown() throws Exception {
        for (Rule rule : rulesToRemove) {
            ruleDataGen.remove(rule);
        }
        rulesToRemove.clear();
    }


}
