package com.dotcms.rest.api;


import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import com.dotcms.rest.exception.ValidationException;

import java.util.Set;

public abstract class Validated {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean isValid(Set<ConstraintViolation<Validated>> violations) {
        return violations.isEmpty();
    }

    @JsonIgnore
    public boolean isValid(){
        return this.isValid(validator.validate(this));
    }

    public void checkValid() {
        Set<ConstraintViolation<Validated>> violations = validator.validate(this);
        if(!violations.isEmpty()) {
            throw new ValidationException(this, violations);
        }
    }

}
