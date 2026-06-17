package com.dotcms.rendering.velocity.viewtools;

/**
 * ES-suite concrete of {@link ContentTypeIdSearchResolutionTestBase} (registered in
 * {@code MainSuite1b}): runs the #37870 scenario under migration phase 1
 * (PHASE_1_DUAL_WRITE_ES_READS) — an in-progress migration whose reads are still served by
 * Elasticsearch. MainSuite1b provisions only the ES backend, so the OS half of the Phase-1
 * dual-write is a fire-and-forget no-op here and the read is served by ES.
 */
public class ContentTypeIdSearchResolutionESTest extends ContentTypeIdSearchResolutionTestBase {

    @Override
    protected int migrationPhase() {
        return 1; // PHASE_1_DUAL_WRITE_ES_READS
    }
}
