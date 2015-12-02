package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.model.ConditionValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MockTrueConditionlet extends Conditionlet {
    private static final String CONDITIONLET_NAME = "MockTrueConditionlet";

    public MockTrueConditionlet() {
        super(CONDITIONLET_NAME);
    }

    @Override
    public Set<Comparison> getComparisons() {
        return new HashSet<>();
    }

    @Override
    public ValidationResults validate(Comparison comparison, Set<ConditionletInputValue> inputValues) {
        return null;
    }

    @Override
    protected ValidationResult validate(Comparison comparison, ConditionletInputValue inputValue) {
        return null;
    }

    @Override
    public Collection<ConditionletInput> getInputs(String comparisonId) {
        return null;
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, String comparisonId, List<ConditionValue> values) {
        return true;
    }
}
