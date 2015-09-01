package com.dotcms.rest.validation;

import com.dotcms.repackage.javax.validation.ConstraintValidator;
import com.dotcms.repackage.javax.validation.ConstraintValidatorContext;
import com.dotcms.rest.validation.constraints.Operator;
import com.dotmarketing.portlets.rules.model.Condition;


public class OperatorValidator implements ConstraintValidator<Operator, String> {

    public boolean isValid(String value, ConstraintValidatorContext constraintContext) {

        try {
            Condition.Operator.valueOf(value);
        } catch(IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    @Override
    public void initialize(Operator fireOn)  {

    }

}