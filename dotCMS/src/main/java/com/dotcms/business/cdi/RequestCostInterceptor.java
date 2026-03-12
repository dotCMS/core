package com.dotcms.business.cdi;

import com.dotcms.business.interceptor.RequestCostHandler;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link RequestCost}. Delegates to {@link RequestCostHandler} for the
 * actual logic, keeping the implementation DRY with the ByteBuddy advice.
 *
 * <p>Nesting is safe: cost tracking is additive — nested calls just add their own cost.</p>
 */
@Interceptor
@RequestCost
@Priority(Interceptor.Priority.APPLICATION + 20)
public class RequestCostInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        final Method method = context.getMethod();
        final RequestCost annotation = method.getAnnotation(RequestCost.class);
        if (annotation != null) {
            final Price price = annotation.value();
            RequestCostHandler.incrementCost(price, method, context.getParameters());
        }
        return context.proceed();
    }
}