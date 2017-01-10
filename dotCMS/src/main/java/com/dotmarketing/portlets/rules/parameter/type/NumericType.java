package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.parameter.type.constraint.StandardConstraints;
import com.dotmarketing.portlets.rules.parameter.type.constraint.TypeConstraint;

/**
 * @author Geoff M. Granum
 */
public class NumericType extends DataType<Number> {

    public NumericType() {
        super("numeric", "api.system.type.numeric");
    }

    public NumericType required(){
        return this.restrict(StandardConstraints.required);
    }

    public NumericType maxValue(double maxValue) {
        return this.restrict(StandardConstraints.max(maxValue));
    }

    public NumericType minValue(double minValue) {
        return this.restrict(StandardConstraints.min(minValue));
    }

    @Override
    public Number convert(String from) {
        return StringUtils.isEmpty(from) ? 0.0 : Double.parseDouble(from);
    }

    @Override
    public void checkValid(String value) {
        if(StringUtils.isNotBlank(value)) {
            try {
                //noinspection ResultOfMethodCallIgnored
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new ParameterNotValidException(e, "Could not parse %s into a numeric type.", value);
            }
        }
        // blank might mean zero.
    }

    /**
     * Overridden for mutable return type.
     */
    @Override
    public NumericType defaultValue(Number defaultValue) {
        return (NumericType)super.defaultValue(defaultValue);
    }

    /**
     * Overridden for mutable return type.
     */
    @Override
    public NumericType restrict(TypeConstraint restriction) {
        return (NumericType)super.restrict(restriction);
    }
}
 
