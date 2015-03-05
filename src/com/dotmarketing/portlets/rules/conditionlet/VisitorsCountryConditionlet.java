package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.util.UtilMethods;

import java.util.LinkedHashMap;

public class VisitorsCountryConditionlet extends Conditionlet {

    private LinkedHashMap<String, String> operators;
    private ConditionletInput options;

    @Override
    public String getName() {
        return "Visitor's Country";
    }

    @Override
    public LinkedHashMap<String, String> getOperators() {
        if(operators!=null)
            return operators;

        operators = new LinkedHashMap<>();
        operators.put("is", getLabel(getClass().getName() + ".is") );
        operators.put("isNot", getLabel(getClass().getName() + ".isNot") );

        return operators;
    }

    @Override
    public boolean validate(String operator, String value) {
        return false;
    }

    @Override
    public ConditionletInput getInput(String operator) {
        if (options != null)
            return options;

        options = new ConditionletInput();
        options.setResponseType(ConditionletInput.ResponseType.RAW_DATA);
        options.setInputType(ConditionletInput.InputType.FILTERING_SELECT);
        options.setMultipleChoice(true);

        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("CR", getLabel(getClass().getName() + ".CR"));
        data.put("US", getLabel(getClass().getName() + ".US"));
        data.put("VE", getLabel(getClass().getName() + ".VE"));

        options.setData(data);
        options.setDefaultValue("US");

        return options;
    }

    @Override
    public boolean evaluate(String leftArgument, String operator, String rightArgument) {
        if (!UtilMethods.isSet(leftArgument) || !UtilMethods.isSet(operator) || !UtilMethods.isSet(rightArgument))
            return false;

        switch (operator) {
            case "is":
                return leftArgument.equals(rightArgument);
            case "isNot":
                return !leftArgument.equals(rightArgument);
            default:
                return false;
        }

    }
}
