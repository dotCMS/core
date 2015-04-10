package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class VisitorsCountryConditionlet extends Conditionlet {

    private LinkedHashSet<Comparison> comparisons;
    private Map<String, ConditionletInput> inputs;

    @Override
    public String getName() {
        return "Visitor's Country";
    }

    @Override
    public LinkedHashSet<Comparison> getComparisons() {
        if(comparisons !=null)
            return comparisons;

        comparisons = new LinkedHashSet<>();
        comparisons.add(new Comparison("is", "is"));
        comparisons.add(new Comparison("isNot", "isNot"));

        return comparisons;
    }

    @Override
    protected ValidationResult validate(Comparison comparison, ConditionletInputValue inputValue) {
        ValidationResult result = new ValidationResult();
        String inputId = inputValue.getConditionletInputId();

        if(!UtilMethods.isSet(inputId))
            return result;

        ConditionletInput input = inputs.get(inputId);
        String value = inputValue.getValue();

        result.setConditionletInputId(input.getId());

        Set<EntryOption> entries = input.getData();
        for (Iterator<EntryOption> iterator = entries.iterator(); iterator.hasNext(); ) {
            EntryOption entryOption =  iterator.next();
            if(entryOption.getLabel().equals(value)) {
                result.setValid(true);
            }
        }

        result.setErrorMessage("Non valid selected value: " + value);
        return result ;
    }

    @Override
    public ValidationResults validate(Comparison comparison, Set<ConditionletInputValue> inputValues) {

        ValidationResults results = new ValidationResults();

        if(!UtilMethods.isSet(inputValues))
            return results;


        List<ValidationResult> resultList = new ArrayList();

        for (ConditionletInputValue inputValue : inputValues) {
            resultList.add(validate(comparison, inputValue));
        }

        return results;
    }



    @Override
    public Collection<ConditionletInput> getInputs(String comparisonId) {
        if (inputs != null)
            return inputs.values();

        inputs = new LinkedHashMap<>();

        ConditionletInput input = new ConditionletInput();
        input.setId("country");
        input.setMultipleSelectionAllowed(true);

        LinkedHashSet<EntryOption> data = new LinkedHashSet<>();
        data.add(new EntryOption("CR"));
        data.add(new EntryOption("US"));
        data.add(new EntryOption("VE"));

        input.setData(data);
        input.setDefaultValue("US");

        inputs.put(input.getId(), input);

        return inputs.values();
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, String comparisonId, List<ConditionValue> values) {
        return false;
    }
}
