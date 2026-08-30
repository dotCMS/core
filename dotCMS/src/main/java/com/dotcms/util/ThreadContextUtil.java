package com.dotcms.util;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;

/**
 * Thread Context Util provides methods to handle reindex stuff and other thread local context things
 *
 * <p><b>Two mechanisms, on purpose.</b> The two pieces of state here flow in opposite directions,
 * so they are held differently:</p>
 *
 * <ul>
 *   <li>{@code reindex} travels <b>caller to callee</b>: an API call declares "do not index inline
 *       while I run" and everything underneath reads it. That is a dynamically scoped binding, and
 *       it is held in a {@link ScopedValue} — bounded by the call, immutable, and restored on exit
 *       whether the delegate returns or throws.</li>
 *   <li>{@code includeDependencies} travels <b>callee to caller</b>: code deep in the stack records
 *       that the eventual deferred reindex must include dependencies, and code higher up reads it
 *       afterwards to decide how to index. A scoped value cannot express that — its bindings are
 *       immutable by design — so this one stays in the mutable {@link ThreadContext} held by a
 *       thread local. See {@code WorkflowAPIImpl.fireWorkflowPostCheckin}, which consumes what
 *       {@code ESContentletAPIImpl} recorded.</li>
 * </ul>
 *
 * <p>Reaching for a scoped value for the second one would be the classic mistake: the mechanism is
 * a better thread local only for context that genuinely descends.</p>
 *
 * @author jsancas
 */
public class ThreadContextUtil {

    /**
     * Whether API calls on this thread should index inline. Unbound means yes, which is the default
     * the platform relies on.
     */
    private static final ScopedValue<Boolean> REINDEX = ScopedValue.newInstance();

    private static ThreadLocal<ThreadContext> contextLocal = new ThreadLocal<>();

    /**
     * Get the context from the current thread
     *
     * <p>Only {@code includeDependencies} still lives here; the reindex flag is a
     * {@link ScopedValue}. Note that this method <i>installs</i> a context on first use, so callers
     * that merely want to read the reindex flag should use {@link #isReindex()} instead of coming
     * through here.</p>
     *
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

        return REINDEX.orElse(Boolean.TRUE);
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

        wrapReturnNoReindex(() -> {

            delegate.execute();
            return null;
        });
    }


    /**
     * Wrap a return method into not reindex call
     * @param delegate {@link VoidDelegate}
     * @throws Exception
     */
    public  static <T> T wrapReturnNoReindex (final ReturnableDelegate<T> delegate) {

        try {

            return ScopedValue.where(REINDEX, Boolean.FALSE).call(delegate::execute);
        } catch(Throwable e) {

            throw new DotRuntimeException(e);
        }
    }
}
