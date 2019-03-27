package com.dotcms.rest.api;


import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.javax.validation.ConstraintViolation;
import com.dotcms.repackage.javax.validation.Validation;
import com.dotcms.repackage.javax.validation.Validator;
import com.dotcms.rest.exception.ValidationException;

import java.util.Set;

public abstract class Validated {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean isValid(Set<ConstraintViolation<Validated>> violations) {
        return violations.size() != 0;
    }

    @JsonIgnore
    public boolean isValid(){
        return this.isValid(validator.validate(this));
    }

    public void checkValid() {
        Set<ConstraintViolation<Validated>> violations = validator.validate(this);
        if(violations.size() != 0) {
            throw new ValidationException(this, violations);
        }
    }

}
