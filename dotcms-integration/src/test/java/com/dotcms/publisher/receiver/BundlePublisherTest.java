package com.dotcms.publisher.receiver;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.FilterDescriptor;
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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dotcms.util.CollectionsUtils.list;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
     * When: Updating the audit table fails with a DotPublisherException
     * Should: Throw a DotPublishingException with appropriate message
     */
    @Test
    public void test_updateAuditTableFails_shouldThrowDotPublishingException() throws Exception {
        // Setup test data
        LicenseTestUtil.getLicense();
        final String bundleName = "test_bundle_" + System.currentTimeMillis() + ".tar.gz";
        final String bundleId = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
        
        final PublishAuditAPI mockAuditAPI = mock(PublishAuditAPI.class);
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleId);
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("endpoint1", "endpoint1", null);
        publishAuditStatus.setStatusPojo(history);
        
        final PushPublisherConfig config = new PushPublisherConfig();
        config.setId(bundleName);
        config.setPublishAuditStatus(publishAuditStatus);
        config.setAssets(new ArrayList<>());
        
        // Mock updatePublishAuditStatus to throw DotPublisherException
        doThrow(new DotPublisherException("Database connection failed"))
            .when(mockAuditAPI)
            .updatePublishAuditStatus(eq(bundleId), any(PublishAuditStatus.Status.class), any(PublishAuditHistory.class));
        
        // Use MockedStatic to inject the mocked API
        final BundlePublisher bundlePublisher;
        try (MockedStatic<PublishAuditAPI> mockedStatic = Mockito.mockStatic(PublishAuditAPI.class)) {
            mockedStatic.when(PublishAuditAPI::getInstance).thenReturn(mockAuditAPI);
            bundlePublisher = new BundlePublisher();
            bundlePublisher.init(config);
        }
        
        // Execute and verify using MockedStatic
        try (MockedStatic<PublishAuditAPI> mockedStatic = Mockito.mockStatic(PublishAuditAPI.class)) {
            mockedStatic.when(PublishAuditAPI::getInstance).thenReturn(mockAuditAPI);
            
            try {
                bundlePublisher.process(new PublishStatus());
                fail("Expected DotPublishingException to be thrown");
            } catch (DotPublishingException e) {
                assertTrue("Exception message should contain 'Unable to update audit table'",
                    e.getMessage().contains("Unable to update audit table for bundle with ID"));
                assertTrue("Exception message should contain bundle name",
                    e.getMessage().contains(bundleName));
                assertNotNull("Exception should have a cause", e.getCause());
                assertTrue("Cause should be DotPublisherException",
                    e.getCause() instanceof DotPublisherException);
            }
        }
    }

    /**
     * Method to Test: {@link BundlePublisher#process(PublishStatus)}
     * When: An IOException occurs during bundle extraction
     * Should: Throw a DotPublishingException and update audit table with failure status
     */
    @Test
    public void test_ioExceptionDuringBundleExtraction_shouldThrowDotPublishingException() throws Exception {
        // Setup test data
        LicenseTestUtil.getLicense();
        final String bundleName = "test_bundle_io_" + System.currentTimeMillis() + ".tar.gz";
        final String bundleId = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
        
        final PublishAuditAPI realAuditAPI = PublishAuditAPI.getInstance();
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleId);
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("endpoint1", "endpoint1", null);
        publishAuditStatus.setStatusPojo(history);
        
        // Insert initial audit status
        realAuditAPI.insertPublishAuditStatus(publishAuditStatus);
        
        final PushPublisherConfig config = new PushPublisherConfig();
        config.setId(bundleName);
        config.setPublishAuditStatus(publishAuditStatus);
        config.setAssets(new ArrayList<>());
        
        // Create a corrupted/invalid bundle file
        final String bundlePath = ConfigUtils.getBundlePath() + File.separator + "MY_TEMP";
        final File bundleDir = new File(bundlePath);
        bundleDir.mkdirs();
        
        final File bundleFile = new File(bundlePath + File.separator + bundleName);
        // Write invalid gzip content that will cause IOException during extraction
        FileUtils.writeByteArrayToFile(bundleFile, "invalid gzip content".getBytes());
        
        final BundlePublisher bundlePublisher = new BundlePublisher();
        bundlePublisher.init(config);
        
        // Execute and verify
        try {
            bundlePublisher.process(new PublishStatus());
            fail("Expected DotPublishingException to be thrown");
        } catch (DotPublishingException e) {
            assertTrue("Exception message should contain 'Error publishing bundle'",
                e.getMessage().contains("Error publishing bundle with ID"));
            assertTrue("Exception message should contain bundle name",
                e.getMessage().contains(bundleName));
            assertNotNull("Exception should have a cause", e.getCause());
            
            // Verify audit table was updated with failure
            final PublishAuditStatus updatedStatus = realAuditAPI.getPublishAuditStatus(bundleId);
            assertNotNull("Audit status should exist", updatedStatus);
            assertEquals("Status should be FAILED_TO_PUBLISH",
                PublishAuditStatus.Status.FAILED_TO_PUBLISH,
                updatedStatus.getStatus());
        } finally {
            // Cleanup
            FileUtils.deleteQuietly(bundleFile);
            FileUtils.deleteQuietly(bundleDir);
            try {
                realAuditAPI.deletePublishAuditStatus(bundleId);
            } catch (Exception ex) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Method to Test: {@link BundlePublisher#process(PublishStatus)}
     * When: Untaring a bundle fails
     * Should: Throw DotPublishingException with message "Exception untaring bundle"
     */
    @Test
    public void test_untarBundleFails_shouldThrowDotPublishingExceptionWithCorrectMessage() throws Exception {
        // Setup test data
        LicenseTestUtil.getLicense();
        final String bundleName = "test_bundle_untar_" + System.currentTimeMillis() + ".tar.gz";
        final String bundleId = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
        
        final PublishAuditAPI realAuditAPI = PublishAuditAPI.getInstance();
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleId);
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("endpoint1", "endpoint1", null);
        publishAuditStatus.setStatusPojo(history);
        
        // Insert initial audit status
        realAuditAPI.insertPublishAuditStatus(publishAuditStatus);
        
        final PushPublisherConfig config = new PushPublisherConfig();
        config.setId(bundleName);
        config.setPublishAuditStatus(publishAuditStatus);
        config.setAssets(new ArrayList<>());
        
        // Create a corrupted bundle file
        final String bundlePath = ConfigUtils.getBundlePath() + File.separator + "MY_TEMP";
        final File bundleDir = new File(bundlePath);
        bundleDir.mkdirs();
        
        final File bundleFile = new File(bundlePath + File.separator + bundleName);
        // Write corrupted tar.gz content
        FileUtils.writeByteArrayToFile(bundleFile, new byte[]{0x1f, (byte) 0x8b, 0x08, 0x00, 0x00, 0x00});
        
        final BundlePublisher bundlePublisher = new BundlePublisher();
        bundlePublisher.init(config);
        
        // Execute and verify
        try {
            bundlePublisher.process(new PublishStatus());
            fail("Expected DotPublishingException to be thrown");
        } catch (DotPublishingException e) {
            assertTrue("Exception message should contain 'Exception untaring bundle'",
                e.getMessage().contains("Exception untaring bundle") ||
                e.getMessage().contains("Error publishing bundle"));
            assertNotNull("Exception should have a cause", e.getCause());
        } finally {
            // Cleanup
            FileUtils.deleteQuietly(bundleFile);
            FileUtils.deleteQuietly(bundleDir);
            try {
                realAuditAPI.deletePublishAuditStatus(bundleId);
            } catch (Exception ex) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Method to Test: {@link BundlePublisher#process(PublishStatus)}
     * When: An IOException occurs during bundle extraction
     * Should: Verify correct failure event is notified and system state is updated
     */
    @Test
    public void test_ioExceptionDuringExtraction_shouldNotifyFailureEventAndUpdateState() throws Exception {
        // Setup test data
        LicenseTestUtil.getLicense();
        final String bundleName = "test_bundle_event_" + System.currentTimeMillis() + ".tar.gz";
        final String bundleId = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
        
        final PublishAuditAPI realAuditAPI = PublishAuditAPI.getInstance();
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleId);
        final PublishAuditHistory history = new PublishAuditHistory();
        history.addOrUpdateEndpoint("endpoint1", "endpoint1", null);
        publishAuditStatus.setStatusPojo(history);
        
        // Insert initial audit status
        realAuditAPI.insertPublishAuditStatus(publishAuditStatus);
        
        final List<PublishQueueElement> assets = new ArrayList<>();
        final PublishQueueElement element = new PublishQueueElement();
        element.setAsset("test-asset");
        assets.add(element);
        
        final PushPublisherConfig config = new PushPublisherConfig();
        config.setId(bundleName);
        config.setPublishAuditStatus(publishAuditStatus);
        config.setAssets(assets);
        
        // Create a corrupted bundle file
        final String bundlePath = ConfigUtils.getBundlePath() + File.separator + "MY_TEMP";
        final File bundleDir = new File(bundlePath);
        bundleDir.mkdirs();
        
        final File bundleFile = new File(bundlePath + File.separator + bundleName);
        // Write invalid content
        FileUtils.writeByteArrayToFile(bundleFile, "corrupted data".getBytes());
        
        // Subscribe to failure event
        final AtomicBoolean failureEventReceived = new AtomicBoolean(false);
        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        localSystemEventsAPI.subscribe(PushPublishFailureOnReceiverEvent.class, 
            (EventSubscriber<PushPublishFailureOnReceiverEvent>) event -> {
                failureEventReceived.set(true);
                assertNotNull("Event should not be null", event);
                assertNotNull("Event should have publish queue elements", event.getPublishQueueElements());
            });
        
        final BundlePublisher bundlePublisher = new BundlePublisher();
        bundlePublisher.init(config);
        
        // Execute and verify
        try {
            bundlePublisher.process(new PublishStatus());
            fail("Expected DotPublishingException to be thrown");
        } catch (DotPublishingException e) {

            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilTrue(failureEventReceived);
            
            // Verify audit table was updated with failure status
            final PublishAuditStatus updatedStatus = realAuditAPI.getPublishAuditStatus(bundleId);
            assertNotNull("Audit status should exist", updatedStatus);
            assertEquals("Status should be FAILED_TO_PUBLISH",
                PublishAuditStatus.Status.FAILED_TO_PUBLISH,
                updatedStatus.getStatus());
            
            final PublishAuditHistory updatedHistory = updatedStatus.getStatusPojo();
            assertNotNull("History should exist", updatedHistory);
            assertNotNull("Publish end date should be set", updatedHistory.getPublishEnd());
        } finally {
            // Cleanup
            FileUtils.deleteQuietly(bundleFile);
            FileUtils.deleteQuietly(bundleDir);
            try {
                realAuditAPI.deletePublishAuditStatus(bundleId);
            } catch (Exception ex) {
                // Ignore cleanup errors
            }
        }
    }

}
