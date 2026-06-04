package com.dotcms.ai.api;

import com.dotcms.ai.AiTest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.ai.rest.forms.EmbeddingsForm;
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
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link BulkEmbeddingsRunner}.
 *
 * <p>The setup intentionally configures AI secrets only on the site host, not on System Host.
 * This validates that the runner resolves the host per-contentlet and uses the correct
 * site config rather than falling back to System Host.
 */
public class BulkEmbeddingsRunnerTest {

    private static final int MAX_ATTEMPTS = 30;

    private static User user;
    private static Host host;
    private static LanguageAPI languageApi;
    private static WireMockServer wireMockServer;
    private static ContentType blogContentType;
    private static Contentlet contentlet;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        user = APILocator.getUserAPI().getSystemUser();
        languageApi = APILocator.getLanguageAPI();
        host = new SiteDataGen().nextPersisted();
        wireMockServer = AiTest.prepareWireMock();
        // Configure AI only on the site host — intentionally NOT on System Host
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, AiTest.MODEL));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
        if (contentlet != null) {
            try {
                APILocator.getContentletAPI().archive(contentlet, user, false);
                APILocator.getContentletAPI().delete(contentlet, user, false);
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
        AiTest.removeAiAppSecrets(host);
    }

    /**
     * Given a contentlet published on a site that has AI configured,
     * and System Host does NOT have AI configured,
     * When BulkEmbeddingsRunner processes that contentlet,
     * Then embeddings should be generated using the site config.
     */
    @Test
    public void test_run_generatesEmbeddings_withSiteOnlyConfig() throws Exception {
        DotAIAPIFacadeImpl.setDefaultEmbeddingsAPIProvider(
                new DotAIAPIFacadeImpl.DefaultEmbeddingsAPIProvider());

        blogContentType = TestDataUtils.getBlogLikeContentType("blog", host);
        final String text = "BulkEmbeddingsRunner should resolve the host from the contentlet "
                + "and use the site config rather than falling back to System Host.";
        contentlet = TestDataUtils.withEmbeddings(
                true,
                host,
                languageApi.getDefaultLanguage().getId(),
                blogContentType.id(),
                text);
        APILocator.getContentletAPI().publish(contentlet, user, false);

        final EmbeddingsForm form = new EmbeddingsForm.Builder()
                .indexName("default")
                .build();
        new BulkEmbeddingsRunner(List.of(contentlet.getInode()), form).run();

        assertTrue(
                "Expected embeddings after BulkEmbeddingsRunner.run() with site-only AI config",
                waitForEmbeddings(contentlet, text));
    }

    private static boolean waitForEmbeddings(final Contentlet contentlet, final String text) {
        int count = 0;
        boolean exists = APILocator.getDotAIAPI().getEmbeddingsAPI(host)
                .embeddingExists(contentlet.getInode(), "default", text);
        while (!exists) {
            if (count++ > MAX_ATTEMPTS) {
                break;
            }
            sleep(500);
            exists = APILocator.getDotAIAPI().getEmbeddingsAPI(host)
                    .embeddingExists(contentlet.getInode(), "default", text);
        }
        return exists;
    }

}
