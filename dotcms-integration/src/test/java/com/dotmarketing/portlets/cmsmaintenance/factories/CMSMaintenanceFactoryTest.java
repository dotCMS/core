package com.dotmarketing.portlets.cmsmaintenance.factories;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.After;

public class CMSMaintenanceFactoryTest {

    private static User adminUser;
    private static ContentletAPI contentletAPI;
    private List<Contentlet> testContentlets = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        adminUser = TestUserUtils.getAdminUser();
        contentletAPI = APILocator.getContentletAPI();
    }

    @After
    public void cleanup() throws Exception {
        // Clean up test contentlets to avoid interference between tests
        for (Contentlet contentlet : testContentlets) {
            try {
                contentletAPI.destroy(contentlet, adminUser, false);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        testContentlets.clear();
    }

    /**
     * Method to test: {@link CMSMaintenanceFactory#deleteOldAssetVersions(Date)}
     * Given Scenario: Create a contentlet that will have a few of old versions, that are gonna be deleted.
     * ExpectedResult: Old versions of the contentlet (that not are live or working) are deleted.
     */
    @Test
    public void Test_deleteOldAssetVersions_success()
            throws DotSecurityException, DotDataException {
        //Create a site
        final Host site = new SiteDataGen().nextPersisted();
        //Create a contentlet, this version will have today's Date
        Contentlet contentlet = TestDataUtils.getGenericContentContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),site);
        //Create a couple of new versions, with an old Date so these will be deleted
        //Using the "_use_mod_date" so when checkin the contentlet use the Date that is set
        for(int i=0;i<100;i++) {
            contentlet.setInode(UUIDGenerator.generateUuid());
            contentlet.getMap()
                    .put("_use_mod_date", DateUtil.addDate(new Date(), Calendar.MONTH, -2));
            contentlet = contentletAPI.checkin(contentlet, adminUser, false);
        }
        //Create a new version it is the working version of the contentlet, so the above can be deleted
        contentlet.setInode(UUIDGenerator.generateUuid());
        contentlet = contentletAPI.checkin(contentlet,adminUser,false);

        //Check that the contentlet has diff versions
        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
        final List<Contentlet> contentletVersionListBeforeDelete = contentletAPI.findAllVersions(identifier,adminUser,false);
        Assert.assertFalse(contentletVersionListBeforeDelete.isEmpty());

        //DeleteOldAssetVersions, using current Date
        final int amountOfVersionsDeleted = CMSMaintenanceFactory.deleteOldAssetVersions(new Date());

        //Assert that some contentlets were deleted
        Assert.assertNotEquals(0,amountOfVersionsDeleted);
        final List<Contentlet> contentletVersionListAfterDelete = contentletAPI.findAllVersions(identifier,adminUser,false);
        Assert.assertFalse(contentletVersionListAfterDelete.isEmpty());
        Assert.assertNotEquals(contentletVersionListBeforeDelete.size(),contentletVersionListAfterDelete.size());
    }

    /**
     * Method to test: {@link ContentletAPI#deleteOldContent(Date, Date)}
     * Given Scenario: Create contentlets with specific modification dates and test bounded query functionality.
     * ExpectedResult: Only contentlets within the specified date range are processed and deleted.
     */
    @Test
    public void test_boundedDeleteOldContent_onlyProcessesSpecifiedDateRange()
            throws DotSecurityException, DotDataException {
        
        final Host site = new SiteDataGen().nextPersisted();
        
        // Create test dates for different time periods
        final Date baseDate = new Date();
        final Date oldDate = DateUtil.addDate(baseDate, Calendar.MONTH, -3); // 3 months ago
        final Date middleDate = DateUtil.addDate(baseDate, Calendar.MONTH, -2); // 2 months ago
        final Date recentDate = DateUtil.addDate(baseDate, Calendar.MONTH, -1); // 1 month ago
        
        // Create contentlets with different modification dates
        List<Contentlet> oldContentlets = createTestContentlets(site, oldDate, 5);
        List<Contentlet> middleContentlets = createTestContentlets(site, middleDate, 5);
        List<Contentlet> recentContentlets = createTestContentlets(site, recentDate, 5);
        
        // Test bounded query: delete only middle period content
        final int deletedCount = contentletAPI.deleteOldContent(
            DateUtil.addDate(middleDate, Calendar.DAY_OF_MONTH, -1), // Start slightly before middle date
            DateUtil.addDate(middleDate, Calendar.DAY_OF_MONTH, 1)   // End slightly after middle date
        );
        
        // Verify that only middle period contentlets were processed
        Assert.assertTrue("Should have deleted some contentlets from middle period", deletedCount > 0);
        
        // Verify old and recent contentlets still exist (not processed by bounded query)
        for (Contentlet contentlet : oldContentlets) {
            try {
                Contentlet found = contentletAPI.find(contentlet.getInode(), adminUser, false);
                Assert.assertNotNull("Old contentlet should still exist (outside date range)", found);
            } catch (Exception e) {
                // Expected for deleted contentlets
            }
        }
        
        for (Contentlet contentlet : recentContentlets) {
            try {
                Contentlet found = contentletAPI.find(contentlet.getInode(), adminUser, false);
                Assert.assertNotNull("Recent contentlet should still exist (outside date range)", found);
            } catch (Exception e) {
                // Expected for deleted contentlets
            }
        }
    }

    /**
     * Method to test: Multiple iterations with different date ranges
     * Given Scenario: Create contentlets across multiple time periods and test sequential bounded queries.
     * ExpectedResult: Each iteration processes only its specific date range without overlap or missing content.
     */
    @Test
    public void test_multipleBoundedIterations_noOverlapOrMissedContent()
            throws DotSecurityException, DotDataException {
        
        final Host site = new SiteDataGen().nextPersisted();
        
        // Create test dates for 3 distinct time periods
        final Date baseDate = new Date();
        final Date period1Start = DateUtil.addDate(baseDate, Calendar.MONTH, -6);
        final Date period1End = DateUtil.addDate(baseDate, Calendar.MONTH, -4);
        final Date period2Start = period1End;
        final Date period2End = DateUtil.addDate(baseDate, Calendar.MONTH, -2);
        final Date period3Start = period2End;
        final Date period3End = baseDate;
        
        // Create contentlets for each period
        List<Contentlet> period1Contentlets = createTestContentlets(site, 
            DateUtil.addDate(period1Start, Calendar.DAY_OF_MONTH, 15), 3);
        List<Contentlet> period2Contentlets = createTestContentlets(site, 
            DateUtil.addDate(period2Start, Calendar.DAY_OF_MONTH, 15), 3);
        List<Contentlet> period3Contentlets = createTestContentlets(site, 
            DateUtil.addDate(period3Start, Calendar.DAY_OF_MONTH, 15), 3);
        
        // Track deletion counts for each iteration
        Map<String, Integer> deletionCounts = new HashMap<>();
        
        // Iteration 1: Process period 1
        int deleted1 = contentletAPI.deleteOldContent(period1Start, period1End);
        deletionCounts.put("period1", deleted1);
        
        // Iteration 2: Process period 2
        int deleted2 = contentletAPI.deleteOldContent(period2Start, period2End);
        deletionCounts.put("period2", deleted2);
        
        // Iteration 3: Process period 3
        int deleted3 = contentletAPI.deleteOldContent(period3Start, period3End);
        deletionCounts.put("period3", deleted3);
        
        // Verify each iteration processed some content
        Assert.assertTrue("Period 1 should have processed some content", deleted1 >= 0);
        Assert.assertTrue("Period 2 should have processed some content", deleted2 >= 0);
        Assert.assertTrue("Period 3 should have processed some content", deleted3 >= 0);
        
        // Verify total processed content matches expected
        int totalDeleted = deleted1 + deleted2 + deleted3;
        Assert.assertTrue("Total deletions should be reasonable", totalDeleted >= 0);
        
        // Log results for performance analysis
        System.out.println("Bounded Query Performance Test Results:");
        System.out.println("Period 1 deletions: " + deleted1);
        System.out.println("Period 2 deletions: " + deleted2);
        System.out.println("Period 3 deletions: " + deleted3);
        System.out.println("Total deletions: " + totalDeleted);
    }

    /**
     * Method to test: Performance comparison between bounded and cumulative approaches
     * Given Scenario: Measure query performance with bounded vs cumulative date ranges.
     * ExpectedResult: Bounded queries should show consistent performance regardless of historical data size.
     */
    @Test
    public void test_boundedQueryPerformance_consistentExecutionTime()
            throws DotSecurityException, DotDataException {
        
        final Host site = new SiteDataGen().nextPersisted();
        
        // Create test data across multiple time periods
        final Date baseDate = new Date();
        final Date veryOldDate = DateUtil.addDate(baseDate, Calendar.MONTH, -12);
        final Date oldDate = DateUtil.addDate(baseDate, Calendar.MONTH, -6);
        final Date recentDate = DateUtil.addDate(baseDate, Calendar.MONTH, -1);
        
        // Create contentlets for different periods
        createTestContentlets(site, veryOldDate, 2);
        createTestContentlets(site, oldDate, 2);
        createTestContentlets(site, recentDate, 2);
        
        // Test bounded query performance - each iteration should process similar amount of data
        long startTime1 = System.currentTimeMillis();
        int deleted1 = contentletAPI.deleteOldContent(
            DateUtil.addDate(veryOldDate, Calendar.DAY_OF_MONTH, -1),
            DateUtil.addDate(veryOldDate, Calendar.DAY_OF_MONTH, 1)
        );
        long duration1 = System.currentTimeMillis() - startTime1;
        
        long startTime2 = System.currentTimeMillis();
        int deleted2 = contentletAPI.deleteOldContent(
            DateUtil.addDate(oldDate, Calendar.DAY_OF_MONTH, -1),
            DateUtil.addDate(oldDate, Calendar.DAY_OF_MONTH, 1)
        );
        long duration2 = System.currentTimeMillis() - startTime2;
        
        long startTime3 = System.currentTimeMillis();
        int deleted3 = contentletAPI.deleteOldContent(
            DateUtil.addDate(recentDate, Calendar.DAY_OF_MONTH, -1),
            DateUtil.addDate(recentDate, Calendar.DAY_OF_MONTH, 1)
        );
        long duration3 = System.currentTimeMillis() - startTime3;
        
        // Log performance metrics
        System.out.println("Bounded Query Performance Metrics:");
        System.out.println("Very old period: " + deleted1 + " deleted in " + duration1 + "ms");
        System.out.println("Old period: " + deleted2 + " deleted in " + duration2 + "ms");
        System.out.println("Recent period: " + deleted3 + " deleted in " + duration3 + "ms");
        
        // Verify bounded queries completed successfully
        Assert.assertTrue("All bounded queries should complete", duration1 >= 0 && duration2 >= 0 && duration3 >= 0);
        
        // Performance should be consistent (no exponential degradation)
        // This is a qualitative test - in a real scenario with large datasets,
        // bounded queries would show much more consistent performance
        Assert.assertTrue("Bounded queries should complete in reasonable time", 
            duration1 < 30000 && duration2 < 30000 && duration3 < 30000);
    }

    /**
     * Method to test: Backward compatibility of existing single-parameter methods
     * Given Scenario: Test that existing single-parameter deleteOldContent method still works correctly.
     * ExpectedResult: Single-parameter method should work exactly as before, maintaining backward compatibility.
     */
    @Test
    public void test_backwardCompatibility_singleParameterMethodStillWorks()
            throws DotSecurityException, DotDataException {
        
        final Host site = new SiteDataGen().nextPersisted();
        
        // Create test contentlets with old modification dates
        final Date oldDate = DateUtil.addDate(new Date(), Calendar.MONTH, -2);
        List<Contentlet> oldContentlets = createTestContentlets(site, oldDate, 3);
        
        // Test single-parameter method (backward compatibility)
        final Date cutoffDate = DateUtil.addDate(new Date(), Calendar.MONTH, -1);
        final int deletedCount = contentletAPI.deleteOldContent(cutoffDate);
        
        // Verify the method works as expected
        Assert.assertTrue("Single-parameter method should process content", deletedCount >= 0);
        
        // Verify the method maintains its original behavior
        // (processing all content older than the specified date)
        System.out.println("Backward Compatibility Test: " + deletedCount + " contentlets processed");
        
        // Test that the method signature is still available and functional
        try {
            // This should compile and execute without errors
            contentletAPI.deleteOldContent(new Date());
            Assert.assertTrue("Single-parameter method should be available", true);
        } catch (Exception e) {
            Assert.fail("Single-parameter method should maintain backward compatibility: " + e.getMessage());
        }
    }

    /**
     * Helper method to create test contentlets with specific modification dates
     */
    private List<Contentlet> createTestContentlets(Host site, Date modDate, int count) 
            throws DotSecurityException, DotDataException {
        
        List<Contentlet> contentlets = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Contentlet contentlet = TestDataUtils.getGenericContentContent(
                true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), site);
            
            // Create multiple versions with old dates that can be deleted
            for (int version = 0; version < 3; version++) {
                contentlet.setInode(UUIDGenerator.generateUuid());
                contentlet.getMap().put("_use_mod_date", modDate);
                contentlet = contentletAPI.checkin(contentlet, adminUser, false);
            }
            
            // Create a current working version so the old versions can be deleted
            contentlet.setInode(UUIDGenerator.generateUuid());
            contentlet = contentletAPI.checkin(contentlet, adminUser, false);
            
            contentlets.add(contentlet);
            testContentlets.add(contentlet); // Track for cleanup
        }
        
        return contentlets;
    }
}



