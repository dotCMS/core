package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.portlets.rules.ParameterDataGen;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.actionlet.RuleActionDataGen;
import com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;

import java.util.List;
import java.util.Random;

/**
 * Created by freddy on 27/01/16.
 */
public class ConditionletTestUtil {

    private Random random = new Random();

    private ConditionGroupDataGen conditionGroupDataGen = new ConditionGroupDataGen();
    private ConditionDataGen conditionDataGen = new ConditionDataGen();
    private RuleDataGen ruleDataGen;
    private List<Rule> rulesToRemove = Lists.newArrayList();

    public Rule createRandomSetResponseHeaderRule (Condition condition, String randomKey, String value , String name) {
        return createRandomSetResponseHeaderRule(condition, randomKey, value, name, Rule.FireOn.EVERY_REQUEST);
    }

    public Rule createRandomSetResponseHeaderRule (Condition condition, String randomKey, String value , String name, Rule.FireOn fireOn) {

        //Create the rule
        ruleDataGen = new RuleDataGen(fireOn).name(name);
        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        //Creating the conditionlets group
        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();
        //And relating our conditionlet
        condition.setConditionGroup(group.getId());
        conditionDataGen.persist(condition);

        //Creating the Action to execute
        RuleActionDataGen actionDataGen = new RuleActionDataGen().ruleId(rule.getId());
        RuleAction action = actionDataGen.actionlet(SetResponseHeaderActionlet.class).priority(random.nextInt(100) + 1).next();

        ParameterDataGen pDataGen = new ParameterDataGen().ownerId(action.getId());
        action.addParameter(pDataGen.key(SetResponseHeaderActionlet.HEADER_KEY).value(randomKey).next());
        action.addParameter(pDataGen.key(SetResponseHeaderActionlet.HEADER_VALUE).value(value).next());

        actionDataGen.persist(action);

        return rule;
    }

    public void clear(){
        for ( Rule rule : rulesToRemove ) {
            ruleDataGen.remove(rule);
        }
        rulesToRemove.clear();
    }

}
