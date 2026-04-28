package com.dotcms.ai.listener;

import com.dotcms.ai.AiTest;
import com.dotcms.ai.api.DotAIAPIFacadeImpl;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dotmarketing.util.ThreadUtils.sleep;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This class contains integration tests for the EmbeddingContentListener class.
 * It tests the behavior of the EmbeddingContentListener when contentlets are published, archived, and deleted.
 * The tests ensure that embeddings are correctly added to and removed from the index based on the contentlet's lifecycle events.
 * The class sets up a mock environment and uses WireMock to simulate the AI service.
 *
 * @author vico
 */
public class EmbeddingContentListenerTest {

    private static final int MAX_ATTEMPTS = 30;

    private static User user;
    private static Host host;
    private static ContentTypeAPI contentTypeApi;
    private static ContentletAPI contentletApi;
    private static LanguageAPI languageApi;
    private static AppsAPI appsAPI;
    private static final List<ContentType> contentTypes = new ArrayList<>();
    private static final List<Contentlet> contentlets = new ArrayList<>();
    private static WireMockServer wireMockServer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        IPUtils.disabledIpPrivateSubnet(true);

        user = APILocator.getUserAPI().getSystemUser();
        contentTypeApi = APILocator.getContentTypeAPI(user);
        contentletApi = APILocator.getContentletAPI();
        languageApi = APILocator.getLanguageAPI();
        appsAPI = APILocator.getAppsAPI();
        host = new SiteDataGen().nextPersisted();
        wireMockServer = AiTest.prepareWireMock();
        addDotAISecrets();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);

        removeContentRelated();
        removeDotAISecrets();
    }

    /**
     * Given a ContentType and a Contentlet of that type with some text,
     * When the Contentlet is published,
     * Then it waits for the embeddings to be created for the published Contentlet,
     * And asserts that the embeddings exist after the Contentlet is published,
     * And checks and asserts that the embeddings are correctly added to the index.
     */
    @Test
    public void test_onPublish() throws Exception {
        DotAIAPIFacadeImpl.setDefaultEmbeddingsAPIProvider(new DotAIAPIFacadeImpl.DefaultEmbeddingsAPIProvider());

        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType("blog", host);
        contentTypes.add(blogContentType);
        final String text = "OpenAI has developed a new AI model that surpasses previous benchmarks in natural language understanding and generation. This model, GPT-4, can perform complex tasks such as writing essays, creating code, and understanding nuanced prompts with unprecedented accuracy.";
        final Contentlet blogContent = TestDataUtils.withEmbeddings(
                true,
                host,
                languageApi.getDefaultLanguage().getId(),
                blogContentType.id(),
                text);
        contentlets.add(blogContent);
        contentletApi.publish(blogContent, user, false);

        boolean embeddingsExist = waitForEmbeddings(blogContent, text, true);

        assertTrue(embeddingsExist);
        final Map<String, Map<String, Object>> embeddingsByIndex = APILocator.getDotAIAPI().getEmbeddingsAPI().countEmbeddingsByIndex();
        assertFalse(embeddingsByIndex.isEmpty());
        assertTrue(embeddingsByIndex.containsKey("default"));
        final Map<String, Object> embeddings = embeddingsByIndex.get("default");
        assertFalse(embeddings.isEmpty());
        assertTrue(embeddings.containsKey("contentTypes"));
        assertTrue(embeddings.containsKey("contents"));
        assertTrue(embeddings.containsKey("fragments"));
        assertTrue((Long) embeddings.get("tokenTotal") > 0L);
        assertTrue((Long) embeddings.get("tokensPerChunk") > 0L);
    }

    /**
     * Given a published contentlet,
     * When the contentlet is archived,
     * Then the embeddings should be removed from the index.
     */
    @Test
    public void test_onArchive() throws Exception {
        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType("blog", host);
        contentTypes.add(blogContentType);
        final String text = "In the latest NBA finals, the Golden State Warriors clinched the championship title after a thrilling seven-game series against the Boston Celtics. Stephen Curry was named the MVP for his outstanding performance.";
        final Contentlet blogContent = TestDataUtils.withEmbeddings(
                true,
                host,
                languageApi.getDefaultLanguage().getId(),
                blogContentType.id(),
                text);
        contentlets.add(blogContent);

        contentletApi.publish(blogContent, user, false);
        waitForEmbeddings(blogContent, text, true);

        contentletApi.archive(blogContent, user, false);
        boolean embeddingsExist = waitForEmbeddings(blogContent, text, false);

        assertFalse(embeddingsExist);
    }

    /**
     * Given a published and archived contentlet,
     * When the contentlet is deleted,
     * Then the embeddings should be removed from the index.
     */
    @Test
    public void test_onDelete() throws Exception {
        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType("blog");
        contentTypes.add(blogContentType);
        final String text = "The latest Marvel movie, 'Avengers: Endgame,' has broken box office records with its stunning visual effects and compelling storyline. Critics praise the film for its emotional depth and action-packed scenes.";
        final Contentlet blogContent = TestDataUtils.withEmbeddings(
                true,
                host,
                languageApi.getDefaultLanguage().getId(),
                blogContentType.id(),
                text);
        contentlets.add(blogContent);

        contentletApi.publish(blogContent, user, false);
        waitForEmbeddings(blogContent, text, true);

        contentletApi.archive(blogContent, user, false);
        waitForEmbeddings(blogContent, text, false);

        contentletApi.delete(blogContent, user, false);
        boolean embeddingsExist = waitForEmbeddings(blogContent, text, false);

        assertFalse(embeddingsExist);
    }

    private static boolean waitForEmbeddings(final Contentlet blogContent, final String text, final boolean expected) {
        int count = 0;
        boolean embeddingsExist = APILocator.getDotAIAPI().getEmbeddingsAPI().embeddingExists(blogContent.getInode(), "default", text);
        while (embeddingsExist != expected) {
            if (count++ > MAX_ATTEMPTS) {
                break;
            }

            sleep(500);
            embeddingsExist = APILocator.getDotAIAPI().getEmbeddingsAPI().embeddingExists(blogContent.getInode(), "default", text);
        }
        return embeddingsExist;
    }

    private static void addDotAISecrets() throws Exception {
        AiTest.aiAppSecrets(host, AiTest.API_KEY);
        AiTest.aiAppSecrets(APILocator.systemHost(), AiTest.API_KEY);
    }

    private static void removeDotAISecrets() throws DotDataException, DotSecurityException {
        appsAPI.removeApp(AppKeys.APP_KEY, user, false);
    }

    private static void removeContentRelated() {
        // As we use the same mechanism to create contentlets,
        // using deterministic identifiers this collection might have duplicates
        contentlets.stream().filter(distinctByKey(Contentlet::getIdentifier)).collect(Collectors.toList())
        .forEach(contentlet -> {
            try {
                contentletApi.archive(contentlet, user, false);
                contentletApi.delete(contentlet, user, false);
            } catch (DotDataException | DotSecurityException e) {
                //ignore
            }
        });

        contentTypes.forEach(contentType -> {
            try {
                contentTypeApi.delete(contentType);
            } catch (DotDataException | DotSecurityException e) {
                //ignore
            }
        });
    }

    /**
     * Returns a predicate that filters elements based on a key extracted from each element.
     * @param keyExtractor the key extractor
     * @return the predicate
     * @param <T> the type of the input to the predicate
     */
    public static <T> java.util.function.Predicate<T> distinctByKey(
            Function<? super T, Object> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
