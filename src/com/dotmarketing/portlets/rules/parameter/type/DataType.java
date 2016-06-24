package com.dotmarketing.portlets.rules.parameter.type;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.parameter.type.constraint.TypeConstraint;
import java.util.Collections;
import java.util.Map;

/**
 * @author Geoff M. Granum
 */
public abstract class DataType<T> {

    private final String id;
    private final String errorMessageKey;

    private T defaultValue;
    private Map<String, TypeConstraint> restrictions = Maps.newHashMap();

    public DataType(String id, String errorMessageKey) {
        this.id = id;
        this.errorMessageKey = errorMessageKey;
    }

    public String getId() {
        return id;
    }

    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    public abstract T convert(String from);

    public T getDefaultValue() {
        return defaultValue;
    }

    public DataType<T> defaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Verify that a value can be represented by this DataType. If valid an empty list will
     * be returned. If invalid a map containing each failed constraint will be returned.
     * @param value The string representation of the value to be verified.
     * @return A map of Errors
     */
    public final Map<String, String> verify(String value){
        Map<String, String> errors = null;
        for (TypeConstraint constraint : restrictions.values()) {
            if(!constraint.fn.apply(value)) {
                if(errors == null) {
                    errors = Maps.newHashMap();
                }
                errors.put(constraint.id, value);
            }
        }
        return errors == null ? Collections.emptyMap() : errors;
    }

    public void checkValid(String value){
        Map<String, String> errors = verify(value);
        if(!errors.isEmpty()){
            String failedMsg = "Constraint(s) failed: [" + StringUtils.join(errors.keySet(), ",") + "]";
            throw new ParameterNotValidException(failedMsg);
        }
    }

    public DataType<T> restrict(TypeConstraint restriction) {
        this.restrictions.put(restriction.id, restriction);
        return this;
    }

    public Map<String, TypeConstraint> getConstraints() {
        return ImmutableMap.copyOf(this.restrictions);
    }
}
 
