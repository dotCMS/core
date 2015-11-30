package com.dotmarketing.portlets.rules.parameter.type.comparison;

import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.List;

/**
 * @author Geoff M. Granum
 */
public interface Comparison<T> {
    boolean perform(String id, List<ParameterModel> params);

    public interface Is<T> extends Comparison<T> {
        boolean is(T actual, ParameterModel specified);
    }

    public interface IsNot<T> extends Comparison<T> {

        boolean isNot(T actual, ParameterModel specified);
    }
}


