package com.dotcms.business.bytebuddy;

import com.dotcms.business.CloseDB;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.util.LogTime;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.Throw;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;
/**
 * This Advice handles the @{@link CloseDBIfOpened} with AspectJ
 * @author spbolton
 */
public class CloseDBIfOpenedAdvice {


    @Advice.OnMethodEnter(inline = false)
    public static boolean enter(final @Advice.Origin Method method) {
        return !DbConnectionFactory.connectionExists();
    }


    @Advice.OnMethodExit(inline = false, onThrowable = DotDataException.class)
    public static void exit(final @Advice.Origin Method method, @Advice.Enter boolean isNewConnection
            ) {


        final CloseDBIfOpened closeDB =
                getMethodAnnotation(method, CloseDBIfOpened.class);

        if (null != closeDB && closeDB.connection()
                && isNewConnection) {

            DbConnectionFactory.closeSilently();
        }
    }
}
