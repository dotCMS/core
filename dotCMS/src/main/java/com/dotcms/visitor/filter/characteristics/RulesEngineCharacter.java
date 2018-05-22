package com.dotcms.visitor.filter.characteristics;

import com.dotmarketing.portlets.rules.business.FiredRule;
import com.dotmarketing.portlets.rules.business.FiredRulesList;
import com.dotmarketing.util.WebKeys;

import java.util.stream.Collectors;

public class RulesEngineCharacter extends AbstractCharacter {



    public RulesEngineCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);
        String rulesRequest = null;
        String rulesSession = null;
        if (request.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST) != null) {
            FiredRulesList firedRulesList = (FiredRulesList) request.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST);
            rulesRequest =
                    String.join(" ", firedRulesList.values().stream().map(FiredRule::getRuleID).collect(Collectors.toList()));
            getMap().put("rulesRequest", rulesRequest);
        }
        if (request.getSession().getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST) != null) {
            FiredRulesList firedRulesList = (FiredRulesList) request.getSession().getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST);
            rulesSession =
                    String.join(" ", firedRulesList.values().stream().map(FiredRule::getRuleID).collect(Collectors.toList()));
            getMap().put("rulesSession", rulesSession);
        }
    }

}
