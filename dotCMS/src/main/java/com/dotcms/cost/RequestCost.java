package com.dotcms.cost;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically track the cost of a method call. The cost will be added to the current request's cost
 * tracked by {@link RequestCostApiImpl}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface RequestCost {

  /**
   * The cost to add to the request when this method is executed.
   *
   * @return cost value to increment
   */
  int increment() default 1;

}
