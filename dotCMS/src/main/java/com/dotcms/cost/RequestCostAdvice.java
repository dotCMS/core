package com.dotcms.cost;

import com.dotcms.business.interceptor.RequestCostHandler;
import com.dotcms.cost.RequestPrices.Price;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;

/**
 * ByteBuddy advice for {@link RequestCost}. Delegates to {@link RequestCostHandler} for the
 * actual logic, keeping the implementation DRY with the CDI interceptor.
 */
public class RequestCostAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class, inline = true)
    public static void enter(
            final @Advice.Origin Method method,
            final @Advice.AllArguments Object[] args
    ) {
        final RequestCost annotation = method.getAnnotation(RequestCost.class);
        if (annotation != null) {
            final Price price = annotation.value();
            RequestCostHandler.incrementCost(price, method, args);
        }
    }
}
