package com.dotcms.ai.api;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.dotmarketing.util.ThreadUtils.sleep;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link BulkEmbeddingsRunner}.
 *
 * <p>Validates that the runner uses the requesting site's dotAI config for all
 * contentlets in a batch, regardless of which site each contentlet belongs to.
 * This ensures consistent embedding model usage within a single index and
 * allows indexing content from sites that have no dotAI config of their own.
 */
public class BulkEmbeddingsRunnerTest {

    private static final int MAX_ATTEMPTS = 30;

    private static User user;
    private static Host configuredSite;
    private static LanguageAPI languageApi;
    private static WireMockServer wireMockServer;
    private static ContentType blogContentType;
    private static Contentlet contentletOnSystemHost;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        user = APILocator.getUserAPI().getSystemUser();
        languageApi = APILocator.getLanguageAPI();
        configuredSite = new SiteDataGen().nextPersisted();
        wireMockServer = AiTest.prepareWireMock();
        // Only the requesting site has dotAI configured — System Host does NOT
        AiTest.aiAppSecretsWithProviderConfig(configuredSite, AiTest.providerConfigJson(AiTest.PORT, AiTest.MODEL));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
        if (contentletOnSystemHost != null) {
            try {
                APILocator.getContentletAPI().archive(contentletOnSystemHost, user, false);
                APILocator.getContentletAPI().delete(contentletOnSystemHost, user, false);
            } catch (DotDataException | DotSecurityException e) {
                // ignore
            }
        }
        if (blogContentType != null) {
            try {
                APILocator.getContentTypeAPI(user).delete(blogContentType);
            } catch (DotDataException | DotSecurityException e) {
                // ignore
            }
        }
        AiTest.removeAiAppSecrets(configuredSite);
    }

    /**
     * Given a contentlet on System Host (which has no dotAI config),
     * and a form with requestHostId pointing to a site that has dotAI configured,
     * When BulkEmbeddingsRunner processes that contentlet,
     * Then embeddings should be generated using the requesting site's config.
     */
    @Test
    public void test_run_usesRequestHostConfig_whenContentletHostHasNoConfig() throws Exception {
        DotAIAPIFacadeImpl.setDefaultEmbeddingsAPIProvider(
                new DotAIAPIFacadeImpl.DefaultEmbeddingsAPIProvider());

        blogContentType = TestDataUtils.getBlogLikeContentType("blog", APILocator.systemHost());
        final String text = "BulkEmbeddingsRunner should use the requesting site config "
                + "when the contentlet's host has no dotAI configuration.";
        contentletOnSystemHost = TestDataUtils.withEmbeddings(
                true,
                APILocator.systemHost(),
                languageApi.getDefaultLanguage().getId(),
                blogContentType.id(),
                text);
        APILocator.getContentletAPI().publish(contentletOnSystemHost, user, false);

        final EmbeddingsForm form = new EmbeddingsForm.Builder()
                .indexName("default")
                .requestHostId(configuredSite.getIdentifier())
                .build();
        new BulkEmbeddingsRunner(List.of(contentletOnSystemHost.getInode()), form).run();

        assertTrue(
                "Expected embeddings when requestHostId points to a site with dotAI config",
                waitForEmbeddings(contentletOnSystemHost, text));
    }

    /**
     * Given the same contentlet on System Host,
     * and a form WITHOUT requestHostId (falls back to System Host which has no config),
     * When BulkEmbeddingsRunner processes that contentlet,
     * Then no embeddings should be generated.
     */
    @Test
    public void test_run_noEmbeddings_whenRequestHostHasNoConfig() throws Exception {
        DotAIAPIFacadeImpl.setDefaultEmbeddingsAPIProvider(
                new DotAIAPIFacadeImpl.DefaultEmbeddingsAPIProvider());

        final ContentType ct = TestDataUtils.getBlogLikeContentType("blogNoConfig", APILocator.systemHost());
        final String text = "This contentlet should not be embedded because neither its host "
                + "nor the request host has dotAI configured.";
        final Contentlet contentlet = TestDataUtils.withEmbeddings(
                true,
                APILocator.systemHost(),
                languageApi.getDefaultLanguage().getId(),
                ct.id(),
                text);
        try {
            APILocator.getContentletAPI().publish(contentlet, user, false);

            // No requestHostId — falls back to System Host, which has no dotAI config
            final EmbeddingsForm form = new EmbeddingsForm.Builder()
                    .indexName("default")
                    .build();
            new BulkEmbeddingsRunner(List.of(contentlet.getInode()), form).run();

            assertFalse(
                    "Expected no embeddings when neither contentlet host nor request host has dotAI config",
                    embeddingExists(contentlet, text));
        } finally {
            try {
                APILocator.getContentletAPI().archive(contentlet, user, false);
                APILocator.getContentletAPI().delete(contentlet, user, false);
                APILocator.getContentTypeAPI(user).delete(ct);
            } catch (DotDataException | DotSecurityException e) {
                // ignore
            }
        }
    }

    private static boolean waitForEmbeddings(final Contentlet contentlet, final String text) {
        int count = 0;
        boolean exists = embeddingExists(contentlet, text);
        while (!exists) {
            if (count++ > MAX_ATTEMPTS) {
                break;
            }
            sleep(500);
            exists = embeddingExists(contentlet, text);
        }
        return exists;
    }

    private static boolean embeddingExists(final Contentlet contentlet, final String text) {
        return APILocator.getDotAIAPI().getEmbeddingsAPI(configuredSite)
                .embeddingExists(contentlet.getInode(), "default", text);
    }

}
