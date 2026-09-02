package com.dotcms.cost;

import com.dotcms.cost.RequestPrices.Price;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * Annotation to automatically track the cost of a method call. The cost will be added to the current request's cost
 * tracked by {@link RequestCostApiImpl}.
 * <p>This annotation serves as both a ByteBuddy advice marker and a CDI interceptor binding.
 * ByteBuddy instruments non-CDI classes at load-time, while the CDI interceptor fires at the
 * proxy boundary for managed beans.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
@InterceptorBinding
public @interface RequestCost {

  /**
   * The cost to add to the request when this method is executed.
   *
   * @return cost value to increment
   */
  @Nonbinding
  Price value() default Price.COSTING_INIT;

}
