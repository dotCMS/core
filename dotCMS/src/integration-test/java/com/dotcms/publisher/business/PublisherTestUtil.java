package com.dotcms.publisher.business;

import static java.lang.String.valueOf;
import static org.junit.Assert.assertEquals;

import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.enterprise.publishing.PublishDateUpdater;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class PublisherTestUtil {

    private final static String PROTOCOL = "http";

    /**
     * Creates a generic TestEnvironment
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static Environment createEnvironment (final User user) throws DotDataException, DotSecurityException {

        final EnvironmentAPI environmentAPI               = APILocator.getEnvironmentAPI();
        final Environment      environment1               = new Environment();
        final List<Permission> permissions1               = new ArrayList<>();
        permissions1.add(new Permission(environment1.getId(),
                APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId(), PermissionAPI.PERMISSION_USE));

        environment1.setName("TestEnvironment_" + valueOf(new Date().getTime()));
        environment1.setPushToAll(false);
        environmentAPI.saveEnvironment(environment1, permissions1);

        return environment1;
    } // createEnvironment

    /**
     * Creates a generic TestEndpoint
     * @param environment
     * @return
     * @throws DotDataException
     */
    public static PublishingEndPoint createEndpoint (final Environment environment) throws DotDataException {

        final PublishingEndPointAPI publisherEndPointAPI  = APILocator.getPublisherEndPointAPI();
        final PublishingEndPointFactory factory = new PublishingEndPointFactory();
        final PublishingEndPoint endpoint1 = factory.getPublishingEndPoint(PROTOCOL);
        endpoint1.setServerName( new StringBuilder( "TestEndPoint" + valueOf( new Date().getTime() ) ) );
        endpoint1.setAddress( "127.0.0.1" );
        endpoint1.setPort( "999" );
        endpoint1.setProtocol(PROTOCOL);
        endpoint1.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
        endpoint1.setEnabled( true );
        endpoint1.setSending( false );
        endpoint1.setGroupId( environment.getId() );

        publisherEndPointAPI.saveEndPoint( endpoint1 );

        return endpoint1;
    } // createEndpoint

    /**
     * Deletes the bundle, endpoitn and environment, specially util at the end of a PP process for testing
     * @param bundle1
     * @param endpoint1
     * @param environment1
     */
    public static void cleanBundleEndpointEnv (final Bundle bundle1, final PublishingEndPoint endpoint1,
                                               final Environment environment1) {

        final BundleAPI bundleAPI                         = APILocator.getBundleAPI();
        final PublishingEndPointAPI publisherEndPointAPI  = APILocator.getPublisherEndPointAPI();
        final EnvironmentAPI   environmentAPI             = APILocator.getEnvironmentAPI();

        try {
            if (null != bundle1) {

                bundleAPI.deleteBundle(bundle1.getId());
            }

        } catch (Exception e) { e.printStackTrace(); } finally {

            try {
                if (null != endpoint1) {

                    publisherEndPointAPI.deleteEndPointById(endpoint1.getId());
                }

            } catch (Exception e) { e.printStackTrace();  } finally {

                if (null != environment1) {
                    try {
                        environmentAPI.deleteEnvironment(environment1.getId());
                    } catch (DotDataException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    } // cleanBundleEndpointEnv.

    /**
     * Creates a folder generic based on a given name
     * @param testfolder
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static  Folder createFolder(final String testfolder) throws DotDataException, DotSecurityException {

        final User sysuser = APILocator.getUserAPI().getSystemUser();
        final Host host    = APILocator.getHostAPI().findDefaultHost(sysuser, false);

        return APILocator.getFolderAPI().createFolders(
                testfolder + UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);

    }

    /**
     * Creates and saves bundle
     * @param bundleName
     * @param user
     * @return
     * @throws DotDataException
     */
    public static Bundle createBundle (final String bundleName, final User user, final Environment environment)
            throws DotDataException {

        final BundleAPI  bundleAPI         = APILocator.getBundleAPI();
        final Bundle     bundle1           = new Bundle(bundleName, null, null, user.getUserId());

        bundleAPI.saveBundle(bundle1);
        bundleAPI.saveBundleEnvironment(bundle1, environment);
        return bundle1;
    }

    /**
     * Creates a template, and a generic page based on it, saving the info in a given folder.
     * @param folder
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static HTMLPageAsset createPage (final Folder folder, final User user) throws DotDataException, DotSecurityException {

        final User sysuser      = APILocator.getUserAPI().getSystemUser();
        final Host host         = APILocator.getHostAPI().findDefaultHost(sysuser, false);
        Template template       = new Template();
        template.setTitle("a template "+UUIDGenerator.generateUuid());
        template. setBody("<html><body> I'm mostly empty </body></html>");
        template                = APILocator.getTemplateAPI().saveTemplate(template, host, sysuser, false);

        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(1).nextPersisted();

        final HTMLPageAsset pageAsset = (HTMLPageAsset)APILocator.getHTMLPageAssetAPI().findPage(page.getInode(), sysuser, false);

        APILocator.getContentletAPI().publish(pageAsset, user, false);
        APILocator.getContentletIndexAPI().addContentToIndex(pageAsset);

        APILocator.getContentletAPI().isInodeIndexed(pageAsset.getInode());
        assertEquals(1, APILocator.getContentletAPI().indexCount("+inode:" + pageAsset.getInode(), user, false));

        return pageAsset;
    }

    /**
     * Get the assets to push based on a set of contentlet
     * @param bundle
     * @param contentlets
     * @return
     */
    public static List<PublishQueueElement> getAssets(final Bundle bundle, final Contentlet... contentlets) {

        final List<PublishQueueElement> queueElements = new ArrayList<>();

        for (final Contentlet contentlet: contentlets) {

            queueElements.add(getAsset(bundle, contentlet));
        }

        return queueElements;
    }

    /**
     * Get the assets to push based on a set of folders
     * @param bundle
     * @param folders
     * @return
     */
    public static List<PublishQueueElement> getAssets(final Bundle bundle, final Folder... folders) {

        final List<PublishQueueElement> queueElements = new ArrayList<>();

        for (final Folder folder: folders) {

            queueElements.add(getAsset(bundle, folder));
        }

        return queueElements;
    }

    /**
     * Push a list of assets into a bundle
     * @param assets
     * @param bundle
     * @throws DotPublisherException
     * @throws DotDataException
     */
    public static PublishStatus push(final List<PublishQueueElement> assets, final Bundle bundle, final User user)
            throws DotPublisherException, DotDataException, IllegalAccessException, InstantiationException, DotSecurityException {

        final PublishAuditStatus  status              = new PublishAuditStatus(bundle.getId());
        final PublisherConfig     pushPublisherConfig = new PushPublisherConfig();
        final PublishAuditHistory historyPojo         = new PublishAuditHistory();
        final PublisherConfig.DeliveryStrategy deliveryStrategy =
                PublisherConfig.DeliveryStrategy.ALL_ENDPOINTS;

        PublishDateUpdater.updatePublishExpireDates(new Date());
        pushPublisherConfig.setAssets(assets);

        audit(assets, status, historyPojo);
        return pushPublish(preparePushConfiguration(assets, bundle, pushPublisherConfig, deliveryStrategy, PushPublisherConfig.Operation.PUBLISH), historyPojo);
    }

    /**
     * Push a remote remove
     * @param assets
     * @param bundle
     * @param user
     * @return
     */
    public static PublishStatus remoteRemove(final List<PublishQueueElement> assets, final Bundle bundle, final User user)
            throws DotPublisherException, DotDataException, IllegalAccessException, InstantiationException, DotSecurityException {

        final PublishAuditStatus  status              = new PublishAuditStatus(bundle.getId());
        final PublisherConfig     pushPublisherConfig = new PushPublisherConfig();
        final PublishAuditHistory historyPojo         = new PublishAuditHistory();
        final PublisherConfig.DeliveryStrategy deliveryStrategy =
                PublisherConfig.DeliveryStrategy.ALL_ENDPOINTS;

        PublishDateUpdater.updatePublishExpireDates(new Date());
        pushPublisherConfig.setAssets(assets);

        audit(assets, status, historyPojo);
        return pushPublish(preparePushConfiguration(assets, bundle, pushPublisherConfig, deliveryStrategy, PushPublisherConfig.Operation.UNPUBLISH), historyPojo);
    }

    private static PublishQueueElement getAsset(final Bundle bundle, final Contentlet contentlet) {

        final PublishQueueElement element = new PublishQueueElement();

        element.setId((int)System.currentTimeMillis());
        element.setAsset(contentlet.getIdentifier());
        element.setBundleId(bundle.getId());
        element.setEnteredDate(new Date());
        element.setPublishDate(new Date());
        element.setType(PusheableAsset.CONTENTLET.getType());

        return element;
    }

    private static PublishQueueElement getAsset(final Bundle bundle, final Folder folder) {

        final PublishQueueElement element = new PublishQueueElement();

        element.setId((int)System.currentTimeMillis());
        element.setAsset(folder.getIdentifier());
        element.setBundleId(bundle.getId());
        element.setEnteredDate(new Date());
        element.setPublishDate(new Date());
        element.setType(PusheableAsset.FOLDER.getType());

        return element;
    }



    private static PublishStatus pushPublish(final PublisherConfig publisherConfig, final PublishAuditHistory historyPojo) throws DotPublisherException {

        final PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();
        final PublisherAPI    publisherAPI    = PublisherAPI.getInstance();
        PublishStatus         publishStatus   = null;

        try {

            publishStatus = APILocator.getPublisherAPI().publish(publisherConfig);
        } catch (DotPublishingException e) {

            publishAuditAPI.updatePublishAuditStatus(publisherConfig.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, historyPojo);
            publisherAPI.deleteElementsFromPublishQueueTable(publisherConfig.getId());
        }

        return publishStatus;
    }

    private static PublisherConfig preparePushConfiguration(final List<PublishQueueElement> assets,
                                                            final Bundle bundle,
                                                            final PublisherConfig publisherConfig,
                                                            final PublisherConfig.DeliveryStrategy deliveryStrategy,
                                                            final PublisherConfig.Operation operation) throws DotDataException, InstantiationException, IllegalAccessException {

        publisherConfig.setLuceneQueries(PublisherUtil.prepareQueries(assets));
        publisherConfig.setId(bundle.getId());
        publisherConfig.setUser(APILocator.getUserAPI().getSystemUser());
        publisherConfig.setStartDate(new Date());
        publisherConfig.runNow();
        publisherConfig.setPublishers(Arrays.asList(TestPushPublisher.class)); // we use our test PP, since we do not really do a http request
        publisherConfig.setDeliveryStrategy(deliveryStrategy);
        publisherConfig.setOperation(operation);

        return setUpConfigForPublisher(publisherConfig);
    }

    private static PublisherConfig setUpConfigForPublisher(PublisherConfig pconf)
            throws IllegalAccessException, InstantiationException {

        final List<Class> publishers = pconf.getPublishers();
        for (Class<?> publisher : publishers) {
            pconf = ((Publisher)publisher.newInstance()).setUpConfig(pconf);
        }

        return pconf;
    }

    private static void audit(final List<PublishQueueElement> assets,
                              final PublishAuditStatus status, final PublishAuditHistory historyPojo) throws DotPublisherException {

        final PublishAuditAPI     publishAuditAPI     = PublishAuditAPI.getInstance();

        historyPojo.setAssets(assets.stream().collect(Collectors.toMap(asset-> asset.getAsset(), asset-> asset.getType())));
        status.setStatusPojo(historyPojo);

        //Insert in Audit table
        publishAuditAPI.insertPublishAuditStatus(status);
    }

    public static Optional<BundlerStatus> getBundleStatus(final List<BundlerStatus> bundlerStatuses, final Class<? extends IBundler> contentBundlerClass) {

        return bundlerStatuses.stream().filter(status -> status.getBundlerClass().equals(contentBundlerClass.getName())).findFirst();
    }

    public static boolean existsFolder(final String bundlePath, final Folder folder) {

        final File folderTestPath   = new File(bundlePath, "/ROOT/" + folder.getName());
        final File folderIdTestPath = new File(bundlePath, "/ROOT/" + folder.getIdentifier() + ".folder.xml");

        return folderTestPath.exists() && folderIdTestPath.exists();
    }

    public static boolean existsPage(final String bundlePath, final Host host, final int countOrder,
                                     final Folder folder, final HTMLPageAsset page) {

        final File pageTestPath   = new File(bundlePath,
                "/live/" + host.getHostname() + "/" + page.getLanguageId() + "/" +
                        folder.getName() + "/" + countOrder + "-" + page.getPageUrl() + ".content.xml");

        boolean exists = pageTestPath.exists();
        Logger.info(PublisherTestUtil.class,
                pageTestPath.getAbsolutePath() + " - Exists [" + exists + "]");

        return exists;
    }



}
