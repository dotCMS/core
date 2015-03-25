package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class VisitorsCountryConditionlet extends Conditionlet {

    private LinkedHashSet<Comparison> comparisons;
    private Set<ConditionletInput> inputs;
    private Map<String, ConditionletInput> inputMap = new HashMap<String, ConditionletInput>();

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

        ConditionletInput input = inputMap.get(inputId);
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
    public Set<ConditionletInput> getInputs(Comparison comparison) {
        if (inputs != null)
            return inputs;

        inputs = new TreeSet<>();

        ConditionletInput input = new ConditionletInput();
        input.setId("country");
        input.setMultipleSelectionAllowed(true);

        LinkedHashSet<EntryOption> data = new LinkedHashSet<>();
        data.add(new EntryOption("CR"));
        data.add(new EntryOption("US"));
        data.add(new EntryOption("CR"));

        input.setData(data);
        input.setDefaultValue("US");

        inputs.add(input);
        inputMap.put(input.getId(), input);

        return inputs;
    }

    @Override
    public boolean evaluate(Comparison comparison, HttpServletRequest request) {
        return false;
    }
}
