package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class VisitorsCountryConditionlet extends Conditionlet {

    private LinkedHashSet<Operator> operators;
    private ConditionletInput conditionletInput;

    @Override
    public String getName() {
        return "Visitor's Country";
    }

    @Override
    public LinkedHashSet<Operator> getOperators() {
        if(operators!=null)
            return operators;

        operators = new LinkedHashSet<>();
        operators.add(new Operator("is", "is"));
        operators.add(new Operator("isNot", "isNot"));

        return operators;
    }

    @Override
    protected ValidationResult validate(Operator operator, String value) {
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
    public ValidationResults validate(Operator operator, Collection<String> values) {

        ValidationResults results = new ValidationResults();

        if(!UtilMethods.isSet(values))
            return results;

        List<ValidationResult> resultList = new ArrayList();

        for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
            ValidationResult result = validate(operator, iterator.next());
            resultList.add(result);
            if(!result.isValid())
                results.setErrors(true);
        }

        return results;

    }



    @Override
    public ConditionletInput getInput(Operator operator) {
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
    public boolean evaluate(Operator operator, HttpServletRequest request) {
        return false;
    }
}
