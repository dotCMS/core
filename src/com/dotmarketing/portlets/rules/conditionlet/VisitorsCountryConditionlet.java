package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class VisitorsCountryConditionlet extends Conditionlet {

    private LinkedHashSet<Comparison> comparisons;
    private ConditionletInput conditionletInput;

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
    protected ValidationResult validate(Comparison comparison, String value) {
        ValidationResult result = new ValidationResult();

        Set<EntryOption> entries = conditionletInput.getData();
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
    public ValidationResults validate(Comparison comparison, Collection<String> values) {

        ValidationResults results = new ValidationResults();

        if(!UtilMethods.isSet(values))
            return results;

        List<ValidationResult> resultList = new ArrayList();

        for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
            ValidationResult result = validate(comparison, iterator.next());
            resultList.add(result);
            if(!result.isValid())
                results.setErrors(true);
        }

        return results;

    }



    @Override
    public ConditionletInput getInput(Comparison comparison) {
        if (conditionletInput != null)
            return conditionletInput;

        conditionletInput = new ConditionletInput();
        conditionletInput.setMultipleSelectionAllowed(true);

        LinkedHashSet<EntryOption> data = new LinkedHashSet<>();
        data.add(new EntryOption("CR"));
        data.add(new EntryOption("US"));
        data.add(new EntryOption("CR"));

        conditionletInput.setData(data);
        conditionletInput.setDefaultValue("US");

        return conditionletInput;
    }

    @Override
    public boolean evaluate(Comparison comparison, HttpServletRequest request) {
        return false;
    }
}
