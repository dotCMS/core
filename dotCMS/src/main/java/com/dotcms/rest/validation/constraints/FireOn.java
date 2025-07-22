package com.dotcms.rest.validation.constraints;

import javax.validation.Constraint;
import javax.validation.Payload;
import com.dotcms.rest.validation.FireOnValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {FireOnValidator.class}
)
public @interface FireOn {
    String message() default "{javax.validation.constraints.FireOn.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
