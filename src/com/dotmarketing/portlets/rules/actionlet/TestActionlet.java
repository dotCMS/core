package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestActionlet extends RuleActionlet {
    public TestActionlet() {
        super("TestActionlet");
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, ParameterModel> params) {
        //
    }
}
