package com.dotcms.publisher.util;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.dependencies.DependencyModDateUtil;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class DependencyModDateUtilTest extends IntegrationTestBase {

    private final User systemUser = APILocator.systemUser();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider(format = "%m: ForcePush: %p[0]")
    public static Object[] dataProviderForcePush() {
        return new Object[]{true, false};
    }

    @Test
    @UseDataProvider("dataProviderForcePush")
    public void addOrClean_shouldCreatePushedAssetEntry(final boolean forcePush) throws DotSecurityException, DotDataException {
        final Environment environment = createTestEnviroment();
        final PublishingEndPoint endPoint = createTestEndpoint(environment.getId());

        final Bundle testBundle = createTestBundle(forcePush, Collections.singletonList(environment));

        final boolean IS_DOWNLOADING = false;
        final boolean IS_STATIC = false;
        PushPublisherConfig config = mock(PushPublisherConfig.class);
        when(config.getId()).thenReturn(testBundle.getId());
        when(config.isStatic()).thenReturn(IS_STATIC);
        when(config.isDownloading()).thenReturn(IS_DOWNLOADING);
        when(config.getOperation()).thenReturn(Operation.PUBLISH);

        final DependencyModDateUtil dependencySet = new DependencyModDateUtil();

        final Host site = new SiteDataGen().nextPersisted();
        final Contentlet testGenericContent = TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                site);
        ContentletDataGen.publish(testGenericContent);

        final Contentlet contentlet = APILocator.getContentletAPI()
                .findContentletByIdentifier(testGenericContent.getIdentifier(),
            false, 1, systemUser, false);

        dependencySet.add(contentlet.getIdentifier(), new Date());

        final PushedAsset pushedAsset = APILocator.getPushedAssetsAPI().getLastPushForAsset(contentlet.getIdentifier(),
            environment.getId(), endPoint.getId());

        assertNotNull(pushedAsset);
    }

    private Bundle createTestBundle(final boolean forcePush, final List<Environment> environments)
        throws DotDataException {
        final Bundle bundle = new Bundle();
        bundle.setName("testBundle"+System.currentTimeMillis());
        bundle.setForcePush(forcePush);
        bundle.setPublishDate(new Date());
        bundle.setOwner(systemUser.getUserId());
        APILocator.getBundleAPI().saveBundle(bundle, environments);
        return bundle;
    }

    private Environment createTestEnviroment() throws DotDataException, DotSecurityException {
        final Environment environment = new Environment();
        environment.setName("testEnvironment"+System.currentTimeMillis());
        environment.setPushToAll(true);
        APILocator.getEnvironmentAPI().saveEnvironment(environment, null);
        return environment;
    }

    private PublishingEndPoint createTestEndpoint(final String environmentId) throws DotDataException {
        final PublishingEndPoint publishingEndPoint = new PushPublishingEndPoint();
        publishingEndPoint.setGroupId(environmentId);
        publishingEndPoint.setServerName(new StringBuilder("testEndpoint").append(System.currentTimeMillis()));
        publishingEndPoint.setAddress("x.x.x.x");
        publishingEndPoint.setPort("8080");
        publishingEndPoint.setProtocol("http");
        publishingEndPoint.setEnabled(true);
        publishingEndPoint.setAuthKey(new StringBuilder("key"));
        publishingEndPoint.setSending(false);
        APILocator.getPublisherEndPointAPI().saveEndPoint(publishingEndPoint);
        return publishingEndPoint;
    }

}