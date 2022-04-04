package com.dotcms.business.bytebuddy;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import net.bytebuddy.asm.Advice;

public class CloseDBAdvice {

    @Advice.OnMethodExit(inline = false, onThrowable = DotDataException.class)
    public static void exit(
            @Advice.Thrown Throwable t
            ) throws Throwable {

        try {
            DbConnectionFactory.closeAndCommit();
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }
}
