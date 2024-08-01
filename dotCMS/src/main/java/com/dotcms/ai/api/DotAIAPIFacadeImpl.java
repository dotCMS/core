package com.dotcms.ai.api;

import com.dotcms.ai.app.AppConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;
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

    private static final AtomicReference<String> currentApiProviderName = new AtomicReference<>("default");
    private static final Map<String, CompletionsAPIProvider> completionsProviderMap = new ConcurrentHashMap<>();
    private static final Map<String, EmbeddingsAPIProvider> embeddingsProviderMap   = new ConcurrentHashMap<>();

    static {
        try {
            completionsProviderMap.put("default", new DefaultCompletionsAPIProvider());
            embeddingsProviderMap.put("default", new DefaultEmbeddingsAPIProvider());
        } catch (Exception e) {
            Logger.error(DotAIAPI.class, e.getMessage(), e);
        }
    }

    private static class DefaultCompletionsAPIProvider implements CompletionsAPIProvider {

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

    private static class DefaultEmbeddingsAPIProvider implements EmbeddingsAPIProvider {

        private final EmbeddingsAPI defaultCompletionAPI = new EmbeddingsAPIImpl(null);
        @Override
        public EmbeddingsAPI getEmbeddingsAPI(final Object... initArguments) {
            return Objects.nonNull(initArguments) && initArguments.length > 0?
                    new EmbeddingsAPIImpl(unwrap(initArguments)):
                    defaultCompletionAPI;
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
        completionsProviderMap.put("default", completionsAPI);
    }

    /**
     * Adds the default embeddings API Provider.
     * @param embeddingsAPI
     */
    public static final void setDefaultEmbeddingsAPIProvider(final EmbeddingsAPIProvider embeddingsAPI) {
        embeddingsProviderMap.put("default", embeddingsAPI);
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
    public CompletionsAPI getCompletionsAPI(Object... initArguments) {

        return completionsProviderMap.get(currentApiProviderName.get()).getCompletionsAPI(initArguments);
    }

    @Override
    public EmbeddingsAPI getEmbeddingsAPI(Object... initArguments) {
        return embeddingsProviderMap.get(currentApiProviderName.get()).getEmbeddingsAPI(initArguments);
    }
}
