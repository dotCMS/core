package com.dotcms.rendering.velocity.viewtools;

/**
 * OpenSearch-suite concrete of {@link ContentTypeIdSearchResolutionTestBase} (registered in
 * {@code OpenSearchUpgradeSuite}): runs the #37870 scenario under migration phase 3
 * (PHASE_3_OPENSEARCH_ONLY), so both the write (indexing) and the read ({@code $estool.search()})
 * go through the OpenSearch path against the live OpenSearch 3.x upgrade container.
 *
 * <p>Phase 3 makes {@code PhaseRouter.writeProviders()} return {@code [osImpl]}, so contentlet
 * indexing uses the OpenSearch client (compatible with OS 3.x) rather than the legacy ES client.</p>
 *
 * <p>Run with: {@code ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false
 * -Dopensearch.upgrade.test=true}</p>
 */
public class ContentTypeIdSearchResolutionOSTest extends ContentTypeIdSearchResolutionTestBase {

    @Override
    protected int migrationPhase() {
        return 3; // PHASE_3_OPENSEARCH_ONLY
    }
}
