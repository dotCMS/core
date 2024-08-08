package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a facade for all artificial intelligence services offered by dotCMS.
 * Also provides a static way to set the current API implementation name and the default implementations for completions and embeddings.
 * @author jsanca
 */
public class DotAIAPIFacadeImpl implements DotAIAPI {


    private static final String DEFAULT = "default";
    private static final AtomicReference<String> currentApiProviderName = new AtomicReference<>(DEFAULT);
    private static final Map<String, CompletionsAPIProvider> completionsProviderMap = new ConcurrentHashMap<>();
    private static final Map<String, EmbeddingsAPIProvider> embeddingsProviderMap   = new ConcurrentHashMap<>();
    private static final Map<String, ChatAPIProvider> chatProviderMap   = new ConcurrentHashMap<>();
    private static final Map<String, ImageAPIProvider> imageProviderMap   = new ConcurrentHashMap<>();

    static {
        try {
            completionsProviderMap.put(DEFAULT, new DefaultCompletionsAPIProvider());
            embeddingsProviderMap.put(DEFAULT, new DefaultEmbeddingsAPIProvider());
            chatProviderMap.put(DEFAULT, new DefaultChatAPIProvider());
            imageProviderMap.put(DEFAULT, new DefaultImageAPIProvider());
        } catch (Exception e) {
            Logger.error(DotAIAPI.class, e.getMessage(), e);
        }
    }

    public static class DefaultChatAPIProvider implements ChatAPIProvider {

        @Override
        public ChatAPI getChatAPI(final Object... initArguments) {
            if (Objects.nonNull(initArguments) && initArguments.length > 0 && initArguments[0] instanceof AppConfig) {
                return new OpenAIChatAPIImpl((AppConfig) initArguments[0]);
            }

            throw new IllegalArgumentException("To create a ChatAPI you need to provide an AppConfig");
        }
    }

    public static class DefaultImageAPIProvider implements ImageAPIProvider {

        @Override
        public ImageAPI getImageAPI(final Object... initArguments) {
            if (Objects.nonNull(initArguments) && initArguments.length >= 4
                    && initArguments[0] instanceof AppConfig
                    && initArguments[1] instanceof User
            ) {

                final AppConfig config = (AppConfig) initArguments[0];
                final User user = (User) initArguments[1];
                final HostAPI hostApi = APILocator.getHostAPI();
                final TempFileAPI tempFileApi = APILocator.getTempFileAPI();
                return new OpenAIImageAPIImpl(config, user, hostApi, tempFileApi);
            }

            throw new IllegalArgumentException("To create an Image  you need to provide an AppConfig");
        }
    }

    public static class DefaultCompletionsAPIProvider implements CompletionsAPIProvider {

        private final CompletionsAPI defaultCompletionAPI = new CompletionsAPIImpl(null);

        @Override
        public CompletionsAPI getCompletionsAPI(final Object... initArguments) {
            return Objects.nonNull(initArguments) && initArguments.length > 0?
                    new CompletionsAPIImpl(unwrap(initArguments)):
                    defaultCompletionAPI;
        }

        private Lazy<AppConfig> unwrap(final Object... initArguments) {
            return initArguments[0] instanceof AppConfig?
                    Lazy.of (()-> (AppConfig) initArguments[0]):(Lazy<AppConfig>) initArguments[0];
        }
    }

    public static class DefaultEmbeddingsAPIProvider implements EmbeddingsAPIProvider {

        private final EmbeddingsAPI defaultEmbeddingsAPI = new EmbeddingsAPIImpl(null);

        public EmbeddingsAPI getEmbeddingsAPI(final Object... initArguments) {
            return Objects.nonNull(initArguments) && initArguments.length > 0?
                    new EmbeddingsAPIImpl(unwrap(initArguments)):
                    defaultEmbeddingsAPI;
        }

        private Host unwrap(final Object... initArguments) {
            return initArguments[0] instanceof Host ?
                    (Host) initArguments[0]:null;
        }
    }


    /**
     * Sets the current API implementation name.
     * @param apiName
     */
    public static final void setCurrentApiProviderName(final String apiName) {
        currentApiProviderName.set(apiName);
    }

    /**
     * Sets the default completions API Provider.
     * @param completionsAPI
     */
    public static final void setDefaultCompletionsAPIProvider(final CompletionsAPIProvider completionsAPI) {
        completionsProviderMap.put(DEFAULT, completionsAPI);
    }

    /**
     * Adds the default embeddings API Provider.
     * @param embeddingsAPI
     */
    public static final void setDefaultEmbeddingsAPIProvider(final EmbeddingsAPIProvider embeddingsAPI) {
        embeddingsProviderMap.put(DEFAULT, embeddingsAPI);
    }

    /**
     * Adds the default completions API provider.
     * @param completionsAPI
     */
    public static final void addCompletionsAPIImplementation(final String apiName, final CompletionsAPIProvider completionsAPI) {
        completionsProviderMap.put(apiName, completionsAPI);
    }

    /**
     * Sets the default embeddings API provider.
     * @param embeddingsAPI
     */
    public static final void addDefaultEmbeddingsAPIImplementation(final String apiName, final EmbeddingsAPIProvider embeddingsAPI) {
        embeddingsProviderMap.put(apiName, embeddingsAPI);
    }

    @Override
    public CompletionsAPI getCompletionsAPI(final Object... initArguments) {

        return completionsProviderMap.get(currentApiProviderName.get()).getCompletionsAPI(initArguments);
    }

    @Override
    public EmbeddingsAPI getEmbeddingsAPI(final Object... initArguments) {
        return embeddingsProviderMap.get(currentApiProviderName.get()).getEmbeddingsAPI(initArguments);
    }

    @Override
    public ChatAPI getChatAPI(final Object... initArguments) {

        return chatProviderMap.get(currentApiProviderName.get()).getChatAPI(initArguments);
    }

    @Override
    public ImageAPI getImageAPI(final Object... initArguments) {

        return imageProviderMap.get(currentApiProviderName.get()).getImageAPI(initArguments);
    }
}
