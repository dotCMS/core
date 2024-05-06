package com.dotcms.publishing.listener;

import static com.dotcms.publisher.business.PublisherTestUtil.createEndpoint;
import static com.dotcms.publisher.business.PublisherTestUtil.createEnvironment;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.company.CompanyAPI;
import com.dotcms.company.CompanyAPIFactory;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.Encryptor;
import java.security.Key;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SecurityKeyResetTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    /**
     * Given scenario: This basically tests that after a key reset the push publish endpoint are
     * still usable for decrypting important stuff using the new key; Expected: The endpoints that
     * were using the key that got re-generated remain usable.
     */
    @Test
    public void Test_Push_Publish_Endpoints_Have_Valid_Keys_After_Key_Reset_Expect_Success() throws Exception {

        final PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();

        final List<PublishingEndPoint> allEndPoints = publisherEndPointAPI.getAllEndPoints();
        for (final PublishingEndPoint endPoint : allEndPoints) {
            publisherEndPointAPI.deleteEndPointById(endPoint.getId());
        }

        final CompanyAPI companyAPI = CompanyAPIFactory.getInstance().getCompanyAPI();
        final Company company = companyAPI.getDefaultCompany();

        //This tests only makes sense if we reuse the same endpoint for both attempts
        final String seedText = "12345678";
        final Key originalKey = company.getKeyObj();
        final User admin = TestUserUtils.getAdminUser();
        final Environment environment = createEnvironment(admin);
        PublishingEndPoint endpoint = createEndpoint(environment, originalKey, seedText);
        final String authKeyBefore = endpoint.getAuthKey().toString();

        Assert.assertTrue(PushPublisher.retriveEndpointKeyDigest(endpoint).isPresent());

        //Regenerate the company key spawns an event that should be handled by anyone who might be affected by the change in the keys.
        final Company updatedCompany = companyAPI.regenerateKey(company, admin);

        //Refresh endpoint to see the newly reset key
        endpoint = publisherEndPointAPI.findEndPointById(endpoint.getId());

        final String authKeyAfter = endpoint.getAuthKey().toString();
        //After the company key reset event the endpoint should have changed.
        Assert.assertNotEquals(authKeyBefore, authKeyAfter);

        Assert.assertTrue(PushPublisher.retriveEndpointKeyDigest(endpoint).isPresent());

        //the company so we can access the new Key that
        final String decryptedText = Encryptor.decrypt(updatedCompany.getKeyObj(), authKeyAfter);
        Assert.assertEquals("These two must match", seedText, decryptedText);

    }


}
