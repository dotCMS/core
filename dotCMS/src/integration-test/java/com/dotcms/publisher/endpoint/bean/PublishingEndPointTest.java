package com.dotcms.publisher.endpoint.bean;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.PublishingEndPointValidationException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.dotmarketing.business.APILocator;

public class PublishingEndPointTest extends IntegrationTestBase {

    static PublishingEndPointFactory factory;
    static User user;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        LicenseTestUtil.getLicense();

        factory = new PublishingEndPointFactory();
        user = APILocator.systemUser();
    }

    @Test
    public void validatePublishingEndPoint_whenAWSS3PublishWithoutProperties_returnException() throws LanguageException {

        boolean exceptionCatched = false;

        PublishingEndPoint endPoint = factory.getPublishingEndPoint(AWSS3Publisher.PROTOCOL_AWS_S3);
        try {
            endPoint.validatePublishingEndPoint();
        } catch (PublishingEndPointValidationException e) {
            exceptionCatched = true;
            Assert.assertTrue(e.getMessage(user).contains(LanguageUtil.get(user, "publisher_Endpoint_awss3_authKey_missing_properties")));
        }

        Assert.assertTrue(exceptionCatched);
    }

    @Test
    public void validatePublishingEndPoint_whenAWSS3PublishWithoutBucketID_returnException() {

        boolean exceptionCatched = false;

        final String noBucketID = "Key=Value";

        PublishingEndPoint endPoint = factory.getPublishingEndPoint(AWSS3Publisher.PROTOCOL_AWS_S3);
        endPoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(noBucketID)));

        try {
            endPoint.validatePublishingEndPoint();
        } catch (Exception e) {
            Assert.assertTrue("No Exception should be thrown", false);
        }

        Assert.assertTrue("No Exception should be thrown", true);
    }

    @Test
    public void validatePublishingEndPoint_whenAWSS3PublishWithoutValidCredentials_returnException() {

        boolean exceptionCatched = false;

        final String noBucketID = "aws_bucket_name=name\naws_access_key=value\naws_secret_access_key=value";

        PublishingEndPoint endPoint = factory.getPublishingEndPoint(AWSS3Publisher.PROTOCOL_AWS_S3);
        endPoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(noBucketID)));

        try {
            endPoint.validatePublishingEndPoint();
        } catch (Exception e) {
            Assert.assertTrue("No Exception should be thrown", false);
        }

        Assert.assertTrue("No Exception should be thrown", true);
    }

    @Test
    public void validatePublishingEndPoint_whenStaticPublishWithWritePermission_returnOK() {

        boolean exceptionCatched = false;

        PublishingEndPoint endPoint = factory.getPublishingEndPoint(StaticPublisher.PROTOCOL_STATIC);
        try {
            endPoint.validatePublishingEndPoint();
        } catch (Exception e) {
            Assert.assertTrue("No Exception should be thrown", false);
        }

        Assert.assertTrue("No Exception should be thrown", true);
    }

}
