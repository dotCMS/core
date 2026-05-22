package com.dotcms.api;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.common.SiteTestHelperService;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.language.Language;
import com.dotcms.model.site.CopySiteRequest;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class SiteAPIIT {

    /**
     * One-shot guard so the language cache warm-up runs at most once per JVM regardless
     * of how many @BeforeEach invocations happen across this test class.
     */
    private static volatile boolean languageCacheWarmed = false;

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @Inject
    SiteTestHelperService siteTestHelper;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").url(new URL("http://localhost:8080")).active(true).build());

        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);

        warmLanguageCacheIfNeeded();
    }

    /**
     * Ensures the dotCMS test environment has a real default language (id &gt; 0) before
     * the SiteAPIIT tests run. Without this, {@code POST /api/v1/site} resolves the Host
     * contentlet's languageId from {@code getDefaultLanguage()} — which in some CI test
     * environments returns the {@code LANG__404} sentinel with id=-1, breaking the
     * downstream unique-field validation with HTTP 500 "Language cannot be null".
     * See issue #35780.
     *
     * <p>Strategy:
     * <ol>
     *   <li>List languages</li>
     *   <li>If the entry marked {@code defaultLanguage=true} has a valid id, we are done.</li>
     *   <li>Otherwise pick the first language with id &gt; 0 (English in starter data)
     *       and call {@code PUT /api/v2/languages/{id}/_makedefault} to fix the broken
     *       default. {@code fireTransferAssetsJob=false} so this is a fast metadata change.</li>
     * </ol>
     */
    private void warmLanguageCacheIfNeeded() {
        if (languageCacheWarmed) {
            return;
        }
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                final List<Language> languages = listLanguages();
                if (hasValidDefault(languages)) {
                    languageCacheWarmed = true;
                    return;
                }
                final Long fallbackId = pickFallbackLanguageId(languages);
                if (fallbackId != null && tryMakeDefault(fallbackId)) {
                    // Re-list to confirm the fix took before declaring success.
                    if (hasValidDefault(listLanguages())) {
                        languageCacheWarmed = true;
                        return;
                    }
                }
            } catch (Exception ignored) {
                // fall through to retry
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private List<Language> listLanguages() {
        final ResponseEntityView<List<Language>> response =
                clientFactory.getClient(LanguageAPI.class).list();
        return response != null ? response.entity() : null;
    }

    private static boolean hasValidDefault(final List<Language> languages) {
        if (languages == null) {
            return false;
        }
        return languages.stream().anyMatch(l ->
                l != null
                        && l.id().isPresent() && l.id().get() > 0
                        && l.defaultLanguage().orElse(false));
    }

    private static Long pickFallbackLanguageId(final List<Language> languages) {
        if (languages == null) {
            return null;
        }
        return languages.stream()
                .filter(l -> l != null && l.id().isPresent() && l.id().get() > 0)
                .map(l -> l.id().get())
                .findFirst()
                .orElse(null);
    }

    private boolean tryMakeDefault(final Long languageId) {
        try {
            clientFactory.getClient(LanguageAPI.class)
                    .makeDefault(String.valueOf(languageId),
                            Map.of("fireTransferAssetsJob", Boolean.FALSE));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Retries the supplied Site create call when the server returns HTTP 500
     * "Language cannot be null". The {@link #warmLanguageCacheIfNeeded()} warmup
     * covers the most common race but the underlying server-side defect (POST
     * {@code /api/v1/site} does not default the Host contentlet's
     * {@code languageId} from {@code getDefaultLanguage()}, so unique-field
     * validation NPEs in {@code UniqueFieldCriteria}) can still surface on a
     * fresh dotCMS startup. This wrapper is a test-side mitigation; the real
     * fix is server-side. See issue #35780.
     */
    private <T> T retryOnLanguageNull(final Supplier<T> call) {
        final int maxAttempts = 3;
        final long backoffMs = 250L;
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                if (!isLanguageNullError(e)) {
                    throw e;
                }
                lastError = e;
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw lastError;
    }

    private static boolean isLanguageNullError(Throwable t) {
        while (t != null) {
            final String msg = t.getMessage();
            if (msg != null && msg.contains("Language cannot be null")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    @Test
    void Test_Get_Sites() {

        final ResponseEntityView<List<Site>> sitesResponse = clientFactory.getClient(SiteAPI.class).getSites(null, false, true, true, 1, 10);
        Assertions.assertNotNull(sitesResponse);
    }

    @Test
    void Test_Find_Host_By_Name() {
        final ResponseEntityView<SiteView> sitesResponse = clientFactory.getClient(SiteAPI.class)
                .findByName(
                        GetSiteByNameRequest.builder().siteName(siteName).build());
        Assertions.assertNotNull(sitesResponse);
        Assertions.assertEquals(siteName,sitesResponse.entity().hostName());
    }

    @Test
    void Test_Find_Non_Existing_Host_By_Name() {
        try {
            final ResponseEntityView<SiteView> sitesResponse = clientFactory.getClient(SiteAPI.class)
                    .findByName(
                            GetSiteByNameRequest.builder().siteName("myRandomSite" + System.currentTimeMillis()).build());
            Assertions.fail(" 404 Exception should have been thrown here.");
        }catch (Exception e){
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    void Test_Default_Site() {
        ResponseEntityView<Site> defaultSiteResponse = clientFactory.getClient(SiteAPI.class).defaultSite();
        Assertions.assertNotNull(defaultSiteResponse);
        Assertions.assertTrue(defaultSiteResponse.entity().isDefault());
    }

    @Test
    void Test_Create_New_Site_Then_Update_Then_Delete() {

        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = retryOnLanguageNull(
                () -> clientFactory.getClient(SiteAPI.class).create(newSiteRequest));
        Assertions.assertNotNull(createSiteResponse);
        Assertions.assertFalse(createSiteResponse.entity().isDefault());
        String identifier = createSiteResponse.entity().identifier();
        Assertions.assertNotNull(identifier);
        Assertions.assertEquals(newSiteName,createSiteResponse.entity().hostName());

        final String updateSiteName = String.format("updatedSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest updateSiteRequest = CreateUpdateSiteRequest.builder().siteName(updateSiteName).forceExecution(true).build();
        ResponseEntityView<SiteView> updateSiteResponse = clientFactory.getClient(SiteAPI.class).update(identifier,updateSiteRequest);
        Assertions.assertNotNull(updateSiteResponse);
        Assertions.assertFalse(updateSiteResponse.entity().isDefault());
        String returnedIdentifier = updateSiteResponse.entity().identifier();
        Assertions.assertNotNull(returnedIdentifier);
        Assertions.assertEquals(updateSiteName,updateSiteResponse.entity().hostName());

        ResponseEntityView<SiteView> archiveSite = clientFactory.getClient(SiteAPI.class).archive(returnedIdentifier);
        Assertions.assertNotNull(archiveSite.entity());
        Assertions.assertEquals(updateSiteName,archiveSite.entity().hostName());

        ResponseEntityView<Boolean> deleteSite = clientFactory.getClient(SiteAPI.class).delete(returnedIdentifier);
        Assertions.assertTrue(deleteSite.entity());

    }

    @Test
    void Test_Archive_Unarchive() {

        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = retryOnLanguageNull(
                () -> clientFactory.getClient(SiteAPI.class).create(newSiteRequest));
        Assertions.assertNotNull(createSiteResponse);
        Assertions.assertFalse(createSiteResponse.entity().isDefault());
        final String identifier = createSiteResponse.entity().identifier();
        Assertions.assertNotNull(identifier);
        Assertions.assertEquals(newSiteName,createSiteResponse.entity().hostName());

        ResponseEntityView<SiteView> archiveSite = clientFactory.getClient(SiteAPI.class).archive(identifier);
        Assertions.assertNotNull(archiveSite.entity());
        Assertions.assertTrue(siteTestHelper.checkValidSiteStatus(newSiteName, false, true));

        ResponseEntityView<SiteView> unarchiveSite = clientFactory.getClient(SiteAPI.class).unarchive(identifier);
        Assertions.assertNotNull(unarchiveSite.entity());
        Assertions.assertTrue(
                siteTestHelper.checkValidSiteStatus(newSiteName, false, false));
    }

    @Test
    void Test_Publish_UnPublish_Site() {

        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = retryOnLanguageNull(
                () -> clientFactory.getClient(SiteAPI.class).create(newSiteRequest));
        Assertions.assertNotNull(createSiteResponse);
        Assertions.assertFalse(createSiteResponse.entity().isDefault());
        final String identifier = createSiteResponse.entity().identifier();
        Assertions.assertNotNull(identifier);
        Assertions.assertEquals(newSiteName,createSiteResponse.entity().hostName());
        Assert.assertFalse(createSiteResponse.entity().isLive());

        ResponseEntityView<SiteView> publishedSite = clientFactory.getClient(SiteAPI.class).publish(identifier);
        Assertions.assertNotNull(publishedSite.entity());
        Assertions.assertTrue(siteTestHelper.checkValidSiteStatus(newSiteName, true, false));

        ResponseEntityView<SiteView> unPublishedSite = clientFactory.getClient(SiteAPI.class).unpublish(identifier);
        Assertions.assertNotNull(unPublishedSite.entity());
        Assertions.assertTrue(
                siteTestHelper.checkValidSiteStatus(newSiteName, false, false));
    }

    @Test
    void Test_Copy_Site() {
        final String newSiteName = String.format("newSiteName-%d",System.currentTimeMillis());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder().siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = retryOnLanguageNull(
                () -> clientFactory.getClient(SiteAPI.class).create(newSiteRequest));
        Assertions.assertNotNull(createSiteResponse);

        final String copySiteName = String.format("newSiteName-%d",System.currentTimeMillis());

        CopySiteRequest copySiteRequest = CopySiteRequest.builder().copyFromSiteId(
                createSiteResponse.entity().identifier()).
                copyContentOnSite(true).
                copyAll(true).
                copyContentOnPages(true).
                copyFolders(true).
                copyTemplatesContainers(true).
                copyLinks(true).
                copySiteVariables(true).
                site(CreateUpdateSiteRequest.builder().siteName(copySiteName).build()).build();

        ResponseEntityView<SiteView> copy = clientFactory.getClient(SiteAPI.class).copy(copySiteRequest);
        Assertions.assertNotNull(copy.entity());

    }

}
