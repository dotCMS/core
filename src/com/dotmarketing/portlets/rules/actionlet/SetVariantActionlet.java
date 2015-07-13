package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class SetVariantActionlet extends RuleActionlet {

    public SetVariantActionlet(String name) {
        super("Set Variant");
    }

    @Override
    public void executeAction(HttpServletRequest request, Map<String, RuleActionParameter> params) {
        // INSERT SOME AWESOMENESS HERE
    }
}
