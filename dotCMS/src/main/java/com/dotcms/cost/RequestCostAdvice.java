package com.dotcms.cost;

import com.dotcms.cost.RequestPrices.Price;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;

/**
 * Advice class that handles the RequestCost annotation. This class is used by AOP frameworks to intercept method calls
 * and process the RequestCost annotation.
 */
public class RequestCostAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class, inline = true)
    public static void enter(
            final @Advice.Origin Method method,
            final @Advice.AllArguments Object[] args
    ) {
        try {
            RequestCost annotation = method.getAnnotation(RequestCost.class);
            if (annotation != null) {
                RequestCostApi api = APILocator.getRequestCostAPI();

                Price price = annotation.value();

                api.incrementCost(price, method, args);
            }
        } catch (Throwable t) {
            // Log any exceptions that occur - even though suppress=Throwable.class prevents them from propagating
            Logger.warnAndDebug(RequestCostAdvice.class, "Error in RequestCostAdvice.enter(): " + t.getMessage(), t);
        }

    }
}
