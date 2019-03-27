package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetVariantActionlet extends RuleActionlet<SetVariantActionlet.Instance> {

    public SetVariantActionlet(String name) {
        super("Set Variant");
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        // INSERT SOME AWESOMENESS HERE
        return false;
    }

    @Override
    public Instance instanceFrom(Map values) {
        return new Instance();
    }

    static class Instance implements RuleComponentInstance {}
}
