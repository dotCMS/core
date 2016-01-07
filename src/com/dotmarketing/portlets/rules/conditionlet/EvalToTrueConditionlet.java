package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class EvalToTrueConditionlet extends Conditionlet<EvalToTrueConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public EvalToTrueConditionlet() {
        super("");
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        return true;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance();
    }

    public static class Instance implements RuleComponentInstance {
    }
}
