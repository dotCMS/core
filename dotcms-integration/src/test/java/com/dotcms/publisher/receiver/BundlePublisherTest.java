package com.dotcms.publisher.receiver;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishEndOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishFailureOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishStartOnReceiverEvent;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishSuccessOnReceiverEvent;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.ConfigUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BundlePublisherTest extends IntegrationTestBase {

    public BundlePublisherTest() {
    }

    @BeforeClass
    public static void prepare() throws Exception{

        IntegrationTestInitService.getInstance().init();
        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        localSystemEventsAPI.subscribe(PushPublishStartOnReceiverEvent.class, (EventSubscriber<PushPublishStartOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishStartOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

        localSystemEventsAPI.subscribe(PushPublishFailureOnReceiverEvent.class, (EventSubscriber<PushPublishFailureOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishFailureOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

        localSystemEventsAPI.subscribe(PushPublishSuccessOnReceiverEvent.class, (EventSubscriber<PushPublishSuccessOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishSuccessOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

        localSystemEventsAPI.subscribe(PushPublishEndOnReceiverEvent.class, (EventSubscriber<PushPublishEndOnReceiverEvent>) event -> {
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getName());
            Assert.assertEquals(PushPublishEndOnReceiverEvent.class.getCanonicalName(), event.getName());
            Assert.assertNotNull(event.getPublishQueueElements());
            Assert.assertEquals(1, event.getPublishQueueElements().size());
        });

    }

    @Test
    public void trigger_events()  {

        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        final List<PublishQueueElement> publishQueueElements = new ArrayList<>();
        PublishQueueElement publishQueueElement = new PublishQueueElement();
        publishQueueElements.add(publishQueueElement);
        final PublisherConfig publisherConfig = new PublisherConfig();
        publisherConfig.setAssets(publishQueueElements);
        localSystemEventsAPI.notify(new PushPublishStartOnReceiverEvent(publishQueueElements));
        localSystemEventsAPI.notify(new PushPublishFailureOnReceiverEvent(publishQueueElements));
        localSystemEventsAPI.notify(new PushPublishSuccessOnReceiverEvent(publisherConfig));
        localSystemEventsAPI.notify(new PushPublishEndOnReceiverEvent(publishQueueElements));
    }

    /**
     * Method to Test: {@link BundlePublisher#process(PublishStatus)}
     * When: an entry of the bundle resolves to a sibling directory whose name starts with the name
     * of the bundles directory
     * Should: not extract the entry.
     */
    @Test
    public void test_untar_doesNotExtractEntryOutsideBundlesDirectory() throws Exception {
        LicenseTestUtil.getLicense();
        final String bundleId = "untar_" + (System.currentTimeMillis() % 1_000_000_000L);
        final String bundleName = bundleId + ".tar.gz";

        final File bundlesDir = new File(ConfigUtils.getBundlePath());
        final File sibling = new File(bundlesDir.getParentFile(), bundlesDir.getName() + "_s");
        assertTrue(sibling.mkdirs() || sibling.isDirectory());
        final File entryTarget = new File(sibling, "entry.txt");

        final PublishAuditAPI realAuditAPI = PublishAuditAPI.getInstance();
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleId);
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("endpoint1", "endpoint1", null);
        publishAuditStatus.setStatusPojo(history);
        realAuditAPI.insertPublishAuditStatus(publishAuditStatus);

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setId(bundleName);
        config.setPublishAuditStatus(publishAuditStatus);
        config.setAssets(new ArrayList<>());

        final File bundleFile = new File(bundlesDir, bundleName);
        FileUtils.writeByteArrayToFile(bundleFile,
                tarGzWithEntry("../../" + sibling.getName() + "/entry.txt"));

        final BundlePublisher bundlePublisher = new BundlePublisher();
        bundlePublisher.init(config);

        try {
            bundlePublisher.process(new PublishStatus());
        } catch (DotPublishingException e) {
            // the bundle is expected to fail; what matters is what was written
        } finally {
            final boolean extracted = entryTarget.exists();
            FileUtils.deleteQuietly(bundleFile);
            FileUtils.deleteQuietly(new File(bundlesDir, bundleId));
            FileUtils.deleteQuietly(sibling);
            try {
                realAuditAPI.deletePublishAuditStatus(bundleId);
            } catch (Exception ex) {
                // Ignore cleanup errors
            }
            assertFalse("an entry must not be extracted outside the bundles directory", extracted);
        }
    }

    /**
     * Method to Test: {@link BundlePublisher#process(PublishStatus)}
     * When: the bundle name carries extra dots right before the {@code .tar.gz} extension, so the
     * part before the extension is {@code .} or {@code ..}
     * Should: throw a DotPublishingException before updating the audit table or touching the
     * bundles directory, instead of resolving a working folder outside of it.
     *
     * <p>Deliberately NOT run against the pre-fix code: a pre-fix run would resolve the working
     * folder to the bundles directory itself (for {@code .}) or its parent (for {@code ..}), and
     * delete it. The derivation that makes this possible was verified separately, without
     * touching the filesystem, by {@link com.dotcms.util.PathContainmentUtilTest}.
     */
    @Test
    public void test_process_rejectsABundleNameWhoseIdIsADotOrDotDot() throws Exception {
        LicenseTestUtil.getLicense();
        final PublishAuditAPI realAuditAPI = PublishAuditAPI.getInstance();

        for (final String bundleName : new String[] {"..tar.gz", "...tar.gz"}) {
            final String bundleId = bundleName.substring(0, bundleName.length() - ".tar.gz".length());

            final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleId);
            final PublishAuditHistory history = new PublishAuditHistory();
            history.addOrUpdateEndpoint("endpoint1", "endpoint1", null);
            publishAuditStatus.setStatusPojo(history);

            final PushPublisherConfig config = new PushPublisherConfig();
            config.setId(bundleName);
            config.setPublishAuditStatus(publishAuditStatus);
            config.setAssets(new ArrayList<>());

            final BundlePublisher bundlePublisher = new BundlePublisher();
            bundlePublisher.init(config);

            try {
                bundlePublisher.process(new PublishStatus());
                fail("Expected DotPublishingException for bundle name " + bundleName);
            } catch (DotPublishingException e) {
                assertEquals("no audit status must be inserted for bundle id '" + bundleId + "'",
                        null, realAuditAPI.getPublishAuditStatus(bundleId));
            } finally {
                try {
                    realAuditAPI.deletePublishAuditStatus(bundleId);
                } catch (Exception ex) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    private byte[] tarGzWithEntry(final String entryName) throws IOException {
        final byte[] content = "content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GZIPOutputStream(bytes))) {
            final TarArchiveEntry entry = new TarArchiveEntry(entryName);
            entry.setSize(content.length);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
        }
        return bytes.toByteArray();
    }

}
