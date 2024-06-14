package com.dotcms.ai.listener;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.viewtool.AiTest;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.dotmarketing.util.ThreadUtils.sleep;

public class EmbeddingContentListenerTest {

    private static User user;
    private static Host host;
    private static HostAPI hostAPI;
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
        hostAPI = APILocator.getHostAPI();
        contentTypeApi = APILocator.getContentTypeAPI(user);
        contentletApi = APILocator.getContentletAPI();
        languageApi = APILocator.getLanguageAPI();
        appsAPI = APILocator.getAppsAPI();
        host = APILocator.systemHost();
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

    private static void addDotAISecrets() throws DotDataException, DotSecurityException {
        final AppSecrets.Builder builder = AppSecrets.builder()
                .withKey(AppKeys.APP_KEY)
                .withSecrets(AiTest.appConfigMap(wireMockServer));
        appsAPI.saveSecrets(builder.build(), host, user);
    }

    @Test
    public void test_onPublish() throws DotDataException, DotSecurityException {
        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType("blog");
        contentTypes.add(blogContentType);
        final Contentlet blogContent = TestDataUtils.getBlogContentWithEmbeddings(
                true,
                languageApi.getDefaultLanguage().getId(),
                blogContentType.id(),
                "Elaborate on the black holes view from Stephen Hawking in the words a 12 year old could understand.");
        contentlets.add(blogContent);
        contentletApi.publish(blogContent, user, false);

        sleep(2000);

        final Contentlet published = contentletApi.find(blogContent.getInode(), user, false);

    }

    private static void removeDotAISecrets() throws DotDataException, DotSecurityException {
        appsAPI.removeApp(AppKeys.APP_KEY, user, false);
    }

    private static void removeContentRelated() {
        contentlets.forEach(contentlet -> {
            try {
                contentletApi.archive(contentlet, user, false);
                contentletApi.delete(contentlet, user, false);
            } catch (DotDataException | DotSecurityException e) {
            }
        });

        contentTypes.forEach(contentType -> {
            try {
                contentTypeApi.delete(contentType);
            } catch (DotDataException | DotSecurityException e) {
            }
        });
    }
}
