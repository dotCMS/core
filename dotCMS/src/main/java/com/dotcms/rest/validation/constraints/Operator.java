package com.dotcms.rest.validation.constraints;

import com.dotcms.repackage.javax.validation.Constraint;
import com.dotcms.repackage.javax.validation.Payload;
import com.dotcms.rest.validation.FireOnValidator;
import com.dotcms.rest.validation.OperatorValidator;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {OperatorValidator.class}
)
public @interface Operator {
    String message() default "{javax.validation.constraints.Operator.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
