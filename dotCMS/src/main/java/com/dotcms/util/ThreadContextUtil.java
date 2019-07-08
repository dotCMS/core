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
    private static final ThreadContextUtil     INSTANCE    = new ThreadContextUtil();

    public static ThreadContextUtil getInstance () {

        return INSTANCE;
    }
    /**
     * Get the context from the current thread
     * @return {@link ThreadContext}
     */
    public ThreadContext getContext () {

        return contextLocal.get();
    }


    /**
     * Set the current thread context
     */
    public void setContext (final ThreadContext  context) {

        contextLocal.set(context);
    }

    /**
     * Return true if the current thread is config to reindex things in the api calls, otherwise false.
     * @return Boolean
     */
    public boolean isReindex () {

        final ThreadContext context = INSTANCE.getContext();
        return null == context || context.isReindex();
    }

    /**
     * Executes the delegate if the reindex is set to true for the current thread
     * @param delegate
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public void ifReindex (final VoidDelegate delegate) throws DotSecurityException, DotDataException {

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
    public void ifReindex (final VoidDelegate delegate, final boolean includeDependencies) throws DotSecurityException, DotDataException {

        if (isReindex()) {

            delegate.execute();
        } else {

            final ThreadContext threadContext =
                    UtilMethods.get(INSTANCE.getContext(), ()->new ThreadContext());
            threadContext.setIncludeDependencies(includeDependencies);
        }
    }

    /**
     * Wrap a void method into not reindex call
     * @param delegate {@link VoidDelegate}
     * @throws Exception
     */
    public void wrapVoidNoReindex (final VoidDelegate delegate) {

        final ThreadContext threadContext =
                UtilMethods.get(INSTANCE.getContext(), ()->new ThreadContext());
        boolean reindex = threadContext.isReindex();

        try {

            threadContext.setReindex(false);
            this.setContext(threadContext);
            delegate.execute();
        } catch(Exception e) {

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
    public  <T> T wrapReturnNoReindex (final ReturnableDelegate<T> delegate) {

        final ThreadContext threadContext =
                UtilMethods.get(INSTANCE.getContext(), ()->new ThreadContext());
        boolean reindex = threadContext.isReindex();

        try {

            threadContext.setReindex(false);
            this.setContext(threadContext);
            return delegate.execute();
        } catch(Throwable e) {

            throw new DotRuntimeException(e);
        } finally {

            threadContext.setReindex(reindex);
        }
    }
}
