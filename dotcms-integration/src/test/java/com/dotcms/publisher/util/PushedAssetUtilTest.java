package com.dotcms.publisher.util;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.StaticPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.dependencies.PushedAssetUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PushedAssetUtilTest extends IntegrationTestBase {

    private final User systemUser = APILocator.systemUser();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to test:</b> {@link PushedAssetUtil#savePushedAssetForAllEnv(Object, PusheableAsset)} <p>
     * <b>Given Scenario:</b> PP a content to 2 diff environments (one static and one dynamic) <p>
     * <b>ExpectedResult:</b> In the pushed assets table, only one entry should be inserted, for the dynamic one.
     */
    @Test
    public void savePushedAssetForAllEnv() throws DotDataException {
        final Environment dynamicEnv = new EnvironmentDataGen().nextPersisted();
        final Environment staticEnv = new EnvironmentDataGen().nextPersisted();
        final PushPublishingEndPoint dynamic_endPoint = new PushPublishingEndPointDataGen()
                .environment(dynamicEnv)
                .nextPersisted();
        final StaticPublishingEndPoint static_endPoint = new StaticPublishingEndPointDataGen()
                .protocol("static")
                .address("static.dotcms.com")
                .port("80")
                .authKey("static_publish_to=dotcms-static-{hostname}-{languageIso}")
                .environment(staticEnv)
                .nextPersisted();

        final Bundle testBundle = createTestBundle(false, List.of(dynamicEnv,staticEnv));

        final PushPublisherConfig config = createPushPublisherConfigMock(
                testBundle, false, false, PublisherConfig.Operation.PUBLISH);

        final PushedAssetUtil pushedAssetUtil = new PushedAssetUtil(config);

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Contentlet contentlet =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(new Date())
                .nextPersisted();

        pushedAssetUtil.savePushedAssetForAllEnv(contentlet,PusheableAsset.CONTENTLET);

        final List<PushedAsset> pushedAssetList = APILocator.getPushedAssetsAPI().getPushedAssets(contentlet.getIdentifier());
        Assert.assertEquals(1,pushedAssetList.size());
        Assert.assertEquals(dynamicEnv.getId(),pushedAssetList.get(0).getEnvironmentId());

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

    private PushPublisherConfig createPushPublisherConfigMock(Bundle testBundle,
                                                              boolean isDownloading,
                                                              boolean isStatic, PublisherConfig.Operation operation) {

        final PushPublisherConfig config = mock(PushPublisherConfig.class);
        when(config.getId()).thenReturn(testBundle.getId());
        when(config.isStatic()).thenReturn(isStatic);
        when(config.isDownloading()).thenReturn(isDownloading);
        when(config.getOperation()).thenReturn(operation);
        return config;
    }
}
