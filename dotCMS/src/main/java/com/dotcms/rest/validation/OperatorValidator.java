package com.dotcms.rest.validation;

import com.dotcms.repackage.javax.validation.ConstraintValidator;
import com.dotcms.repackage.javax.validation.ConstraintValidatorContext;
import com.dotcms.rest.validation.constraints.Operator;
import com.dotmarketing.portlets.rules.model.LogicalOperator;

public class OperatorValidator implements ConstraintValidator<Operator, String> {

    public boolean isValid(String value, ConstraintValidatorContext constraintContext) {
        boolean valid;
        try {
            LogicalOperator.valueOf(value);
            valid = true;
        } catch(IllegalArgumentException e) {
            valid = false;
        }
        return valid;
    }

    @Override
    public void initialize(Operator fireOn)  {

    }

}