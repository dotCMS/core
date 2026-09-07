package com.dotcms.content.index;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Why the ES → OpenSearch migration last switched itself off.
 *
 * <p>When OpenSearch rejects an index operation while it is only the shadow store, the failure is
 * absorbed and the migration is halted so that dotCMS keeps serving from Elasticsearch
 * (issue #36222). That policy is right, but on its own it is silent: the reason lives in the log and
 * nothing reaches the operator who pressed the button. This carrier keeps the classified cause and
 * its remediation so callers outside the index layer — the reindex REST resource, which carries no
 * vendor imports — can report <em>why</em> the migration stopped, not merely <em>that</em> it did.</p>
 *
 * <p><strong>Scope:</strong> the last report is held per JVM, deliberately matching the scope of the
 * halt it describes: {@link IndexConfigHelper.MigrationPhase#reset()} is a runtime-only change, so a
 * restart clears both the halted phase and the reason for it. It is not persisted and not
 * cluster-wide — each node reports the halt it absorbed itself.</p>
 *
 * @param haltedPhase the phase that was active when the failure was absorbed
 * @param cause       classified failure kind, e.g. {@code AUTH_FORBIDDEN}
 * @param remediation operator-facing description of how to fix that kind of failure
 * @param indexNames  the physical OpenSearch index names the operation was rejected for
 */
public record MigrationHaltReport(String haltedPhase, String cause, String remediation,
                                  String indexNames) {

    private static final AtomicReference<MigrationHaltReport> LAST = new AtomicReference<>();

    /**
     * Records {@code report} as the most recent halt, replacing any previous one.
     *
     * @param report the halt to remember
     */
    public static void record(final MigrationHaltReport report) {
        LAST.set(report);
    }

    /**
     * Returns the most recent halt absorbed by this JVM, or empty when the migration has not been
     * halted since start-up.
     *
     * @return the last halt report, if any
     */
    public static Optional<MigrationHaltReport> last() {
        return Optional.ofNullable(LAST.get());
    }

    /**
     * Returns a single sentence an operator can act on: what happened, that Elasticsearch is still
     * serving, why it happened, and what to do about it.
     *
     * @return the operator-facing message
     */
    public String operatorMessage() {
        return "The OpenSearch migration was switched off (was " + haltedPhase + ") and dotCMS is"
                + " serving from Elasticsearch only. Cause: " + cause + " (" + remediation + ")."
                + " Rejected index names: " + indexNames + "."
                + " Fix the cause and re-enable the migration phase when ready.";
    }
}
