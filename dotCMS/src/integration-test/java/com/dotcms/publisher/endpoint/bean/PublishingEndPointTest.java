package com.dotcms.publisher.endpoint.bean;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.PublishingEndPointValidationException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PublishingEndPointTest extends IntegrationTestBase {

    static PublishingEndPointFactory factory;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        LicenseTestUtil.getLicense();

        factory = new PublishingEndPointFactory();
    }

    @Test
    public void validatePublishingEndPoint_whenAWSS3PublishWithoutProperties_returnException() {

        boolean exceptionCatched = false;

        PublishingEndPoint endPoint = factory.getPublishingEndPoint(AWSS3Publisher.PROTOCOL_AWS_S3);
        try {
            endPoint.validatePublishingEndPoint();
        } catch (PublishingEndPointValidationException e) {
            exceptionCatched = true;
            Assert.assertTrue(e.getI18nmessages().contains("publisher_Endpoint_awss3_authKey_missing_properties"));
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
        } catch (PublishingEndPointValidationException e) {
            exceptionCatched = true;
            Assert.assertTrue(e.getI18nmessages().contains("publisher_Endpoint_awss3_authKey_missing_bucket_id"));
            Assert.assertTrue(
                e.getI18nmessages().contains("publisher_Endpoint_DefaultAWSCredentialsProviderChain_invalid"));
            Assert.assertFalse(e.getI18nmessages().contains("publisher_Endpoint_awss3_authKey_properties_invalid"));
        }

        Assert.assertTrue(exceptionCatched);
    }

    @Test
    public void validatePublishingEndPoint_whenAWSS3PublishWithoutValidCredentials_returnException() {

        boolean exceptionCatched = false;

        final String noBucketID = "aws_bucket_name=name\naws_access_key=value\naws_secret_access_key=value";

        PublishingEndPoint endPoint = factory.getPublishingEndPoint(AWSS3Publisher.PROTOCOL_AWS_S3);
        endPoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(noBucketID)));

        try {
            endPoint.validatePublishingEndPoint();
        } catch (PublishingEndPointValidationException e) {
            exceptionCatched = true;
            Assert.assertFalse(e.getI18nmessages().contains("publisher_Endpoint_awss3_authKey_missing_bucket_id"));
            Assert.assertFalse(
                e.getI18nmessages().contains("publisher_Endpoint_DefaultAWSCredentialsProviderChain_invalid"));
            Assert.assertTrue(e.getI18nmessages().contains("publisher_Endpoint_awss3_authKey_properties_invalid"));
        }

        Assert.assertTrue(exceptionCatched);
    }

    @Test
    public void validatePublishingEndPoint_whenStaticPublishWithWritePermission_returnOK() {

        boolean exceptionCatched = false;

        PublishingEndPoint endPoint = factory.getPublishingEndPoint(StaticPublisher.PROTOCOL_STATIC);
        try {
            endPoint.validatePublishingEndPoint();
        } catch (PublishingEndPointValidationException e) {
            exceptionCatched = true;
        }

        Assert.assertFalse(exceptionCatched);
    }

}
