package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotmarketing.db.LocalTransaction;

/**
 * Method handler for the {@link com.dotcms.business.Transaction} annotation aspect
 * @author jsanca
 */
public class TransactionMethodInterceptor implements MethodInterceptor<Object> {

    public static final TransactionMethodInterceptor INSTANCE = new TransactionMethodInterceptor();

    protected TransactionMethodInterceptor() {

    }

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        return LocalTransaction.transaction(delegate::proceed);
    } // invoke.

} // E:O:F:LogTimeMethodInterceptor.
