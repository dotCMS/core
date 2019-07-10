package com.dotcms.util;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;

/**
 * Thread Context Util provides methods to handle reindex stuff and other thread local context things
 * @author jsancas
 */
public class ThreadContextUtil {

    private static ThreadLocal<ThreadContext> contextLocal = new ThreadLocal<>();

    /**
     * Get the context from the current thread
     * @return {@link ThreadContext}
     */
    public static ThreadContext getOrCreateContext() {

        return UtilMethods.get(contextLocal.get(), ()-> {

            final ThreadContext context = new ThreadContext();
            contextLocal.set(context);
            return context;
        });
    }


    /**
     * Return true if the current thread is config to reindex things in the api calls, otherwise false.
     * @return Boolean
     */
    public static boolean isReindex () {

        final ThreadContext context = getOrCreateContext();
        return context.isReindex();
    }

    /**
     * Executes the delegate if the reindex is set to true for the current thread
     * @param delegate
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public static void ifReindex (final VoidDelegate delegate) throws DotSecurityException, DotDataException {

        if (isReindex()) {

            delegate.execute();
        }
    }

    /**
     * Executes the delegate if the reindex is set to true for the current thread
     * @param delegate
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public static void ifReindex (final VoidDelegate delegate, final boolean includeDependencies) throws DotSecurityException, DotDataException {

        if (isReindex()) {

            delegate.execute();
        } else {

            getOrCreateContext().setIncludeDependencies(includeDependencies);
        }
    }

    /**
     * Wrap a void method into not reindex call
     * @param delegate {@link VoidDelegate}
     * @throws Exception
     */
    public static void wrapVoidNoReindex (final VoidDelegate delegate) {

        final ThreadContext threadContext = getOrCreateContext();
        final boolean reindex = threadContext.isReindex();

        try {

            threadContext.setReindex(false);
            delegate.execute();
        } catch(Throwable e) {

            throw new DotRuntimeException(e);
        } finally {

            threadContext.setReindex(reindex);
        }
    }


    /**
     * Wrap a return method into not reindex call
     * @param delegate {@link VoidDelegate}
     * @throws Exception
     */
    public  static <T> T wrapReturnNoReindex (final ReturnableDelegate<T> delegate) {

        final ThreadContext threadContext = getOrCreateContext();
        final boolean reindex = threadContext.isReindex();

        try {

            threadContext.setReindex(false);
            return delegate.execute();
        } catch(Throwable e) {

            throw new DotRuntimeException(e);
        } finally {

            threadContext.setReindex(reindex);
        }
    }
}
