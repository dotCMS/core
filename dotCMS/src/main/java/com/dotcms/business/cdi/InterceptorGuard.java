package com.dotcms.business.cdi;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local guard that prevents double-processing when both ByteBuddy advice and CDI
 * interceptors are active on the same method invocation.
 *
 * <p>ByteBuddy instruments classes at load-time by modifying bytecode, while CDI interceptors
 * fire at the Weld proxy boundary. For CDI-managed beans, both mechanisms may attempt to
 * process the same annotated method. This guard ensures each annotation's logic executes
 * at most once per method call on the current thread.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>
 *     if (!InterceptorGuard.acquire(CloseDBIfOpened.class)) {
 *         // Already being processed by ByteBuddy or another interceptor
 *         return context.proceed();
 *     }
 *     try {
 *         // Execute interceptor logic
 *     } finally {
 *         InterceptorGuard.release(CloseDBIfOpened.class);
 *     }
 * </pre>
 */
public final class InterceptorGuard {

    private static final ThreadLocal<Map<Class<? extends Annotation>, Integer>> ACTIVE_GUARDS =
            ThreadLocal.withInitial(HashMap::new);

    private InterceptorGuard() {
        // Utility class
    }

    /**
     * Attempts to acquire the guard for the given annotation type. Returns {@code true} if the
     * guard was successfully acquired (i.e., no other processing is active for this annotation
     * on the current thread), or {@code false} if another handler is already processing this
     * annotation.
     *
     * @param annotationType the annotation class to guard
     * @return {@code true} if the guard was acquired, {@code false} if already active
     */
    public static boolean acquire(final Class<? extends Annotation> annotationType) {
        final Map<Class<? extends Annotation>, Integer> guards = ACTIVE_GUARDS.get();
        final int depth = guards.getOrDefault(annotationType, 0);
        if (depth > 0) {
            return false;
        }
        guards.put(annotationType, 1);
        return true;
    }

    /**
     * Releases the guard for the given annotation type on the current thread.
     *
     * @param annotationType the annotation class to release
     */
    public static void release(final Class<? extends Annotation> annotationType) {
        final Map<Class<? extends Annotation>, Integer> guards = ACTIVE_GUARDS.get();
        guards.remove(annotationType);
        if (guards.isEmpty()) {
            ACTIVE_GUARDS.remove();
        }
    }
}
