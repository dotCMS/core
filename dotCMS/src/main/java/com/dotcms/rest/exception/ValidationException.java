package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.validation.ConstraintViolation;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.api.Validated;

import java.util.Set;
import java.util.stream.Collectors;
public class ValidationException extends BadRequestException {

    private static final long serialVersionUID = 1L;
    transient public final Validated builder;
    transient public final Set<ConstraintViolation<Validated>> violations;

    public ValidationException(final Validated builder, final Set<ConstraintViolation<Validated>> violations) {
        super(null, violations.stream()
                .map(constraint -> new ErrorEntity(null, constraint.getMessage(), constraint.getPropertyPath().toString())).collect(Collectors.toList()),
                createMessage(violations));

        this.builder = builder;
        this.violations = violations;
    }

    private static String createMessage(final Set<ConstraintViolation<Validated>> violations) {

        final StringBuilder sb = new StringBuilder();
        for (final ConstraintViolation<Validated> violation : violations) {
            sb.append("\n\t").append(" '").append(violation.getPropertyPath()).append("' ").append(violation.getMessage());
        }

        return sb.toString();
    }
}

