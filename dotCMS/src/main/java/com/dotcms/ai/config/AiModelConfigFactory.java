package com.dotcms.ai.config;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.config.parser.AiModelConfigParser;
import com.dotcms.ai.config.parser.AiVendorCatalogData;
import com.dotcms.business.SystemCache;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory to retrieve the AiModelConfig
 * @author jsanca
 */
@ApplicationScoped
public class AiModelConfigFactory {

    private final AiModelConfigParser modelConfigParser  = new AiModelConfigParser();
    private final AiModelConfig configCache404 = new AiModelConfig("404", Map.of());
    // todo: we need a way to invalidate this cache
    private final ConcurrentHashMap<String, AiModelConfigCatalog> aiModelConfigCatalogMap = new ConcurrentHashMap<>();
    private final Secret unknownSecret = Secret.builder().withValue("UNKNOWN").withType(Type.STRING).build();

    public Optional<AiModelConfig> getAiModelConfig(final Host site, final String vendorModelPath) {

        return getAiModelConfig(site.getIdentifier(), vendorModelPath);
    }

    public Optional<AiModelConfig> getAiModelConfig(final String siteId, final String vendorModelPath) {

        final boolean useDefaultModelWhenNotFound = Config.getBooleanProperty("AI_USE_DEFAULT_CHAT_MODEL_FALLBACK", true);
        return getAiModelConfig(siteId, vendorModelPath, useDefaultModelWhenNotFound);
    }

    private Optional<AiModelConfig> getAiModelConfig(final String siteId, final String vendorModelPath, boolean useDefaultModelWhenNotFound) {

        final SystemCache systemCache = CacheLocator.getSystemCache();
        final AiModelConfig configFromCache = (AiModelConfig) systemCache.get(key(siteId, vendorModelPath));
        if (null == configFromCache) {

            final AiModelConfig configFromApp = Try.of(()->findAiModelFromApp(siteId, vendorModelPath)).getOrNull();
            if (null != configFromApp) {

                systemCache.put(key(siteId, vendorModelPath), configFromApp);
                return Optional.ofNullable(configFromApp);
            } else {
                systemCache.put(key(siteId, vendorModelPath), configCache404);
            }
        }

        if (configFromCache == configCache404 || null == configFromCache) {

            // try the default model from routing
            if (useDefaultModelWhenNotFound) {
                final AiModelConfig configFromApp = Try.of(() -> findDefaultAiModelFromRoutingApp(siteId)).getOrNull();

                return Optional.ofNullable(configFromApp);
            }

            return Optional.empty();
        }

        return Optional.ofNullable(configFromCache);
    }

    private AiModelConfig findAiModelFromApp(final String siteId, final String vendorModelPath) throws DotDataException, DotSecurityException {

        AiModelConfigCatalog modelConfigCatalog = null;
        if (!this.aiModelConfigCatalogMap.containsKey(siteId)) {

            if (!loadVendorModelFromAppBySiteId(siteId)) {
                return null;
            }
        }

        modelConfigCatalog = this.aiModelConfigCatalogMap.get(siteId);

        return null != modelConfigCatalog? modelConfigCatalog.getByPath(vendorModelPath):null;
    }

    private AiModelConfig findDefaultAiModelFromRoutingApp(final String siteId) throws DotDataException, DotSecurityException {

        AiModelConfigCatalog modelConfigCatalog = null;
        if (!this.aiModelConfigCatalogMap.containsKey(siteId)) {

            if (!loadVendorModelFromAppBySiteId(siteId)) {
                return null;
            }
        }

        modelConfigCatalog = this.aiModelConfigCatalogMap.get(siteId);


        return null != modelConfigCatalog? modelConfigCatalog.getDefaultChatModel():null;
    }

    private boolean loadVendorModelFromAppBySiteId(final String siteId) throws DotDataException, DotSecurityException {

        AiModelConfigCatalog modelConfigCatalog;
        final boolean fallbackOnSystemHost = true;
        final User systemUser = APILocator.systemUser();
        final Host site = Try.of(()->APILocator.getHostAPI().find(siteId, systemUser, // todo: check if system is ok or should be current user
                false)).getOrElse(APILocator.systemHost());
        final Optional<AppSecrets> optionalAppSecrets = APILocator.getAppsAPI().getSecrets(AppKeys.APP_KEY,
                fallbackOnSystemHost, site, systemUser);
        if (optionalAppSecrets.isPresent()) {

            final String aiJsonConfiguration = optionalAppSecrets.get().getSecrets().get(AppKeys.ADVANCE_PROVIDER_SETTINGS_KEY.key).getString();
            if (!StringUtils.isSet(aiJsonConfiguration)) {

                Logger.error(this, AppKeys.ADVANCE_PROVIDER_SETTINGS_KEY.key + " is not set");
                return false;
            }

            final Map<String, Secret> secretsMap = optionalAppSecrets.get().getSecrets();
            final AiVendorCatalogData vendorCatalogData = modelConfigParser.parse(aiJsonConfiguration,
                    (key) -> {
                        // default ValueResolver impl, based on dotCMS config and context (the context encapsulates the dotAI Secrets App
                        return Config.getStringProperty(key, secretsMap.getOrDefault(key, this.unknownSecret).getString());
                    });

            modelConfigCatalog = AiModelConfigCatalogImpl.from(vendorCatalogData);
            this.aiModelConfigCatalogMap.put(siteId, modelConfigCatalog);
        }
        return true;
    }

    private String key(final String siteId, final String vendorModelPath) {
        return "ai_model_config"+siteId +"_"+vendorModelPath;
    }

    public Optional<AiModelConfig> getAiModelConfigOrDefaultChat(final Host site, final String vendorModelPath) {

        final boolean useDefaultModelWhenNotFound = true;
        return getAiModelConfig(site.getIdentifier(), vendorModelPath, useDefaultModelWhenNotFound);
    }
}
