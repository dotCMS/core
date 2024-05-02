package com.dotcms.enterprise.publishing.staticpublishing;

import static com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_ID;
import static com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher.PROTOCOL_AWS_S3;
import static com.dotmarketing.util.WebKeys.CURRENT_HOST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.Encryptor;
import java.security.Key;
import java.util.Date;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
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


}
