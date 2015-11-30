package com.dotmarketing.portlets.rules.parameter.type;

import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.type.comparison.Comparison;
import java.util.List;

/**
 * @author Geoff M. Granum
 */
public class TextType extends DataType implements Comparison.Is<String>, Comparison.IsNot<String> {

    private static final long serialVersionUID = 1L;

    public TextType() {
        super("text");
    }

    @Override
    public void checkValid(String value) {
        // noop. If you got here, the string is valid. Yes, even if it's null.
        // we'll add length validations and such eventually.
    }

    @Override
    public boolean is(String actual, ParameterModel specified) {
        return false;
    }

    @Override
    public boolean isNot(String actual, ParameterModel specified) {
        return false;
    }

    @Override
    public boolean perform(String id, List<ParameterModel> params) {
        return false;
    }
}
 
