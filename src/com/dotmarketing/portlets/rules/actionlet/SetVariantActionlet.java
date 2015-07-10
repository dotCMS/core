package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SetVariantActionlet extends RuleActionlet {

    public SetVariantActionlet(String name) {
        super("Set Variant");
    }

    @Override
    public String getHowTo() {
        return "";
    }

    @Override
    public void executeAction(HttpServletRequest request, Map<String, RuleActionParameter> params) {
        // INSERT SOME AWESOMENESS HERE
    }
}
