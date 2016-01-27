package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;

/**
 * @author Geoff M. Granum
 */
public class NumericType extends DataType {

    private Integer maxValue;
    private Integer minValue;

    public NumericType() {
        super("numeric", "api.system.type.numeric");
    }

    public NumericType maxValue(int maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public NumericType minValue(int minValue) {
        this.minValue = minValue;
        return this;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public Integer getMinValue() {
        return minValue;
    }

    @Override
    public Object convert(String from) {
        return null;
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
}
 
