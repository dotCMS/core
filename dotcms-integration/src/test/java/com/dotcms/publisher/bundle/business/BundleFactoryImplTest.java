package com.dotcms.publisher.bundle.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.integritycheckers.FolderIntegrityChecker;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.GenerateBundlePublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BundleFactoryImplTest extends IntegrationTestBase {

    BundleFactoryImpl bundleFactoryimpl;

    @Before
    public void setup() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link BundleFactoryImpl#findUnsendBundles(String, int, int)}
     * Given Scenario: Admin should be able to view all the bundles regardless of the user who create it
     * ExpectedResult: Admin Role should be able to view/edit all the bundles.
     *
     */
    @Test
    public void test_findUnsendBundles_adminShouldBeAbleToSeeAllBundles() throws DotDataException, DotSecurityException {
        bundleFactoryimpl = new BundleFactoryImpl();

        //create an admin user and a user with limited permissions
        final User user = mockAdminUser();
        final User limitedUser = TestUserUtils.getChrisPublisherUser();

        //create the unsend bundle
        final String bundleIdAdmin = insertPublishingBundle(limitedUser.getUserId(),null);

        //Call the method to test
        //The admin user should be able to see all bundles
        final List<Bundle> data = bundleFactoryimpl.findUnsendBundles(user.getUserId(), 100, 0);

        //Assert that the bundle is returned
        assertNotNull(data);
        assertTrue(data.stream().anyMatch(bundle -> bundle.getId().equals(bundleIdAdmin)));
    }
    private String insertPublishingBundle(final String userId, final Date publishDate)
            throws DotDataException {
        final String uuid = UUIDGenerator.generateUuid();
        final Bundle bundle = new Bundle();
        bundle.setId(uuid);
        bundle.setName("testBundle"+System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(userId);
        bundle.setPublishDate(publishDate);
        APILocator.getBundleAPI().saveBundle(bundle);
        return uuid;
    }
    private User mockAdminUser() {
        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("dotcms.org.1");
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");
        when(adminUser.isAdmin()).thenReturn(true);
        return adminUser;
    }
}
