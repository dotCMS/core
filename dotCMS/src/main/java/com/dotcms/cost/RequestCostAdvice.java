package com.dotcms.cost;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;

/**
 * Advice class that handles the RequestCost annotation. This class is used by AOP frameworks to intercept method calls
 * and process the RequestCost annotation.
 */
public class RequestCostAdvice {


    @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
    public static void enter(
            final @Advice.Origin Method method,
            final @Advice.AllArguments Object[] args
    ) {
        RequestCost annotation = method.getAnnotation(RequestCost.class);
        if (annotation != null) {
            String callingMethod = method.getDeclaringClass().getSimpleName() + "." + method.getName();
            int cost = annotation.increment();
            Logger.debug(RequestCostAdvice.class,
                    () -> "cost:" + callingMethod + " : "
                            + annotation.increment());
            APILocator.getRequestCostAPI().incrementCost(cost, method, args);
        }

    }


}
