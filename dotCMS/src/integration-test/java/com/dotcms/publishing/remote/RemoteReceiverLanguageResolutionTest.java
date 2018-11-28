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
import static org.junit.Assert.assertTrue;
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
import com.dotcms.publisher.business.PublisherTestUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.felix.framework.OSGIUtil;
import org.apache.struts.Globals;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RemoteReceiverLanguageResolutionTest extends IntegrationTestBase {

    static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    static final String ADMIN_NAME = "User Admin";
    static final String REQUIRED_TEXT_FIELD_NAME = "lang-name-field";

    static final String FILE = PublisherTestUtil.FILE;

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


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //If I don't set this we'll end-up getting a ClassCasException at LanguageVariablesHandler.java:75
        when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                .thenReturn(new MultiMessageResources( MultiMessageResourcesFactory.createFactory(),""));

        OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);

        LicenseTestUtil.getLicense();

        workflowAPI = APILocator.getWorkflowAPI();
        contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();

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
    private Contentlet createSampleContent(final ContentType contentType, final Language language)
            throws Exception {
        // Create a content sample
        Contentlet contentlet = new Contentlet();
        // instruct the content with its own type
        contentlet.setStructureInode(contentType.inode());
        contentlet.setHost(host.getIdentifier());
        contentlet.setLanguageId(language.getId());
        //Now lets add some lang info to the instance itself.
        contentlet.setStringProperty(REQUIRED_TEXT_FIELD_NAME, language.toString());
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
     * This method test method simply tests that languages are pushed-published
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

        final List<Language> languages = new ArrayList<>(3);

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
            assertNotNull(bundleData.get(FILE));
            file = File.class.cast(bundleData.get(FILE));
            final String fileName = file.getName();

            final String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));

            final PublishAuditStatus status = PublishAuditAPI
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

            if(null != contentType){
            }  contentTypeAPI.delete(contentType);

            if (null != bundle && null != endpoint && null != environment) {
                cleanBundleEndpointEnv(bundle, endpoint, environment);
            }

            if (null != file) {
                file.delete();
            }
        }
    }


    @Test
    public void test_create_dupe_languages_create_bundle_then_publish_bundle_then_read_languages_verify_dupes_are_ignored()
            throws Exception {

        //We assume these languages already exist in the db
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
            assertNotNull(bundleData.get(FILE));
            file = File.class.cast(bundleData.get(FILE));
            final String fileName = file.getName();

            final String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));

            final PublishAuditStatus status = PublishAuditAPI
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

            // Now lets push-publish
            final PublisherConfig publisherConfig = new PublisherConfig();
            final BundlePublisher bundlePublisher = new BundlePublisher();
            publisherConfig.setId(fileName);
            publisherConfig.setEndpoint(endpoint.getId());
            publisherConfig.setGroupId(endpoint.getGroupId());
            publisherConfig.setPublishAuditStatus(status);

            bundlePublisher.init(publisherConfig);
            bundlePublisher.process(null);
            //Push publish ends here.

            //Now we check the results
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

            if(null != contentType){
            }  contentTypeAPI.delete(contentType);


            if (null != bundle && null != endpoint && null != environment) {
                cleanBundleEndpointEnv(bundle, endpoint, environment);
            }

            if (null != file) {
                file.delete();
            }

        }

    }


    @Test
    public void test_create_new_languages_create_bundle_then_publish_bundle_then_read_verify_dupes_are_ignored_and_new_languges_were_created()
            throws Exception {

        final List<Language> newLanguages = new ImmutableList.Builder<Language>().
                add(newLanguageInstance("ep", "", "Esperanto", "")).
                add(newLanguageInstance("de", "DE", "German", "Germany")).
                add(newLanguageInstance("ru", "RUS", "Russian", "Russia")).
                add(newLanguageInstance("da", "DK ", "Danish", "Denmark")).
                add(newLanguageInstance("en", "NZ ", "English", "New Zealand")).
                build();

        final List<Language> savedNewLanguages = new ArrayList<>();
        for (final Language language : newLanguages) {
            languageAPI.saveLanguage(language);
            savedNewLanguages.add(language);
        }

        final List<String> assetIds = new ArrayList<>();
        final ContentType contentType = createSampleContentType();
        final List<Contentlet> contentlets = new ArrayList<>();
        for (final Language dupeLanguage : savedNewLanguages) {
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
            assertNotNull(bundleData.get(FILE));
            file = File.class.cast(bundleData.get(FILE));
            final String fileName = file.getName();

            final String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));

            final PublishAuditStatus status = PublishAuditAPI
                    .getInstance()
                    .updateAuditTable(endpoint.getId(), endpoint.getGroupId(), bundleFolder, true);

            // Remove contentlets so they can be regenerated from the bundle
            for (final Contentlet contentlet : contentlets) {
                contentletAPI.archive(contentlet, adminUser, false);
                contentletAPI.delete(contentlet, adminUser, false);
            }

            final List<Long>savedLanguagesNowDeletedIds = new ArrayList<>();
            // Remove all the languages we just created from the db.. see if they get re-generated out of the push-publish process.
            for (final Language language : savedNewLanguages) {
                savedLanguagesNowDeletedIds.add(language.getId());
                languageAPI.deleteLanguage(language);
            }

            // Now we need to flush cache so the next time the pp process asks for a Language
            // it will get the first lang and not the one stored in cache during the bundle generation process.
            APILocator.getLanguageAPI().clearCache();

            //Now recreate a few languages to simulate a conflict on the receiver. They will be re-inserted under a different id. so that creates a clash.
            //Now the contentlets generated out of pp should come with these codes.
            final List<Language> reinsertLanguages = new ImmutableList.Builder<Language>().
                    add(newLanguageInstance("ep", "", "Esperanto", "")).
                    add(newLanguageInstance("de", "DE", "German", "Germany")).
                    build();

            final List<Language> savedReinsertedLanguages = new ArrayList<>();
            for (final Language language : reinsertLanguages) {
                languageAPI.saveLanguage(language);
                savedReinsertedLanguages.add(language);
            }
            // We have now added dupe Languages.
            // Organize new dupe languages as a map.
            final Map<Long,Language> reInsertedLanguagesMap = savedReinsertedLanguages.stream().collect(
                    Collectors.toMap(Language::getId, Function.identity())
            );

            final Comparator<Language> comparator = Comparator.comparing( Language::getId );

            // Now lets do push-publish.
            final PublisherConfig publisherConfig = new PublisherConfig();
            final BundlePublisher bundlePublisher = new BundlePublisher();
            publisherConfig.setId(fileName);
            publisherConfig.setEndpoint(endpoint.getId());
            publisherConfig.setGroupId(endpoint.getGroupId());
            publisherConfig.setPublishAuditStatus(status);

            bundlePublisher.init(publisherConfig);
            bundlePublisher.process(null);
            //Push publish ends here.

            //Now we check the results.
            //Should have used only the language that came by default. Meaning the dupes should have been ignored.
            publishedContentlets = contentletAPI.findByStructure(contentType.inode(), adminUser, false,10, 0);
            assertEquals("We expected 5 instances of our custom type ", 5, publishedContentlets.size());

            printContentletLanguageInfo(publishedContentlets);

            //Count number of instances already using an existing language.

            final long contentletsWithReusedLanguages = publishedContentlets.stream().filter(contentlet -> reInsertedLanguagesMap.containsKey(contentlet.getLanguageId())).count();

            assertEquals("We expected 2 languages already existing languages to be re-used.",2, contentletsWithReusedLanguages);

            //Now check the languages now restored.  They should match the ones we previously deleted.
            final long contentletsWithNewLanguages = publishedContentlets.stream().filter(contentlet ->  savedLanguagesNowDeletedIds.contains(contentlet.getLanguageId())).count();

            assertEquals("We expected 3 new languages created after pp.",3, contentletsWithNewLanguages);

            //Expected language codes. These are created by PP
            final Set<String> expectedNewLangs = new HashSet<>(Arrays.asList("ru","da","en")); //Should have been created with the original ids that they originally had.
            final boolean newLanguagesMatch = publishedContentlets.stream().
              filter(contentlet -> savedLanguagesNowDeletedIds.contains(contentlet.getLanguageId())).allMatch(contentlet ->  {
                final Language lang = languageAPI.getLanguage(contentlet.getLanguageId());
                return expectedNewLangs.contains(lang.getLanguageCode());
              } );

            assertTrue("new Languages created are not a match.", newLanguagesMatch);

        } finally {

            // Remove contentlets pushed
            for (final Contentlet contentlet : publishedContentlets) {
                contentletAPI.archive(contentlet, adminUser, false);
                contentletAPI.delete(contentlet, adminUser, false);
            }

            for (final Language language : savedNewLanguages) {
                final Language persistedLang = languageAPI.getLanguage(language.getLanguageCode(), language.getCountryCode());
                if(UtilMethods.isSet(persistedLang) && persistedLang.getId() > 0 ){
                    languageAPI.deleteLanguage(persistedLang);
                }
            }

            if(null != contentType){
            }  contentTypeAPI.delete(contentType);


            if (null != bundle && null != endpoint && null != environment) {
                cleanBundleEndpointEnv(bundle, endpoint, environment);
            }

            if (null != file) {
                file.delete();
            }

        }

    }


    private void printContentletLanguageInfo(final List<Contentlet> publishedContentlets) {
        publishedContentlets.forEach(contentlet -> {
            Logger.info(RemoteReceiverLanguageResolutionTest.class,
                    () -> "id: " + contentlet.getLanguageId() + " - " + contentlet
                            .get(REQUIRED_TEXT_FIELD_NAME)
            );
        });
    }

}
