package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongUnaryOperator;

/**
 * @author Geoff M. Granum
 */
public class RuleTransform {
//    private final Function<Rule, RestRule> toRest = new  AppToRest();
    private final RulesAPI rulesAPI;

    public RuleTransform() { this(new ApiProvider()); }

    public RuleTransform(ApiProvider apiProvider) { this.rulesAPI = apiProvider.rulesAPI(); }

    public Rule restToApp(RestRule rest) {
        Rule app = new Rule();
        return applyRestToApp(rest, app);
    }

    public Rule applyRestToApp(RestRule rest, Rule app) {
        app.setName(rest.name);
        app.setFireOn(Rule.FireOn.valueOf(rest.fireOn));
        app.setPriority(rest.priority);
        app.setShortCircuit(rest.shortCircuit);
        app.setEnabled(rest.enabled);

        rest.groups.forEach((k,v) ->
                System.out.println("k = " + k)
        );

        return app;
    }

    public RestRule appToRest(Rule app) {
        return toRest.apply(app);
    }

    public Function<Rule, RestRule> appToRestFn() {
        return toRest;
    }

//    public static class AppToRest implements Function<Rule, RestRule> {
//
//        @Override
//        public RestRule apply(Rule app) {
//            Map<String, Boolean> groupIds = new HashMap();
//            for (ConditionGroup group : app.getGroups()) {
//                groupIds.put(group.getId(), Boolean.TRUE);
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

    public final Function<Rule, RestRule> toRest = (app) -> {
        Map<String, Boolean> groupIds = new HashMap();

        app.getGroups().forEach(group -> groupIds.put(group.getId(), Boolean.TRUE));

        RestRule rest = new RestRule.Builder()
                .key(app.getId())
                .name(app.getName())
                .fireOn(app.getFireOn().getCamelCaseName())
                .shortCircuit(app.isShortCircuit())
                .priority(app.getPriority())
                .enabled(app.isEnabled())
                .groups(groupIds)
                .build();

        return rest;
    };


}

