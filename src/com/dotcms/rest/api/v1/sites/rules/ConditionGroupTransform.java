package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.google.common.base.Function;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;

import java.util.List;

/**
 * @author Geoff M. Granum
 */
public class ConditionGroupTransform {
//    private final Function<ConditionGroup, RestConditionGroup> toRest = new  AppToRest();
//    public ConditionGroupTransform() {}
//
//    public Rule restToApp(RestRule rest) {
//        Rule app = new Rule();
//        return applyRestToApp(rest, app);
//    }
//
//    public Rule applyRestToApp(RestConditionGroup rest, ConditionGroup app) {
//        app.setOperator(Condition.Operator.valueOf(rest.operator));
//        app.setPriority(rest.priority);
//        return app;
//    }
//
//    public RestCondition appToRest(Condition app) {
//        return toRest.apply(app);
//    }
//
//    public Function<Condition, RestCondition> appToRestFn() {
//        return toRest;
//    }
//
//    public static class AppToRest implements Function<Condition, RestCondition> {
//
//        @Override
//        public RestCondition apply(Condition app) {
//            List<String> groupIds = Lists.newArrayList();
//            for (ConditionGroup group : app.getGroups()) {
//                groupIds.add(group.getId());
//            }
//
//            RestRule rest = new RestRule.Builder()
//                                    .key(app.getId())
//                                    .name(app.getName())
//                                    .fireOn(app.getFireOn().getCamelCaseName())
//                                    .shortCircuit(app.isShortCircuit())
//                                    .priority(app.getPriority())
//                                    .enabled(app.isEnabled())
//                                    .groups(groupIds)
//                                    .build();
//
//            //        List<RuleAction> actions = rulesAPI.getRuleActionsByRule(rule.getId(), user, false);
//            //
//            //        for (RuleAction action : actions) {
//            //            groupsJSON.put(action.getId(), new com.dotmarketing.util.json.JSONObject(action, new String[]{"priority"}));
//            //        }
//
//            return rest;
//        }
//    }
}

