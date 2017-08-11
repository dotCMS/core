package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.db.LocalTransaction;

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

        return LocalTransaction.wrapReturn(delegate::proceed);
    } // invoke.

} // E:O:F:LogTimeMethodInterceptor.