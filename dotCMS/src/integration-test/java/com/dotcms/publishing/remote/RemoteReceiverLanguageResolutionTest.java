package com.dotcms.publishing.remote;

import static com.dotcms.datagen.TestDataUtils.getSpanishLanguage;
import static com.dotcms.datagen.TestDataUtils.getWikiLikeContentType;
import static com.dotcms.publisher.business.PublisherTestUtil.createEndpoint;
import static com.dotcms.publisher.business.PublisherTestUtil.createEnvironment;
import static com.dotcms.publisher.business.PublisherTestUtil.generateBundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
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
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class RemoteReceiverLanguageResolutionTest extends IntegrationTestBase {

    static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    static final String ADMIN_NAME = "User Admin";

    static final String FILE = PublisherTestUtil.FILE;

    private static Host host;
    private static LanguageAPI languageAPI;
    private static ContentletAPI contentletAPI;
    private static User adminUser;

    private static final String languagesSuffix = RandomStringUtils
            .random(3, true, true).toLowerCase();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //If I don't set this we'll end-up getting a ClassCasException at LanguageVariablesHandler.java:75
        when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                .thenReturn(new MultiMessageResources( MultiMessageResourcesFactory.createFactory(),""));

        LicenseTestUtil.getLicense();

        contentletAPI = APILocator.getContentletAPI();

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);
        when(user.getLocale()).thenReturn(Locale.getDefault());

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource
                .init(anyString(), any(HttpServletRequest.class),  any(HttpServletResponse.class), anyBoolean(),
                        anyString())).thenReturn(dataObject);

        languageAPI = APILocator.getLanguageAPI();

        host = new SiteDataGen().nextPersisted();

        adminUser = TestUserUtils.getAdminUser();

        //Make sure some default Languages exist
        getSpanishLanguage();

    }

    private Language newLanguageInstance(final String languageCode, final String countryCode,
            final String language, final String country) {
        return new Language(0, languageCode, countryCode,
                language, country
        );
    }

    /**
     * This method simply tests that languages are pushed-published
     */

    @Test
    public void test_create_languages_create_bundle_then_publish_then_read_languages()
            throws Exception {

        // Any CT should do it.
        ContentType contentType = getWikiLikeContentType();

        final List<Language> newLanguages = new ImmutableList.Builder<Language>().
                add(newLanguageInstance("eu" + languagesSuffix, "", "Basque", "")).
                add(newLanguageInstance("hi" + languagesSuffix, "hi", "Hindi", "India")).
                build();

        final List<String> assetIds = new ArrayList<>();
        final List<Language> languages = new ArrayList<>(3);

        for (final Language language : newLanguages) {
            languageAPI.saveLanguage(language);
            languages.add(language);
        }

        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());

        final List<Contentlet> contentlets = new ArrayList<>();
        for (final Language language : languages) {
            final Contentlet contentlet = contentletDataGen.host(host).setProperty("title","lang is "+language.toString()).languageId(language.getId()).nextPersisted();
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

            //extract and Test Results...

            for (final Language language : languages) {
                assertNotNull(languageAPI
                        .getLanguage(language.getLanguageCode(), language.getCountryCode())
                );
            }



        } finally {

            if (null != file) {
                try {
                    file.delete();
                } catch (Exception e) {
                    //Do nothing...
                }
            }
        }
    }


    @Test
    public void test_create_dupe_languages_create_bundle_then_publish_bundle_then_read_languages_verify_dupes_are_ignored()
            throws Exception {

        // Any CT should do it.
        ContentType contentType = getWikiLikeContentType();

        //We assume these languages already exist in the db
        final List<Language> dupeLanguages = new ImmutableList.Builder<Language>().
                add(newLanguageInstance("en", "US", "English", "United States")).
                add(newLanguageInstance("es", "ES", "Spanish", "Spain")).
                build();

        final List<Language> savedDupeLanguages = new ArrayList<>();
        for (final Language language : dupeLanguages) {
            languageAPI.saveLanguage(language);
            savedDupeLanguages.add(language);
        }

        final List<String> assetIds = new ArrayList<>();
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        final List<Contentlet> contentlets = new ArrayList<>();
        for (final Language dupeLanguage : savedDupeLanguages) {
            final Contentlet contentlet = contentletDataGen.host(host).setProperty("title","lang is "+dupeLanguage.toString()).languageId(dupeLanguage.getId()).nextPersisted();
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

            LocalTransaction.wrap(() -> {
                // Remove contentlets so they can be regenerated from the bundle
                for (final Contentlet contentlet : contentlets) {
                    contentlet.setIndexPolicy(IndexPolicy.FORCE);
                    contentletAPI.destroy(contentlet, adminUser, false);
                }
            });

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
            //Should have used only the language there that came by default. Meaning the dupes should have been ignored.
            publishedContentlets = contentletAPI.findByStructure(contentType.inode(), adminUser, false,10, 0);
            assertEquals("We expected 2 instances of our custom type ", 2, publishedContentlets.size());

            final Map<Long, List<Contentlet>> contentsByLanguage = publishedContentlets.stream().collect(Collectors.groupingBy(Contentlet::getLanguageId));

            for(final Language language : savedDupeLanguages){
                assertFalse("None of the dupe langs should have been used.", contentsByLanguage.containsKey(language.getId()));
            }

        } finally {

            if (null != file) {
                try {
                    file.delete();
                } catch (Exception e) {
                    //Do nothing...
                }
            }

        }

    }

    @Test
    public void test_create_new_languages_create_bundle_then_publish_bundle_then_read_verify_dupes_are_ignored_and_new_languges_were_created()
            throws Exception {

        // Any CT should do it.
        ContentType contentType = getWikiLikeContentType();

        final List<Language> newLanguages = new ImmutableList.Builder<Language>().
                add(newLanguageInstance("ep" + languagesSuffix, "", "Esperanto", "")).
                add(newLanguageInstance("de" + languagesSuffix, "DE", "German", "Germany")).
                add(newLanguageInstance("ru" + languagesSuffix, "RUS", "Russian", "Russia")).
                add(newLanguageInstance("da" + languagesSuffix, "DK ", "Danish", "Denmark")).
                add(newLanguageInstance("en" + languagesSuffix, "NZ ", "English", "New Zealand")).
                build();

        final List<Language> savedNewLanguages = new ArrayList<>();
        for (final Language language : newLanguages) {
            languageAPI.saveLanguage(language);
            savedNewLanguages.add(language);
        }

        final List<String> assetIds = new ArrayList<>();
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        final List<Contentlet> contentlets = new ArrayList<>();
        for (final Language dupeLanguage : savedNewLanguages) {
            final Contentlet contentlet = contentletDataGen.host(host)
                    .setProperty("title", "lang is " + dupeLanguage.toString())
                    .languageId(dupeLanguage.getId()).nextPersisted();
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

            final List<Long> savedLanguagesNowDeletedIds = LocalTransaction.wrapReturn(() -> {

                // Remove contentlets so they can be regenerated from the bundle
                for (final Contentlet contentlet : contentlets) {
                    contentletAPI.destroy(contentlet, adminUser, false);
                }

                final List<Long> internalSavedLanguagesNowDeletedIds = new ArrayList<>();
                // Remove all the languages we just created from the db..., see if they get re-generated out of the push-publish process.
                for (final Language language : savedNewLanguages) {
                    languageAPI.deleteLanguage(language);

                    //The language should be already deleted
                    assertNull(languageAPI.getLanguage(language.getId()));
                    internalSavedLanguagesNowDeletedIds.add(language.getId());
                }

                return internalSavedLanguagesNowDeletedIds;
            });

            // Now we need to flush cache so the next time the pp process asks for a Language
            // it will get the first lang and not the one stored in cache during the bundle generation process.
            APILocator.getLanguageAPI().clearCache();

            //Now recreate a few languages to simulate a conflict on the receiver. They will be re-inserted under a different id. so that creates a clash.
            //Now the contentlets generated out of pp should come with these codes.
            final List<Language> reinsertLanguages = new ImmutableList.Builder<Language>().
                    add(newLanguageInstance("ep" + languagesSuffix, "", "Esperanto", "")).
                    add(newLanguageInstance("de" + languagesSuffix, "DE", "German", "Germany")).
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
            assertEquals("We expected 5 instances of our content type ", 5, publishedContentlets.size());

            printContentletLanguageInfo(publishedContentlets);

            //Count number of instances already using an existing language.

            final long contentletsWithReusedLanguages = publishedContentlets.stream().filter(contentlet -> reInsertedLanguagesMap.containsKey(contentlet.getLanguageId())).count();

            assertEquals("We expected 2 languages already existing languages to be re-used.",2, contentletsWithReusedLanguages);

            //Now check the languages now restored.  They should match the ones we previously deleted.
            final long contentletsWithNewLanguages = publishedContentlets.stream().filter(contentlet ->  savedLanguagesNowDeletedIds.contains(contentlet.getLanguageId())).count();

            assertEquals("We expected 3 new languages created after pp.",3, contentletsWithNewLanguages);

            //Expected language codes. These are created by PP
            final Set<String> expectedNewLangs = new HashSet<>(
                    Arrays.asList("ru" + languagesSuffix,
                            "da" + languagesSuffix,
                            "en"
                                    + languagesSuffix)); //Should have been created with the original ids that they originally had.
            final boolean newLanguagesMatch = publishedContentlets.stream().
              filter(contentlet -> savedLanguagesNowDeletedIds.contains(contentlet.getLanguageId())).allMatch(contentlet ->  {
                final Language lang = languageAPI.getLanguage(contentlet.getLanguageId());
                return expectedNewLangs.contains(lang.getLanguageCode());
              } );

            assertTrue("new Languages created are not a match.", newLanguagesMatch);

        } finally {

            if (null != file) {
                try {
                    file.delete();
                } catch (Exception e) {
                    //Do nothing...
                }
            }

        }

    }


    private void printContentletLanguageInfo(final List<Contentlet> publishedContentlets) {
        publishedContentlets.forEach(contentlet -> {
            Logger.info(RemoteReceiverLanguageResolutionTest.class,
                    () -> "id: " + contentlet.getLanguageId() + " - " + contentlet
                            .get("title")
            );
        });
    }

}
