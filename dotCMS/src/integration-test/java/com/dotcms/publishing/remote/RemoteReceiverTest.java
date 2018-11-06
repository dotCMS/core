package com.dotcms.publishing.remote;

import static com.dotcms.publisher.business.PublisherTestUtil.cleanBundleEndpointEnv;
import static com.dotcms.publisher.business.PublisherTestUtil.createEndpoint;
import static com.dotcms.publisher.business.PublisherTestUtil.createEnvironment;
import static com.dotcms.publisher.business.PublisherTestUtil.generateBundle;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.DM_WORKFLOW;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.felix.framework.OSGIUtil;
import org.apache.struts.Globals;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RemoteReceiverTest extends IntegrationTestBase {

    static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    static final String ADMIN_NAME = "User Admin";
    static final String REQUIRED_TEXT_FIELD_NAME = "required-field";

    private static Host host;
    private static Role adminRole;
    private static WorkflowAPI workflowAPI;
    private static RoleAPI roleAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static LanguageAPI languageAPI;
    private static HostAPI hostAPI;
    private static ContentletAPI contentletAPI;
    private static User systemUser;
    private static User adminUser;
    private static PublishingEndPointAPI endpointAPI;




    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //If I don't set this we'll end-up getting a ClassCasException.
        Mockito.when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                .thenReturn(new MultiMessageResources( MultiMessageResourcesFactory.createFactory(),""));

        OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);

        LicenseTestUtil.getLicense();

        workflowAPI = APILocator.getWorkflowAPI();
        contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        endpointAPI = APILocator.getPublisherEndPointAPI();

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);
        when(user.getLocale()).thenReturn(Locale.getDefault());

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource
                .init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
                        anyString())).thenReturn(dataObject);

        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        systemUser = APILocator.systemUser();

        adminUser = APILocator.getUserAPI()
                .loadByUserByEmail(ADMIN_DEFAULT_MAIL, systemUser, false);

        languageAPI = APILocator.getLanguageAPI();
        hostAPI = APILocator.getHostAPI();

        host = hostAPI.findDefaultHost(systemUser, false);
        adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        //Stopping the OSGI framework
        OSGIUtil.getInstance().stopFramework();
    }

    /**
     * Creates a custom contentType with a required field
     */
    private ContentType createSampleContentType() throws Exception {
        ContentType contentType;
        final String ctPrefix = "PPTestContentType";
        final String newContentTypeName = ctPrefix + System.currentTimeMillis();

        // Create ContentType
        contentType = BaseWorkflowIntegrationTest
                .createContentTypeAndAssignPermissions(newContentTypeName,
                        BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final WorkflowScheme documentWorkflow = workflowAPI
                .findSchemeByName(DM_WORKFLOW);

        // Add fields to the contentType
        final Field field =
                FieldBuilder.builder(TextField.class).name(REQUIRED_TEXT_FIELD_NAME)
                        .variable(REQUIRED_TEXT_FIELD_NAME)
                        .required(true)
                        .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build();
        contentType = contentTypeAPI.save(contentType, Collections.singletonList(field));

        // Assign contentType to Workflows
        workflowAPI.saveSchemeIdsForContentType(contentType,
                Arrays.asList(
                        systemWorkflow.getId(), documentWorkflow.getId()
                )
        );

        return contentType;
    }

    /**
     * Creates a contentlet based on the content type + Language passed
     */
    Contentlet createSampleContent(final ContentType contentType, final Language language)
            throws Exception {
        Contentlet contentlet = null;
        // Create a content sample
        contentlet = new Contentlet();
        // instruct the content with its own type
        contentlet.setStructureInode(contentType.inode());
        contentlet.setHost(host.getIdentifier());
        contentlet.setLanguageId(language.getId());

        contentlet.setStringProperty(REQUIRED_TEXT_FIELD_NAME, "anyValue");
        contentlet.setIndexPolicy(IndexPolicy.FORCE);

        // Save the content
        contentlet = contentletAPI.checkin(contentlet, systemUser, false);
        assertNotNull(contentlet.getInode());
        return contentlet;
    }

    private Language newLanguageInstance(final String languageCode, final String countryCode,
            final String language, final String country) {
        return new Language(0, languageCode, countryCode,
                language, country
        );
    }

    /**
     * This method test the deletePushedAssetsByEnvironment method
     */
    @Test
    public void test_create_languages_create_bundle_then_publish_then_read_languages()
            throws Exception {

        final List<Language> newLanguages = new ImmutableList.Builder<Language>().
                add(newLanguageInstance("eu", "", "Basque", "")).
                add(newLanguageInstance("hin", "hi", "Hindi", "India")).
                build();

        final List<String> assetIds = new ArrayList<>();

        final ContentType contentType = createSampleContentType();

        final List<Language> languages = new ArrayList<>();
        for (final Language language : newLanguages) {
            languageAPI.saveLanguage(language);
            languages.add(language);
        }

        final List<Contentlet> contentlets = new ArrayList<>();
        for (final Language language : languages) {
            final Contentlet contentlet = createSampleContent(contentType, language);
            assetIds.add(contentlet.getIdentifier());
            contentlets.add(contentlet);
        }

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final BundleAPI bundleAPI = APILocator.getBundleAPI();
        Environment environment = null;
        PublishingEndPoint endpoint = null;
        Bundle bundle = null;
        final User adminUser = APILocator.getUserAPI()
                .loadByUserByEmail(ADMIN_DEFAULT_MAIL, APILocator.getUserAPI().getSystemUser(),
                        false);

        File file = null;
        try {

            environment = createEnvironment(adminUser);
            endpoint = createEndpoint(environment);

            //Save the endpoint.
            bundle = new Bundle("testBundle", null, null, adminUser.getUserId());
            bundleAPI.saveBundle(bundle);

            publisherAPI.saveBundleAssets(assetIds, bundle.getId(), adminUser);

            final Map<String, Object> bundleData = generateBundle(bundle.getId(),
                    Operation.PUBLISH);
            assertNotNull(bundleData);
            assertNotNull(bundleData.get("file"));
            file = File.class.cast(bundleData.get("file"));
            final String fileName = file.getName();

            final String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));

            PublishAuditStatus status = PublishAuditAPI
                    .getInstance()
                    .updateAuditTable(endpoint.getId(), endpoint.getGroupId(), bundleFolder, true);

            final PublisherConfig publisherConfig = new PublisherConfig();
            final BundlePublisher bundlePublisher = new BundlePublisher();
            publisherConfig.setId(fileName);
            publisherConfig.setEndpoint(endpoint.getId());
            publisherConfig.setGroupId(endpoint.getGroupId());
            publisherConfig.setPublishAuditStatus(status);

            bundlePublisher.init(publisherConfig);
            bundlePublisher.process(null);

            //extract and Test Results..

            for (final Language language : languages) {
                assertNotNull(languageAPI
                        .getLanguage(language.getLanguageCode(), language.getCountryCode())
                );
            }



        } finally {

            for (final Contentlet contentlet : contentlets) {
                contentletAPI.archive(contentlet, adminUser, false);
                contentletAPI.delete(contentlet, adminUser, false);
            }

            for (final Language language : languages) {
                languageAPI.deleteLanguage(language);
            }

            if (null != bundle && null != endpoint && null != environment) {
                cleanBundleEndpointEnv(bundle, endpoint, environment);
            }

            if (null != file) {
                file.delete();
            }
        }
    }


    @Test
    public void test_create_dupe_languages_create_bundle_then_publish_then_read_languages_verify_dupes_are_ignored()
            throws Exception {

            //We assume these languages already exist on the db
        final List<Language> dupeLanguages = new ImmutableList.Builder<Language>().
                add(newLanguageInstance("en", "US", "English", "United States")).
                add(newLanguageInstance("es", "ES", "Espanol", "Espana")).
                build();


        final List<Language> savedDupeLanguages = new ArrayList<>();
        for (final Language language : dupeLanguages) {
            languageAPI.saveLanguage(language);
            savedDupeLanguages.add(language);
        }

        final List<String> assetIds = new ArrayList<>();
        final ContentType contentType = createSampleContentType();
        final List<Contentlet> contentlets = new ArrayList<>();
        for (final Language dupeLanguage : savedDupeLanguages) {
            final Contentlet contentlet = createSampleContent(contentType, dupeLanguage);
            assetIds.add(contentlet.getIdentifier());
            contentlets.add(contentlet);
        }

        List<Contentlet> publishedContentlets = Collections.emptyList();

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final BundleAPI bundleAPI = APILocator.getBundleAPI();
        Environment environment = null;
        PublishingEndPoint endpoint = null;
        Bundle bundle = null;
        final User adminUser = APILocator.getUserAPI()
                .loadByUserByEmail(ADMIN_DEFAULT_MAIL, APILocator.getUserAPI().getSystemUser(),
                        false);
        File file = null;
        try{

            environment = createEnvironment(adminUser);
            endpoint = createEndpoint(environment);

            //Save the endpoint.
            bundle = new Bundle("testBundle", null, null, adminUser.getUserId());
            bundleAPI.saveBundle(bundle);

            publisherAPI.saveBundleAssets(assetIds, bundle.getId(), adminUser);

            final Map<String, Object> bundleData = generateBundle(bundle.getId(), Operation.PUBLISH);
            assertNotNull(bundleData);
            assertNotNull(bundleData.get("file"));
            file = File.class.cast(bundleData.get("file"));
            final String fileName = file.getName();

            final String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));

            PublishAuditStatus status = PublishAuditAPI
                    .getInstance()
                    .updateAuditTable(endpoint.getId(), endpoint.getGroupId(), bundleFolder, true);

            // Remove contentlets so they can be regenerated from the bundle
            for (final Contentlet contentlet : contentlets) {
                contentletAPI.archive(contentlet, adminUser, false);
                contentletAPI.delete(contentlet, adminUser, false);
            }

            // We have now added dupe Languages.
            // Now we need to flush cache so the next time the pp process asks for a Language
            // it will get the first lang and not the one stored on cache during the bundle generation process.
            APILocator.getLanguageAPI().clearCache();

            final PublisherConfig publisherConfig = new PublisherConfig();
            final BundlePublisher bundlePublisher = new BundlePublisher();
            publisherConfig.setId(fileName);
            publisherConfig.setEndpoint(endpoint.getId());
            publisherConfig.setGroupId(endpoint.getGroupId());
            publisherConfig.setPublishAuditStatus(status);

            bundlePublisher.init(publisherConfig);
            bundlePublisher.process(null);

           // indexNeedsToCatchup();

            //Should have used only the language there came by default. Meaning the dupes should have been ignored.
            publishedContentlets = contentletAPI.findByStructure(contentType.inode(), adminUser, false,10, 0);
            assertEquals("We expected 2 instances of our custom type ", 2, publishedContentlets.size());

            final Map<Long, List<Contentlet>> contentsByLanguage = publishedContentlets.stream().collect(Collectors.groupingBy(Contentlet::getLanguageId));

            for(final Language language : savedDupeLanguages){
                assertFalse("None of the dupe langs should have been used.", contentsByLanguage.containsKey(language.getId()));
            }

        } finally {

            // Remove contentlets pushed
            for (final Contentlet contentlet : publishedContentlets) {
                contentletAPI.archive(contentlet, adminUser, false);
                contentletAPI.delete(contentlet, adminUser, false);
            }

            //Cleanup pushed langs
            for (final Language language : savedDupeLanguages) {
                languageAPI.deleteLanguage(language);
            }

            if (null != bundle && null != endpoint && null != environment) {
                cleanBundleEndpointEnv(bundle, endpoint, environment);
            }

            if (null != file) {
                file.delete();
            }

        }

    }



}
