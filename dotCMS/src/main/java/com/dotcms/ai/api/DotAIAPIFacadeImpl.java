package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

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

    private static <T> T unwrap(final Class<T> clazz, final Object... initArguments) {
        return Objects.nonNull(initArguments)
                && initArguments.length > 0
                && clazz.isInstance(initArguments[0]) ? clazz.cast(initArguments[0]) : null;
    }

    /**
     * Default provider for the ChatAPI
     */
    public static class DefaultChatAPIProvider implements ChatAPIProvider {

        @Override
        public ChatAPI getChatAPI(final Object... initArguments) {
            if (Objects.nonNull(initArguments) && initArguments.length > 0 && initArguments[0] instanceof AppConfig) {
                final User user = initArguments.length > 1 && initArguments[1] instanceof User
                        ? (User) initArguments[1]
                        : null;
                return new OpenAIChatAPIImpl((AppConfig) initArguments[0], user);
            }

            throw new IllegalArgumentException("To create a ChatAPI you need to provide an AppConfig");
        }
    }

    /**
     * Default provider for the ImageAPI
     */
    public static class DefaultImageAPIProvider implements ImageAPIProvider {

        @Override
        public ImageAPI getImageAPI(final Object... initArguments) {
            if (Objects.nonNull(initArguments) && initArguments.length >= 4
                    && initArguments[0] instanceof AppConfig
                    && (Objects.isNull(initArguments[1]) || initArguments[1] instanceof User)
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

    /**
     * Default provider for the CompletionsAPI
     */
    private static class DefaultCompletionsAPIProvider implements CompletionsAPIProvider {

        @Override
        public CompletionsAPI getCompletionsAPI(final Object... initArguments) {
            return new CompletionsAPIImpl(unwrap(initArguments));
        }

        private AppConfig unwrap(final Object... initArguments) {
            return DotAIAPIFacadeImpl.unwrap(AppConfig.class, initArguments);
        }
    }

    /**
     * Default provider for the EmbeddingsAPI
     */
    public static class DefaultEmbeddingsAPIProvider implements EmbeddingsAPIProvider {

        @Override
        public EmbeddingsAPI getEmbeddingsAPI(final Object... initArguments) {
            return new EmbeddingsAPIImpl(unwrap(initArguments));
        }

        private Host unwrap(final Object... initArguments) {
            return DotAIAPIFacadeImpl.unwrap(Host.class, initArguments);
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
     * Set the default embeddings API Provider.
     * @param embeddingsAPI
     */
    public static final void setDefaultEmbeddingsAPIProvider(final EmbeddingsAPIProvider embeddingsAPI) {
        embeddingsProviderMap.put(DEFAULT, embeddingsAPI);
    }

    /**
     * Set the default image API Provider.
     * @param imageAPIProvider
     */
    public static final void setDefaultImageAPIProvider(final ImageAPIProvider imageAPIProvider) {
        imageProviderMap.put(DEFAULT, imageAPIProvider);
    }

    /**
     * Set the default chat API Provider.
     * @param chatAPIProvider
     */
    public static final void setDefaultChatAPIProvider(final ChatAPIProvider chatAPIProvider) {
        chatProviderMap.put(DEFAULT, chatAPIProvider);
    }

    /**
     * Adds completions API provider.
     * @param completionsAPI
     */
    public static final void addCompletionsAPIImplementation(final String apiName, final CompletionsAPIProvider completionsAPI) {
        completionsProviderMap.put(apiName, completionsAPI);
    }

    /**
     * Adds default embeddings API provider.
     * @param embeddingsAPI
     */
    public static final void addEmbeddingsAPIImplementation(final String apiName, final EmbeddingsAPIProvider embeddingsAPI) {
        embeddingsProviderMap.put(apiName, embeddingsAPI);
    }

    /**
     * Adds default chat API provider.
     * @param chatAPI
     */
    public static final void addChatAPIImplementation(final String apiName, final ChatAPIProvider chatAPI) {
        chatProviderMap.put(apiName, chatAPI);
    }

    /**
     * Adds default image API provider.
     * @param imageAPI
     */
    public static final void addImageAPIImplementation(final String apiName, final ImageAPIProvider imageAPI) {
        imageProviderMap.put(apiName, imageAPI);
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
