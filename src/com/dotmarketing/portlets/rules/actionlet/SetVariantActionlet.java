package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import java.util.Map;

public class SetVariantActionlet extends RuleActionlet {
    @Override
    public String getName() {
        return "Set Variant";
    }

    @Override
    public String getHowTo() {
        return "";
    }

    @Override
    public void executeAction(Map<String, RuleActionParameter> params) {
        // INSERT SOME AWESOMENESS HERE
    }
}
