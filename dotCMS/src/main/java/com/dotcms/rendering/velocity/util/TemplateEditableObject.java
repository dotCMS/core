package com.dotcms.rendering.velocity.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Names the one object that template code running on this thread may change. Content, site, container, template,
 * folder and identifier objects are read-only in templates (see
 * {@link org.apache.velocity.util.introspection.SecureUberspector}); a workflow action is the exception, because its
 * Velocity is written to change the content the action is processing.
 *
 * <p>Scopes nest: opening one hides the one before it until it is closed, so an action that fires another workflow
 * gets the outer content back afterwards. Open a scope with try-with-resources so it is closed on every path and never
 * outlives the call on a pooled thread.
 */
public final class TemplateEditableObject implements AutoCloseable {

    private static final ThreadLocal<Deque<Supplier<?>>> SCOPES = new ThreadLocal<>();

    private boolean closed;

    private TemplateEditableObject() {
    }

    /**
     * Opens a scope in which template code on this thread may change the object the supplier returns. The supplier is
     * asked on each check, so the scope follows the object when its holder replaces it.
     *
     * @param editable returns the object template code may change; may return {@code null}
     * @return the scope, to close when the call that runs the template code ends
     */
    public static TemplateEditableObject open(final Supplier<?> editable) {
        Deque<Supplier<?>> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        scopes.push(editable);
        return new TemplateEditableObject();
    }

    /**
     * Tells whether template code on this thread may change the object: it must be the very object the innermost open
     * scope names.
     *
     * @param object the object a template is about to change
     * @return {@code true} if the innermost scope names this instance
     */
    public static boolean isEditable(final Object object) {
        final Deque<Supplier<?>> scopes = SCOPES.get();
        return object != null && scopes != null && !scopes.isEmpty() && scopes.peek().get() == object;
    }

    /**
     * Closes this scope, which makes the scope opened before it the innermost again.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        final Deque<Supplier<?>> scopes = SCOPES.get();
        if (scopes != null) {
            scopes.pop();
            if (scopes.isEmpty()) {
                SCOPES.remove();
            }
        }
    }
}
