package com.dotcms.publisher.util.dependencies;

import static org.junit.Assert.assertEquals;

import com.dotcms.UnitTestBase;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Unit tests for {@link ConcurrentDependencyProcessor}.
 */
public class ConcurrentDependencyProcessorTest extends UnitTestBase {

    private static Contentlet contentlet(final String identifier) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setIdentifier(identifier);
        return contentlet;
    }

    /**
     * <b>Method to test:</b> {@link ConcurrentDependencyProcessor#addAsset(Object, PusheableAsset)} <p>
     * <b>Given Scenario:</b> The same asset is added twice. <p>
     * <b>ExpectedResult:</b> It is processed once.
     */
    @Test
    public void addAsset_sameAssetTwice_isProcessedOnce() throws ExecutionException {
        final ConcurrentDependencyProcessor processor = new ConcurrentDependencyProcessor();
        final AtomicInteger runs = new AtomicInteger();
        processor.addProcessor(PusheableAsset.CONTENTLET, asset -> runs.incrementAndGet());

        processor.addAsset(contentlet("a"), PusheableAsset.CONTENTLET);
        processor.addAsset(contentlet("a"), PusheableAsset.CONTENTLET);
        processor.waitUntilResolveAllDependencies();

        assertEquals(1, runs.get());
    }

    /**
     * <b>Method to test:</b> {@link ConcurrentDependencyProcessor#reprocessAsset(Object, PusheableAsset)} <p>
     * <b>Given Scenario:</b> While an asset is being processed, it is queued again with
     * reprocessAsset, as the Push Publishing processor does when a Contentlet's remaining
     * relationship depth grows. A second asset is queued normally. <p>
     * <b>ExpectedResult:</b> The first asset is processed twice, the second once, and waiting for
     * the dependencies finishes promptly instead of waiting for the idle timeout (issue #38394).
     */
    @Test(timeout = 30_000)
    public void reprocessAsset_alreadyProcessedAsset_isProcessedAgainAndFinishes()
            throws ExecutionException {
        final ConcurrentDependencyProcessor processor = new ConcurrentDependencyProcessor();
        final Map<String, AtomicInteger> runs = new ConcurrentHashMap<>();
        processor.addProcessor(PusheableAsset.CONTENTLET, asset -> {
            final String id = ((Contentlet) asset).getIdentifier();
            final int run = runs.computeIfAbsent(id, key -> new AtomicInteger()).incrementAndGet();
            if ("a".equals(id) && run == 1) {
                processor.reprocessAsset(asset, PusheableAsset.CONTENTLET);
                processor.addAsset(contentlet("b"), PusheableAsset.CONTENTLET);
            }
        });

        processor.addAsset(contentlet("a"), PusheableAsset.CONTENTLET);
        processor.waitUntilResolveAllDependencies();

        assertEquals(2, runs.get("a").get());
        assertEquals(1, runs.get("b").get());
    }

    /**
     * <b>Method to test:</b> {@link ConcurrentDependencyProcessor#reprocessAsset(Object, PusheableAsset)} <p>
     * <b>Given Scenario:</b> reprocessAsset is called for an asset that was never queued. <p>
     * <b>ExpectedResult:</b> It is processed once, like addAsset would do.
     */
    @Test(timeout = 30_000)
    public void reprocessAsset_newAsset_isProcessedOnce() throws ExecutionException {
        final ConcurrentDependencyProcessor processor = new ConcurrentDependencyProcessor();
        final AtomicInteger runs = new AtomicInteger();
        processor.addProcessor(PusheableAsset.CONTENTLET, asset -> runs.incrementAndGet());

        processor.reprocessAsset(contentlet("a"), PusheableAsset.CONTENTLET);
        processor.waitUntilResolveAllDependencies();

        assertEquals(1, runs.get());
    }
}
