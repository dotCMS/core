package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.db.LocalTransaction;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;

/**
 * Method handler for the {@link WrapInTransaction} annotation aspect
 * @author jsanca
 */
public class WrapInTransactionMethodInterceptor implements MethodInterceptor<Object> {

    public static final WrapInTransactionMethodInterceptor INSTANCE = new WrapInTransactionMethodInterceptor();

    protected WrapInTransactionMethodInterceptor() {

    }

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        final WrapInTransaction wrapInTransaction =
                getMethodAnnotation(delegate.getMethod(), WrapInTransaction.class);

        return wrapInTransaction.externalize()?
                LocalTransaction.externalizeTransaction(delegate::proceed):
                LocalTransaction.wrapReturnWithListeners(delegate::proceed);
    } // invoke.

} // E:O:F:LogTimeMethodInterceptor.
