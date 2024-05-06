package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ThrowErrorConditionlet extends Conditionlet {

    public ThrowErrorConditionlet() {
        super("");
        throw new Error("As the name says... " + this.getClass().getName());
    }

    @Override
    public RuleComponentInstance instanceFrom(Map parameters) {
        throw new Error("As the name says... " + this.getClass().getName());
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, RuleComponentInstance instance) {
        throw new Error("As the name says... " + this.getClass().getName());
    }
}