package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class ThrowErrorActionlet extends RuleActionlet {

    public ThrowErrorActionlet() {
        super("");
        throw new Error();
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {

    }
}
