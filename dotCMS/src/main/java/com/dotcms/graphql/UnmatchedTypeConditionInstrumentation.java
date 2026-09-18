package com.dotcms.graphql;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import graphql.execution.FetchedValue;
import com.dotmarketing.util.Logger;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldCompleteParameters;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells a client that a narrowing clause it wrote matched nothing.
 *
 * <p>Without this, {@code ... on BannerImages { campaignName }} is indistinguishable from
 * {@code ... on BannreImages { campaignName }}: both are legal, both quietly contribute nothing,
 * and the response looks the same. One is a correct query over content that happened to be another
 * type; the other is a typo. Only the client can tell them apart, and only if told which clause
 * never applied. See issue #34540.
 *
 * <p>Delivered through the response's {@code extensions} — the standard place in the GraphQL
 * response specification for non-fatal, out-of-band information. A client that ignores
 * {@code extensions} is entirely unaffected, and nothing about the data or the errors changes.
 * Deliberately not a server log: the person who can fix a mistyped clause is the client developer,
 * who never reads the server's log, and one entry per request on the delivery path would flood a
 * shared instance's log with noise nobody can act on.
 *
 * <p>Costs nothing when a query contains no narrowing clauses: the document is walked once at the
 * start of execution, and if it declares no type conditions the instrumentation does no further
 * work.
 *
 * <p>Per-request state lives in {@link State}, obtained through {@link #createState()} — never in a
 * field of this class, which is shared across concurrent requests.
 */
public class UnmatchedTypeConditionInstrumentation extends SimpleInstrumentation {

    static final String EXTENSIONS_KEY = "warnings";

    /**
     * Type conditions the query declared, and the concrete types actually resolved, for one
     * request. A {@link ConcurrentHashMap}-backed set because field resolution may run on several
     * threads within a single execution.
     */
    static class State implements InstrumentationState {

        /** Type name -> the field paths it was written under. */
        private final Map<String, Set<String>> declared = new LinkedHashMap<>();
        private final Set<String> resolved = Collections.newSetFromMap(new ConcurrentHashMap<>());

        void declare(final String typeName, final String path) {
            declared.computeIfAbsent(typeName, key -> new LinkedHashSet<>()).add(path);
        }

        void resolvedAs(final String typeName) {
            resolved.add(typeName);
        }

        boolean isEmpty() {
            return declared.isEmpty();
        }

        /** @return one warning per declared condition that no resolved type ever satisfied. */
        List<Map<String, Object>> unmatched() {
            final List<Map<String, Object>> warnings = new ArrayList<>();
            declared.forEach((typeName, paths) -> {
                if (resolved.contains(typeName)) {
                    return;
                }
                paths.forEach(path -> warnings.add(Map.of(
                        "path", path,
                        "typeCondition", typeName,
                        "message", "No content at this path was of type " + typeName + ".")));
            });
            return warnings;
        }
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(
            final InstrumentationExecuteOperationParameters parameters) {

        // Walked here rather than at beginExecution: the document only becomes reachable once the
        // operation to execute has been chosen, and walking the chosen operation — rather than the
        // whole document — means a warning is never attributed to a query that did not run.
        final State state = parameters.getInstrumentationState();
        if (null != state && null != parameters.getExecutionContext()) {
            collectTypeConditions(parameters.getExecutionContext().getOperationDefinition(),
                    "", state);
        }
        return super.beginExecuteOperation(parameters);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginFieldComplete(
            final InstrumentationFieldCompleteParameters parameters) {

        final State state = parameters.getInstrumentationState();
        if (null != state && !state.isEmpty()) {
            // Read from the fetched VALUE rather than from the schema's type bookkeeping. Asking
            // graphql-java for the step's type gives the type OF the field (`String` for
            // `campaignName`) and asking for its object type gives the declared parent, which for
            // an interface-typed position is the interface -- neither is the concrete type the
            // clause was testing for. The contentlet knows what it actually is, and "did this
            // clause match" is a question about the data, not about the schema.
            recordResolvedType(parameters.getFetchedValue(), state);
        }
        return super.beginFieldComplete(parameters);
    }

    private void recordResolvedType(final Object value, final State state) {
        // graphql-java hands the value wrapped, so the unwrap is not optional: without it nothing
        // is ever recognised as content and every clause is reported as unmatched.
        if (value instanceof FetchedValue) {
            recordResolvedType(((FetchedValue) value).getFetchedValue(), state);
            return;
        }
        if (value instanceof Contentlet) {
            state.resolvedAs(((Contentlet) value).getContentType().variable());
            return;
        }
        if (value instanceof Iterable) {
            ((Iterable<?>) value).forEach(element -> recordResolvedType(element, state));
        }
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(
            final ExecutionResult executionResult,
            final InstrumentationExecutionParameters parameters) {

        final State state = parameters.getInstrumentationState();
        if (null == state || state.isEmpty()) {
            return super.instrumentExecutionResult(executionResult, parameters);
        }

        final List<Map<String, Object>> warnings = state.unmatched();
        if (warnings.isEmpty()) {
            return super.instrumentExecutionResult(executionResult, parameters);
        }

        Logger.debug(this, () -> "Narrowing clauses that matched nothing: " + warnings);

        final Map<Object, Object> extensions = new LinkedHashMap<>();
        if (null != executionResult.getExtensions()) {
            extensions.putAll(executionResult.getExtensions());
        }
        extensions.put(EXTENSIONS_KEY, warnings);

        return CompletableFuture.completedFuture(
                ExecutionResultImpl.newExecutionResult().from(executionResult)
                        .extensions(extensions).build());
    }

    /**
     * Walks the query for inline-fragment type conditions, recording the field path each was
     * written under.
     *
     * <p>Named fragment definitions are not followed: a spread reaches them through
     * {@code FragmentSpread}, which carries no type condition of its own, so following them would
     * attribute a warning to a path the client did not write. The inline form is what the feature's
     * documented usage recommends.
     */
    private void collectTypeConditions(final Node<?> node, final String path, final State state) {
        if (null == node) {
            return;
        }

        for (final Node<?> child : node.getChildren()) {
            if (child instanceof InlineFragment) {
                final InlineFragment fragment = (InlineFragment) child;
                if (null != fragment.getTypeCondition()) {
                    state.declare(fragment.getTypeCondition().getName(),
                            path.isEmpty() ? "(root)" : path);
                }
                collectTypeConditions(child, path, state);
                continue;
            }

            if (child instanceof Field) {
                final Field field = (Field) child;
                final String childPath =
                        path.isEmpty() ? field.getName() : path + "." + field.getName();
                collectTypeConditions(child, childPath, state);
                continue;
            }

            collectTypeConditions(child, path, state);
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof UnmatchedTypeConditionInstrumentation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(UnmatchedTypeConditionInstrumentation.class);
    }
}
