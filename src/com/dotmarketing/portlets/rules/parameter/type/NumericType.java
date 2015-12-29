package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;

/**
 * @author Geoff M. Granum
 */
public class NumericType extends DataType {

    public NumericType() {
        super("numeric");
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
 
