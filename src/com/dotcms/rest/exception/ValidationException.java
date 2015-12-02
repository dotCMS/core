package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.validation.ConstraintViolation;
import com.dotcms.rest.api.Validated;

import java.util.Set;

public class ValidationException extends BadRequestException {

    private static final long serialVersionUID = 1L;
    transient public final Validated builder;
    transient public final Set<ConstraintViolation<Validated>> violations;

    public ValidationException(Validated builder, Set<ConstraintViolation<Validated>> violations) {
        super("One or more failures while validating %s: %s", builder.getClass().getSimpleName(), createMessage(violations));

        this.builder = builder;
        this.violations = violations;
    }

    private static String createMessage(Set<ConstraintViolation<Validated>> violations) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<Validated> violation : violations) {
            sb.append("\n\t").append(" '").append(violation.getPropertyPath()).append("' ").append(violation.getMessage());
        }

        return sb.toString();
    }
}

