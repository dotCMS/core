package com.dotcms.content.index.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;

/**
 * CDI-injectable contract for obtaining an {@link OpenSearchClient}.
 *
 * <p>The default production implementation is {@link OSDefaultClientProvider}.
 * Integration tests may activate an {@code @Alternative} implementation
 * (e.g. {@code OSTestClientProvider}) to redirect all client access to a
 * local, security-disabled container without modifying any production code.</p>
 *
 * @author fabrizio
 */
public interface OSClientProvider {

    /**
     * Returns the shared {@link OpenSearchClient} for this deployment.
     */
    OpenSearchClient getClient();
}