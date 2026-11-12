package com.dotmarketing.business;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks that cache flushes on one node are sent to the other cluster nodes when the configured
 * providers are local (not distributed), which is the default Caffeine chain.
 */
public class ChainableCacheAdministratorImplTest {

    private static final String GROUP = "issue38528group";

    private ChainableCacheAdministratorImpl cacheAdmin;
    private CacheTransport transport;

    /**
     * Builds the administrator over a real {@link CacheProviderAPIImpl} (default chain, Caffeine
     * only) and a mocked transport, so the test sees exactly what would go to the cluster.
     */
    @Before
    public void setUp() throws Exception {
        transport = mock(CacheTransport.class);
        final CacheTransportStrategy strategy = mock(CacheTransportStrategy.class);
        when(strategy.get()).thenReturn(transport);

        cacheAdmin = new ChainableCacheAdministratorImpl(strategy);
        final Field apiField = ChainableCacheAdministratorImpl.class.getDeclaredField("cacheProviderAPI");
        apiField.setAccessible(true);
        apiField.set(cacheAdmin, new CacheProviderAPIImpl());
    }

    /**
     * Flush All must broadcast {@code 0:root} when the providers in use are local. Before the fix,
     * the local flush emptied the provider map first and the empty map was read as "distributed".
     */
    @Test
    public void flushAll_withLocalProviders_sendsRootInvalidation() throws Exception {
        cacheAdmin.put("key", "value", GROUP);

        cacheAdmin.flushAll();

        verify(transport).send("0:" + DotCacheAdministrator.ROOT_GOUP);
    }

    /**
     * A second Flush All runs while the provider map is still empty from the first. Not knowing any
     * provider must not count as "everything is distributed", so it broadcasts as well.
     */
    @Test
    public void flushAll_withNoKnownProviders_stillSendsRootInvalidation() throws Exception {
        cacheAdmin.put("key", "value", GROUP);
        cacheAdmin.flushAll();

        cacheAdmin.flushAll();

        verify(transport, times(2)).send("0:" + DotCacheAdministrator.ROOT_GOUP);
    }

    /** A region flush over local providers keeps broadcasting {@code 0:<group>}. */
    @Test
    public void flushGroup_withLocalProviders_sendsGroupInvalidation() throws Exception {
        cacheAdmin.put("key", "value", GROUP);

        cacheAdmin.flushGroup(GROUP);

        verify(transport).send("0:" + GROUP);
    }
}
