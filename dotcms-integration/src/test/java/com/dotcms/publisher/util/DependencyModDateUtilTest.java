package com.dotcms.publisher.util;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.PushedAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.dependencies.DependencyModDateUtil;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Calendar;
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

        return new TestCase[]{
            new TestCase(true, false, false, Operation.PUBLISH),
            new TestCase(true, false, true, Operation.PUBLISH),
            new TestCase(true, true, false, Operation.PUBLISH),

            new TestCase(true, false, false, Operation.UNPUBLISH),
            new TestCase(true, false, true, Operation.UNPUBLISH),
            new TestCase(true, true, false, Operation.UNPUBLISH),

            new TestCase(false, false, false, Operation.PUBLISH),
            new TestCase(false, false, true, Operation.PUBLISH),
            new TestCase(false, true, false, Operation.PUBLISH),

            new TestCase(false, false, false, Operation.UNPUBLISH),
            new TestCase(false, false, true, Operation.UNPUBLISH),
            new TestCase(false, true, false, Operation.UNPUBLISH)
        };
    }

    /**
     * Method to Test: {@link DependencyModDateUtil#excludeByModDate(Object)}}
     * When: A contentlet with any Push before
     * Should: always return false
     *
     * @param testCase
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("dataProviderForcePush")
    public void excludeByModDateContentNotPushBefore(final TestCase testCase) throws DotDataException {
        final Environment environment_1 = new EnvironmentDataGen().nextPersisted();
        final Environment environment_2 = new EnvironmentDataGen().nextPersisted();
        final PushPublishingEndPoint endPoint = new PushPublishingEndPointDataGen()
                .environment(environment_1)
                .nextPersisted();

        final Bundle testBundle = createTestBundle(testCase.isForcePush, Collections.singletonList(environment_1));

        PushPublisherConfig config = createPushPublisherConfigMock(
                testBundle, testCase.isDownloading, testCase.isStatic, testCase.operation);

        final DependencyModDateUtil dependencyModDateUtil = new DependencyModDateUtil(config);

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Contentlet contentlet =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .nextPersisted();

        final boolean excludeByModDate = dependencyModDateUtil.excludeByModDate(contentlet, PusheableAsset.CONTENTLET);
        assertFalse(excludeByModDate);
    }

    /**
     * Method to Test: {@link DependencyModDateUtil#excludeByModDate(Object)}}
     * When: A contentlet with a PushAsset with date before that moddate
     * Should: always return false
     *
     * @param testCase
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("dataProviderForcePush")
    public void excludeByModDateContentWithPushAssetBeforeModDate(final TestCase testCase) throws DotDataException {
        final Environment environment_1 = new EnvironmentDataGen().nextPersisted();
        final Environment environment_2 = new EnvironmentDataGen().nextPersisted();
        final PushPublishingEndPoint endPoint = new PushPublishingEndPointDataGen()
                .environment(environment_1)
                .nextPersisted();

        final Bundle testBundle = createTestBundle(testCase.isForcePush, Collections.singletonList(environment_1));

        PushPublisherConfig config = createPushPublisherConfigMock(
                testBundle, testCase.isDownloading, testCase.isStatic, testCase.operation);

        final DependencyModDateUtil dependencyModDateUtil = new DependencyModDateUtil(config);

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

        final Bundle bundle = new BundleDataGen().nextPersisted();

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        new PushedAssetDataGen()
                .assetId(contentlet.getIdentifier())
                .assetType(PusheableAsset.CONTENTLET.toString())
                .bundle(bundle)
                .publishingEndPoint(endPoint)
                .environment(environment_1)
                .pushDate(yesterday.getTime())
                .publisher(PushPublisher.class)
                .nextPersisted();

        final boolean excludeByModDate = dependencyModDateUtil.excludeByModDate(contentlet, PusheableAsset.CONTENTLET);
        assertFalse(excludeByModDate);
    }

    /**
     * Method to Test: {@link DependencyModDateUtil#excludeByModDate(Object)}}
     * When: A contentlet with a PushAsset with date after that moddate
     * Should: return false if isForcePush or isDownloading are true otherwise return true
     *
     * @param testCase
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("dataProviderForcePush")
    public void excludeByModDateContentWithPushAssetAfterModDate(final TestCase testCase) throws DotDataException {
        final Environment environment_1 = new EnvironmentDataGen().nextPersisted();
        final Environment environment_2 = new EnvironmentDataGen().nextPersisted();
        final PushPublishingEndPoint endPoint = new PushPublishingEndPointDataGen()
                .environment(environment_1)
                .nextPersisted();

        final Bundle testBundle = createTestBundle(testCase.isForcePush, Collections.singletonList(environment_1));

        PushPublisherConfig config = createPushPublisherConfigMock(
                testBundle, testCase.isDownloading, testCase.isStatic, testCase.operation);

        final DependencyModDateUtil dependencyModDateUtil = new DependencyModDateUtil(config);

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Contentlet contentlet =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(yesterday.getTime())
                .nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .downloading(testCase.isDownloading)
                .addAssets(Arrays.asList(contentlet))
                .operation(testCase.operation)
                .forcePush(testCase.isForcePush)
                .nextPersisted();

        new PushedAssetDataGen()
                .assetId(contentlet.getIdentifier())
                .assetType(PusheableAsset.CONTENTLET.toString())
                .bundle(bundle)
                .publishingEndPoint(endPoint)
                .environment(environment_1)
                .pushDate(new Date())
                .publisher(PushPublisher.class)
                .nextPersisted();

        final boolean excludeByModDate = dependencyModDateUtil.excludeByModDate(contentlet, PusheableAsset.CONTENTLET);

        if ( testCase.isForcePush || testCase.isDownloading ||
                testCase.operation != Operation.PUBLISH ) {

            assertFalse(excludeByModDate);
        } else {
            assertTrue(excludeByModDate);
        }
    }

    /**
     * Method to Test: {@link DependencyModDateUtil#excludeByModDate(Object)}}
     * When: A contentlet with two environment:
     * - One environment with a  PushAsset with date after that moddate
     * - One environment with a  PushAsset with date before that moddate
     *
     * Should: always return false
     *
     * @param testCase
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("dataProviderForcePush")
    public void excludeByModDateContentWithTwoEnvironment(final TestCase testCase) throws DotDataException {
        final Environment environment_1 = new EnvironmentDataGen().nextPersisted();
        final Environment environment_2 = new EnvironmentDataGen().nextPersisted();

        final PushPublishingEndPoint endPoint_1 = new PushPublishingEndPointDataGen()
                .environment(environment_1)
                .nextPersisted();

        final PushPublishingEndPoint endPoint_2 = new PushPublishingEndPointDataGen()
                .environment(environment_2)
                .nextPersisted();

        final Bundle testBundle = createTestBundle(testCase.isForcePush,
                list(environment_1, environment_2));

        PushPublisherConfig config = createPushPublisherConfigMock(
                testBundle, testCase.isDownloading, testCase.isStatic, testCase.operation);

        final DependencyModDateUtil dependencyModDateUtil = new DependencyModDateUtil(config);

        final Host host = new SiteDataGen().nextPersisted();
        final Language language = new UniqueLanguageDataGen().nextPersisted();

        final ContentType contentType =  new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Contentlet contentlet =  new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .host(host)
                .modeDate(yesterday.getTime())
                .nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .downloading(testCase.isDownloading)
                .addAssets(Arrays.asList(contentlet))
                .operation(testCase.operation)
                .forcePush(testCase.isForcePush)
                .nextPersisted();

        new PushedAssetDataGen()
                .assetId(contentlet.getIdentifier())
                .assetType(PusheableAsset.CONTENTLET.toString())
                .bundle(bundle)
                .publishingEndPoint(endPoint_1)
                .environment(environment_1)
                .pushDate(new Date())
                .publisher(PushPublisher.class)
                .nextPersisted();

        final Calendar beforeYesterday = Calendar.getInstance();
        beforeYesterday.add(Calendar.DATE, -2);

        new PushedAssetDataGen()
                .assetId(contentlet.getIdentifier())
                .assetType(PusheableAsset.CONTENTLET.toString())
                .bundle(bundle)
                .publishingEndPoint(endPoint_2)
                .environment(environment_2)
                .pushDate(beforeYesterday.getTime())
                .publisher(PushPublisher.class)
                .nextPersisted();

        final boolean excludeByModDate = dependencyModDateUtil.excludeByModDate(contentlet, PusheableAsset.CONTENTLET);

        assertFalse(excludeByModDate);
    }

    private PushPublisherConfig createPushPublisherConfigMock(Bundle testBundle,
            boolean isDownloading,
            boolean isStatic, Operation operation) {

        final PushPublisherConfig config = mock(PushPublisherConfig.class);
        when(config.getId()).thenReturn(testBundle.getId());
        when(config.isStatic()).thenReturn(isStatic);
        when(config.isDownloading()).thenReturn(isDownloading);
        when(config.getOperation()).thenReturn(operation);
        return config;
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

    private static class TestCase {
        boolean isDownloading;
        boolean isStatic;
        boolean isForcePush;
        Operation operation;

        public TestCase(final boolean isForcePush, final boolean isDownloading, final boolean isStatic,
                final Operation operation) {
            this.isForcePush = isForcePush;
            this.isDownloading = isDownloading;
            this.isStatic = isStatic;
            this.operation = operation;
        }
    }
}