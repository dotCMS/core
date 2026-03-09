package com.dotcms.content.index.opensearch;

import com.dotmarketing.util.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import org.opensearch.client.opensearch.OpenSearchClient;

/**
 * CDI {@code @Alternative} that replaces {@link OSDefaultClientProvider} during integration tests.
 *
 * <p>Because this class carries {@link Priority @Priority(1)} it is activated globally by Weld
 * for every CDI archive that contains it — i.e. the integration-test classpath — without any
 * {@code beans.xml} entry.  All beans that inject {@link OSClientProvider}
 * ({@code OSIndexAPIImpl}, {@code OSBulkHelper}, {@code ContentFactoryIndexOperationsOS}, …)
 * automatically receive this provider.</p>
 *
 * <p>The target endpoint defaults to {@code http://localhost:9201} (the standard
 * {@code opensearch-upgrade} Docker container) and can be overridden at the command line:</p>
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true \
 *       -Dopensearch.test.endpoint=http://myhost:9201
 * </pre>
 *
 * @author fabrizio
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class OSTestClientProvider implements OSClientProvider {

    private final ConfigurableOpenSearchProvider provider;

    public OSTestClientProvider() {
        final String endpoint =
                System.getProperty("opensearch.test.endpoint", "http://localhost:9201");
        Logger.info(this.getClass(),
                "OSTestClientProvider: using test OpenSearch endpoint → " + endpoint);
        provider = new ConfigurableOpenSearchProvider(
                OSClientConfig.builder()
                        .addEndpoints(endpoint)
                        .tlsEnabled(false)
                        .build()
        );
    }

    @Override
    public OpenSearchClient getClient() {
        return provider.getClient();
    }
}