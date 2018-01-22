package com.dotcms.test;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.exception.DotDataException;

@VisibleForTesting
public class TransactionalTester {

    @VisibleForTesting
    @WrapInTransaction
    public void test (final VoidDelegate voidDelegate) throws DotDataException {
        voidDelegate.execute();
    }
}
