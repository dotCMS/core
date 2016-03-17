package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import com.dotmarketing.portlets.rules.parameter.type.TextType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

public class VisitorsGeolocationConditionlet extends Conditionlet<VisitorsGeolocationConditionlet.Instance>{

	private static final long serialVersionUID = 1L;

    public static final String DISTANCE_KEY = "distance";
    public static final String UNIT_OF_DISTANCE_KEY = "unit-of-distance";
    public static final String LATITUDE_KEY = "latitude";
    public static final String LONGITUDE = "longitude";

    private static final ParameterDefinition<NumericType> distance = new ParameterDefinition<>(
        1, DISTANCE_KEY, new NumericInput<>(new NumericType()));

    private static final ParameterDefinition<TextType> unitOfDistance = new ParameterDefinition<>(
        2, UNIT_OF_DISTANCE_KEY, new DropdownInput(new TextType().maxLength(10))
            .minSelections(1)
            .maxSelections(1)
            .option(Location.UnitOfDistance.MILES.name())
            .option(Location.UnitOfDistance.KILOMETERS.name()),
            Location.UnitOfDistance.MILES.name()
    );

    private static final ParameterDefinition<TextType> latitude = new ParameterDefinition<>(
            3, LATITUDE_KEY, new TextInput<>(new TextType()));

    private static final ParameterDefinition<TextType> longitude = new ParameterDefinition<>(
            4, LONGITUDE, new TextInput<>(new TextType()));

    public VisitorsGeolocationConditionlet() {
        super("api.ruleengine.system.conditionlet.VisitorsLocation",
              new ComparisonParameterDefinition(2, WITHIN_DISTANCE, NOT_WITHIN_DISTANCE),
                distance);
    }
    
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String parameterActualValue = request.getParameter(instance.parameterKey);
        boolean evalSuccess;
        if(instance.comparison == EXISTS) {
            evalSuccess = EXISTS.perform(parameterActualValue);
        }
        else {
            if(parameterActualValue == null) {
                // treat null and empty string the same, except for 'Exists' case.
                parameterActualValue = "";
            }
            if(instance.comparison != REGEX) {
                //noinspection unchecked
                evalSuccess = instance.comparison.perform(parameterActualValue.toLowerCase(), instance.parameterValue.toLowerCase());
            } else {
                evalSuccess = REGEX.perform(parameterActualValue, instance.parameterValue);
            }
        }
        return evalSuccess;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final double distance;
        public final Location.UnitOfDistance unitOfDistance;
        public final double latitude;
        public final double longitude;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 3,
                       "Request Parameter Condition requires parameters %s, %s and %s.", PARAMETER_NAME_KEY, PARAMETER_VALUE_KEY, COMPARISON_KEY);
            assert parameters != null;
            this.parameterKey = parameters.get(PARAMETER_NAME_KEY).getValue();
            this.parameterValue = parameters.get(PARAMETER_VALUE_KEY).getValue();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }


}
