package com.dotcms.test;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.exception.DotDataException;

@VisibleForTesting
public class ReadOnlyTester {

    @VisibleForTesting
    @CloseDBIfOpened
    public void test (final VoidDelegate voidDelegate) throws DotDataException {
        voidDelegate.execute();
    }
}
