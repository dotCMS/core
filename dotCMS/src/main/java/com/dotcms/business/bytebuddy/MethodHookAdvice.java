package com.dotcms.business.bytebuddy;

import com.dotmarketing.db.DbConnectionFactory;
import net.bytebuddy.asm.Advice;

public class MethodHookAdvice {

    @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class)
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
