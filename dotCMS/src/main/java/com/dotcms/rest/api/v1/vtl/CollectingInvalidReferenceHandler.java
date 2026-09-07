package com.dotcms.rest.api.v1.vtl;

import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.app.event.InvalidReferenceEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.util.introspection.Info;

/**
 * A per-evaluation {@link InvalidReferenceEventHandler} that <em>collects</em> invalid references
 * as warnings instead of failing. dotCMS runs Velocity in non-strict mode
 * ({@code runtime.references.strict = false}), so a typo like {@code $noSuchVar} renders as literal
 * text and {@code $obj.noSuchMethod()} silently yields {@code null}. Attaching this handler to the
 * evaluation {@link Context} (via an {@code EventCartridge}) lets the {@code /api/vtl/dynamic}
 * endpoints report those mistakes back to the caller while still returning the rendered output.
 *
 * <p>All callbacks preserve default behavior (they never substitute a value), so attaching this
 * handler cannot change what the script produces — it only observes.</p>
 */
public class CollectingInvalidReferenceHandler implements InvalidReferenceEventHandler {

    /** Cap so a pathological script can't accumulate an unbounded warning list. */
    static final int MAX_WARNINGS = 50;

    private final List<VelocityWarningView> warnings = new ArrayList<>();

    public List<VelocityWarningView> getWarnings() {
        return warnings;
    }

    private void add(final String type, final String message, final String reference, final Info info) {
        if (warnings.size() >= MAX_WARNINGS) {
            return;
        }
        final Integer line = info != null && info.getLine() > 0 ? info.getLine() : null;
        final Integer column = info != null && info.getColumn() > 0 ? info.getColumn() : null;
        warnings.add(new VelocityWarningView(type, message, reference, line, column));
    }

    @Override
    public Object invalidGetMethod(final Context context, final String reference, final Object object,
                                   final String property, final Info info) {
        // A null base object with no property is a top-level undefined variable ($noSuchVar);
        // a non-null base object means a null/missing property on a real object ($real.missing).
        if (object == null && property == null) {
            add("UNDEFINED_REFERENCE",
                    "Undefined reference '" + reference + "' — renders as literal text in non-strict mode",
                    reference, info);
        } else {
            add("NULL_METHOD_RESULT",
                    "Reference '" + reference + "' resolved to null" + describeProperty(property),
                    reference, info);
        }
        return null; // keep default (non-strict) behavior
    }

    @Override
    public Object invalidMethod(final Context context, final String reference, final Object object,
                                final String method, final Info info) {
        // object == null here means the method was invoked on a null reference; otherwise the method
        // does not exist on the (non-null) object or it returned null.
        final String type = object == null ? "UNDEFINED_REFERENCE" : "INVALID_METHOD";
        add(type,
                "Method '" + safe(method) + "()' on '" + reference + "' " +
                        (object == null ? "was called on a null reference" : "does not exist or returned null"),
                reference, info);
        return null; // keep default behavior
    }

    @Override
    public boolean invalidSetMethod(final Context context, final String leftreference,
                                    final String rightreference, final Info info) {
        add("NULL_SET",
                "#set assigned null to '" + leftreference + "'" +
                        (rightreference != null ? " from '" + rightreference + "'" : ""),
                leftreference, info);
        return false; // keep default behavior (do not log-and-swallow differently)
    }

    private static String describeProperty(final String property) {
        return property != null ? " (property '" + property + "')" : "";
    }

    private static String safe(final String value) {
        return value != null ? value : "?";
    }
}
