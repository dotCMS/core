package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.type.comparison.Comparison;
import java.util.List;

/**
 * @author Geoff M. Granum
 */
public class NumericType extends DataType implements Comparison.Is<Number>, Comparison.IsNot<Number> {

    private static final long serialVersionUID = 1L;

    public NumericType() {
        super("numeric");
    }

    @Override
    public void checkValid(String value) {
        if(StringUtils.isNotBlank(value)){
            try {
                //noinspection ResultOfMethodCallIgnored
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new ParameterNotValidException(e, "Could not parse %s into a numeric type.", value);
            }
        }
        // blank might mean zero.
    }

    @Override
    public boolean is(Number actual, ParameterModel specified) {
        return false;
    }

    @Override
    public boolean isNot(Number actual, ParameterModel specified) {
        return false;
    }

    @Override
    public boolean perform(String id, List<ParameterModel> params) {
        return false;
    }
}
 
