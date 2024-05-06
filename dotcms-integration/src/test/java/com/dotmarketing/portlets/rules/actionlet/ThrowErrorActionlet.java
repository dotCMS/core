package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class ThrowErrorActionlet extends RuleActionlet {

    public ThrowErrorActionlet() {
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
