package com.dotcms.rest.api;

import java.util.function.Function;

/**
 * @author Geoff M. Granum
 */
public interface RestTransform<A, R> {
    A applyRestToApp(R rest, A app);

    R appToRest(A app);

    Function<A, R> appToRestFn();
}
