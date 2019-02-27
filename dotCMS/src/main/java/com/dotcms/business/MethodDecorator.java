package com.dotcms.business;

import java.lang.annotation.*;

/**
 * Method annotated with this annotation can set a parameter decorator to do something over the
 * parameters before invoke, such as interceptor.
 *
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodDecorator {

    /**
     * Decorator for the parameter
     * @return Class
     */
    Class<? extends ParameterDecorator> parameterDecorator () default ParameterDecorator.class;
}
