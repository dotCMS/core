package com.dotcms.rest.validation;

import com.dotcms.repackage.javax.validation.ConstraintValidator;
import com.dotcms.repackage.javax.validation.ConstraintValidatorContext;
import com.dotcms.rest.validation.constraints.FireOn;
import com.dotmarketing.portlets.rules.model.Rule;


public class FireOnValidator implements ConstraintValidator<FireOn, String> {

    public boolean isValid(String value, ConstraintValidatorContext constraintContext) {

        try {
            Rule.FireOn.valueOf(value);
        } catch(IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    @Override
    public void initialize(FireOn fireOn)  {

    }

}