package com.dotcms.cost;

import com.dotmarketing.util.Logger;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;

/**
 * Advice class that handles the RequestCost annotation. This class is used by AOP frameworks to intercept method calls
 * and process the RequestCost annotation.
 */
public class RequestCostAdvice {

  private final RequestCostApi requestCostApi = RequestCostApi.getInstance();

  @Advice.OnMethodEnter(inline = false)
  public static void enter(final @Advice.Origin Method method) {
    RequestCost annotation = method.getAnnotation(RequestCost.class);
    Logger.info(RequestCostAdvice.class, "OnMethodEnter: " + method.getName());
    if (annotation != null) {
      int cost = annotation.increment();
      RequestCostApi.getInstance().incrementCost(cost);

    }

  }


}
