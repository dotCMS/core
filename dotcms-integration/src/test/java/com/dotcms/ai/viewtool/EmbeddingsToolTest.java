package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.datagen.EmbeddingsDTODataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the \EmbeddingsTool\ class. This test class verifies the functionality
 * of methods in \EmbeddingsTool\ such as counting tokens, generating embeddings, and
 * retrieving index counts. It uses mock objects to simulate the \ViewContext\ and
 * \AppConfig\ dependencies.
 *
 * @author vico
 */
public class EmbeddingsToolTest {

    private static WireMockServer wireMockServer;

    private Host host;
    private AppConfig appConfig;
    private User user;
    private EmbeddingsTool embeddingsTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        AiTest.aiAppSecrets(APILocator.systemHost());
    }

    @Before
    public void before() throws Exception {
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(mock(HttpServletRequest.class));
        host = new SiteDataGen().nextPersisted();
        AiTest.aiAppSecrets(host);
        appConfig = ConfigService.INSTANCE.config(host);
        user = new UserDataGen().nextPersisted();
        embeddingsTool = prepareEmbeddingsTool(viewContext);
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    /**
     * Given a text string
     * When the countTokens method is called
     * Then the correct number of tokens should be returned.
     */
    @Test
    public void test_countTokens() {
        final int tokens = embeddingsTool.countTokens("This is a text with some tokens.");
        assertEquals(8, tokens);
    }

    /**
     * Given a prompt string
     * When the generateEmbeddings method is called
     * Then a list of embeddings should be returned
     * And the size of the list should be 1536.
     */
    @Test
    public void test_generateEmbeddings() {
        final String prompt = "Explain the meaning of life.";
        EmbeddingsDTODataGen.persistEmbeddings(prompt, null, "default");

        final List<Float> embeddings = embeddingsTool.generateEmbeddings(prompt);
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.size());
    }

    /**
     * When the getIndexCount method is called
     * Then a map of embeddings should be returned
     * And the map should contain keys for "default" and "cache"
     * And each key should map to a map containing details about the embeddings.
     */
    @Test
    public void test_getIndexCount() {
        final String prompt = "Explain the meaning of life.";
        EmbeddingsDTODataGen.persistEmbeddings(prompt, null, "default");

        embeddingsTool.generateEmbeddings(prompt);

        final Map<String, Map<String, Object>> embeddings = embeddingsTool.getIndexCount();
        assertNotNull(embeddings);
        assertEmbeddings(embeddings, "default");
        assertEmbeddings(embeddings, "cache");
    }

    private void assertEmbeddings(final Map<String, Map<String, Object>> embeddings, final String key) {
        assertTrue(embeddings.containsKey(key));
        final Map<String, Object> details = embeddings.get(key);
        assertNotNull(details.get("contents"));
        assertNotNull(details.get("fragments"));
        assertNotNull(details.get("tokenTotal"));
        assertNotNull(details.get("tokensPerChunk"));
        assertNotNull(details.get("contentTypes"));
    }

    private EmbeddingsTool prepareEmbeddingsTool(final ViewContext viewContext) {
        return new EmbeddingsTool(viewContext) {
            @Override
            Host host() {
                return host;
            }

            @Override
            AppConfig appConfig() {
                return appConfig;
            }

            @Override
            public User user() {
                return user;
            }
        };
    }

}
