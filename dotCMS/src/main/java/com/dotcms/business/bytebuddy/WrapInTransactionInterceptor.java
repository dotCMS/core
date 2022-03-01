package com.dotcms.business.bytebuddy;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.util.Logger;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;

public class WrapInTransactionInterceptor {

    @RuntimeType
    // Better to rewrite as an Advice
    public static Object intercept(@SuperCall Callable<?> delegate, @Origin Method method) throws Exception {
        final WrapInTransaction wrapInTransaction = getMethodAnnotation(method, WrapInTransaction.class);
        try {
            return Optional.of(wrapInTransaction.externalize()).orElse(false) ? LocalTransaction.externalizeTransaction(delegate) : LocalTransaction.wrapReturnWithListeners(delegate);
        } catch (Throwable e) {
            Logger.error(WrapInTransactionInterceptor.class, "Error", e);
            throw e;
        }
    }
}
