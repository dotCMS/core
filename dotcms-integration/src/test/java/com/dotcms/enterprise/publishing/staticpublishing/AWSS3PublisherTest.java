package com.dotcms.enterprise.publishing.staticpublishing;

import static com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_ID;
import static com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher.PROTOCOL_AWS_S3;
import static com.dotmarketing.util.WebKeys.CURRENT_HOST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.Encryptor;
import java.security.Key;
import java.util.Date;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.mockito.Mockito;

public class AWSS3PublisherTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * This tests that whenever we call {@link AWSS3Publisher#shouldForcePush(String, long)} and the bucket exists we get true
     * @throws Exception
     */
    @Test
    @Ignore
    public void Test_Should_Force_Push() throws Exception{

        final String environmentId = "environment-ID";
        final String endPointId = "endpoint-ID";
        final String configId = UUIDUtil.uuid();
        Host host = mock(Host.class);
        PublisherConfig config = mock(PublisherConfig.class);
        when(config.clone()).thenReturn(config);
        when(config.getId()).thenReturn(configId);
        when(config.get( "dot-static-date")).thenReturn(new Date());
        when(config.get("currentLanguage")).thenReturn("1");
        when(config.get(CURRENT_HOST)).thenReturn(host);
        when(config.get(DOTCMS_PUSH_AWS_S3_BUCKET_ID)).thenReturn("xyz");

        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.find(Mockito.eq("123"), Mockito.any(User.class), Mockito.eq(false))).thenReturn(host);

        final PublishAuditAPI publishAuditAPI = mock(PublishAuditAPI.class);
        final EnvironmentAPI environmentAPI = mock(EnvironmentAPI.class);
        final PublishingEndPointAPI publisherEndPointAPI = mock(PublishingEndPointAPI.class);
        final PushedAssetsAPI pushedAssetsAPI = mock(PushedAssetsAPI.class);

        Environment environment = mock(Environment.class);
        when(environment.getName()).thenReturn("lol");
        when(environment.getId()).thenReturn(environmentId);
        when(environmentAPI.findEnvironmentsByBundleId(Mockito.eq(configId))).thenReturn(ImmutableList.of(environment));

        PublishingEndPoint endPoint = mock(PublishingEndPoint.class);
        when(endPoint.getId()).thenReturn(endPointId);
        when(endPoint.getProtocol()).thenReturn(PROTOCOL_AWS_S3);
        when(endPoint.isEnabled()).thenReturn(true);

        final String string = "aws_access_key=AKIAUIFR72B\n"
                + "aws_secret_access_key=YOnacecA5muddtRWeiqST9MyFULGOD9z\n"
                + "aws_bucket_name=xyz.static.push.test\n"
                + "aws_bucket_region=us-west-2";

        final Key key = PublicCompanyFactory.getDefaultCompany().getKeyObj();
        final String encrypted = Encryptor.encrypt(key, string);
        when(endPoint.getAuthKey()).thenReturn(new StringBuilder(encrypted));

        when(publisherEndPointAPI.findSendingEndPointsByEnvironment(environmentId)).thenReturn(ImmutableList.of(endPoint));

        final AWSS3Publisher publisher = new AWSS3Publisher(hostAPI, publishAuditAPI,
                environmentAPI, publisherEndPointAPI, pushedAssetsAPI);

        publisher.init(config);

        Assert.assertTrue(publisher.shouldForcePush("123", 1));
    }

    private static final String VANITY_ALIAS_FLAG = "STATIC_PUSH_S3_VANITY_ALIAS_ENABLED";

    /**
     * Method to test: {@link AWSS3Publisher#process(PublishStatus)}
     * Given Scenario: a bundle that has no {@code live} folder and S3 vanity aliases are off
     * ExpectedResult: the endpoint is recorded as a success whose info says the bundle was empty
     */
    @Test
    public void test_process_bundleWithoutContentAndWithoutAliases_endpointIsReportedEmpty() throws Exception {
        final EndpointDetail detail = processBundleWithoutLiveFolder(false);

        Assert.assertEquals(PublishAuditStatus.Status.SUCCESS.getCode(), detail.getStatus());
        Assert.assertEquals(StaticPublisher.EMPTY_BUNDLE_INFO, detail.getInfo());
    }

    /**
     * Method to test: {@link AWSS3Publisher#process(PublishStatus)}
     * Given Scenario: a bundle that has no {@code live} folder and S3 vanity aliases are on, so the
     *                 bundle's vanity URLs may still be published or removed as aliases
     * ExpectedResult: the endpoint is recorded as a success and its info does not claim the bundle
     *                 was empty (issue #34434)
     */
    @Test
    public void test_process_bundleWithoutContentAndWithAliases_endpointIsNotReportedEmpty() throws Exception {
        final EndpointDetail detail = processBundleWithoutLiveFolder(true);

        Assert.assertEquals(PublishAuditStatus.Status.SUCCESS.getCode(), detail.getStatus());
        Assert.assertNotEquals(StaticPublisher.EMPTY_BUNDLE_INFO, detail.getInfo());
    }

    /**
     * Runs {@link AWSS3Publisher#process(PublishStatus)} against one S3 endpoint, with the S3 client
     * replaced by a mock, over a bundle that has no {@code live} folder.
     *
     * @param aliasesEnabled value of the S3 vanity alias flag for the run
     * @return the detail the publisher recorded for the endpoint
     */
    private EndpointDetail processBundleWithoutLiveFolder(final boolean aliasesEnabled) throws Exception {
        return processBundleWithoutLiveFolder(aliasesEnabled, null);
    }

    /**
     * Method to test: {@link AWSS3Publisher#process(PublishStatus)}
     * Given Scenario: a retry where the S3 endpoint has a FAILED entry from the earlier attempt and
     *                 the upload now works
     * ExpectedResult: the entry is replaced by this run's success. Before the fix the old failure
     *                 stayed, so the bundle kept reporting failed after S3 recovered (issue #34434)
     */
    @Test
    public void test_process_retryAfterFailure_failedEntryIsReplacedBySuccess() throws Exception {
        final EndpointDetail detail = processBundleWithoutLiveFolder(false,
                PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode());

        Assert.assertEquals(PublishAuditStatus.Status.SUCCESS.getCode(), detail.getStatus());
    }

    /**
     * Same as {@link #processBundleWithoutLiveFolder(boolean)}, with an earlier result already
     * recorded for the endpoint.
     *
     * @param previousStatus status code of the earlier result, or null for a first attempt
     */
    private EndpointDetail processBundleWithoutLiveFolder(final boolean aliasesEnabled,
            final Integer previousStatus) throws Exception {
        final String environmentId = "environment-" + UUIDUtil.uuid();
        final String endPointId = "endpoint-" + UUIDUtil.uuid();
        final String configId = UUIDUtil.uuid();

        final PublisherConfig config = new PublisherConfig();
        config.setId(configId);
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        final PublishAuditHistory history = new PublishAuditHistory();
        if (previousStatus != null) {
            final EndpointDetail previous = new EndpointDetail();
            previous.setStatus(previousStatus);
            previous.setInfo("earlier attempt");
            history.addOrUpdateEndpoint(environmentId, endPointId, previous);
        }
        final PublishAuditStatus auditStatus = new PublishAuditStatus(configId);
        auditStatus.setStatusPojo(history);
        final PublishAuditAPI publishAuditAPI = mock(PublishAuditAPI.class);
        when(publishAuditAPI.getPublishAuditStatus(configId)).thenReturn(auditStatus);

        final Environment environment = mock(Environment.class);
        when(environment.getId()).thenReturn(environmentId);
        when(environment.getPushToAll()).thenReturn(true);
        final EnvironmentAPI environmentAPI = mock(EnvironmentAPI.class);
        when(environmentAPI.findEnvironmentsByBundleId(configId)).thenReturn(ImmutableList.of(environment));

        final String credentials = "aws_access_key=test-key\n"
                + "aws_secret_access_key=test-secret\n"
                + "aws_bucket_name=empty.bundle.test\n"
                + "aws_bucket_region=us-west-2";
        final Key key = PublicCompanyFactory.getDefaultCompany().getKeyObj();
        final PublishingEndPoint endPoint = mock(PublishingEndPoint.class);
        when(endPoint.getId()).thenReturn(endPointId);
        when(endPoint.getProtocol()).thenReturn(PROTOCOL_AWS_S3);
        when(endPoint.isEnabled()).thenReturn(true);
        when(endPoint.getAuthKey()).thenReturn(new StringBuilder(Encryptor.encrypt(key, credentials)));
        final PublishingEndPointAPI publisherEndPointAPI = mock(PublishingEndPointAPI.class);
        when(publisherEndPointAPI.findSendingEndPointsByEnvironment(environmentId))
                .thenReturn(ImmutableList.of(endPoint));

        // the S3 client is replaced, so nothing here talks to AWS
        final AWSS3Publisher publisher = new AWSS3Publisher(mock(HostAPI.class), publishAuditAPI,
                environmentAPI, publisherEndPointAPI, mock(PushedAssetsAPI.class),
                mock(S3VanityAliasService.class)) {
            @Override
            AWSS3EndPointPublisher getAWSS3EndPointPublisher(final String tokenProp,
                    final String secretProp, final String endPoint, final String region) {
                return mock(AWSS3EndPointPublisher.class);
            }
        };
        publisher.init(config);

        final boolean previousFlag = Config.getBooleanProperty(VANITY_ALIAS_FLAG, false);
        try {
            Config.setProperty(VANITY_ALIAS_FLAG, aliasesEnabled);
            publisher.process(new PublishStatus());
        } finally {
            Config.setProperty(VANITY_ALIAS_FLAG, previousFlag);
        }

        final EndpointDetail detail = history.getEndpointsMap().getOrDefault(environmentId, java.util.Map.of())
                .get(endPointId);
        Assert.assertNotNull("the endpoint must have a detail in the audit history", detail);
        return detail;
    }

}
