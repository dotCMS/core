package com.dotcms.translate;

import com.dotcms.rendering.velocity.viewtools.JSONTool;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;


public class GoogleTranslationService extends AbstractTranslationService {

    private JSONTool jsonTool;
    public static final String BASE_URL = "https://translation.googleapis.com/language/translate/v2";
    private String serviceUrl = Config.getStringProperty("GOOGLE_TRANSLATE_SERVICE_BASE_URL",
        BASE_URL);
    private String apiKey;
    private List<ServiceParameter> params;
    private static final String GOOGLE_TRANSLATE_APP_CONFIG_KEY = "googleTranslate-config";
    private static final String API_KEY_VAR = "apiKey";
    private static final String GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY = "GOOGLE_TRANSLATE_SERVICE_API_KEY";

    public GoogleTranslationService() {
        this(Config.getStringProperty(GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY, StringPool.BLANK), new JSONTool(), new ApiProvider());
    }

    public GoogleTranslationService(String apiKey, JSONTool jsonTool, ApiProvider apiProvider) {
        this.apiKey = apiKey;
        this.jsonTool = jsonTool;
        this.apiProvider = apiProvider;
        this.params = Collections.singletonList(new ServiceParameter(API_KEY_VAR, "Service API Key", apiKey));
    }

    @VisibleForTesting
    protected GoogleTranslationService(JSONTool jsonTool) {
        this(Config.getStringProperty(GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY, StringPool.BLANK), jsonTool, new ApiProvider());
    }

    @VisibleForTesting
    protected GoogleTranslationService(ApiProvider apiProvider) {
        this(Config.getStringProperty(GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY, StringPool.BLANK), new JSONTool(), apiProvider);
    }

    private static class Holder {
        private static final GoogleTranslationService INSTANCE = new GoogleTranslationService();
    }

    public static GoogleTranslationService getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public String translateString(String toTranslate, Language from, Language to) throws TranslationException {
        return translateStrings(Collections.singletonList(toTranslate), from, to).get(0);
    }

    @Override
    public List<String> translateStrings(List<String> toTranslate, Language from, Language to)
        throws TranslationException {

        Preconditions.checkArgument(!Strings.isNullOrEmpty(apiKey), new DotStateException("No API Key Found."));

        Preconditions.checkNotNull(from, "'From' Language can't be null.");
        Preconditions.checkNotNull(to, "'To' Language can't be null.");
        Preconditions.checkArgument(from.getId() != to.getId(), "'From' and 'To' Languages must be different.");

        List<String> ret = new ArrayList<>();

        
        StringBuilder restURL = new StringBuilder().append(serviceUrl).append("?key=").append(apiKey);
        

        Map<String, Object> params  =new HashMap<>();
        params.put("q", toTranslate);
        params.put("source", from.getLanguageCode());
        params.put("target", to.getLanguageCode());
        

        try {
            Logger.debug(this.getClass(), "translating:" + restURL + "params: " + params.toString());
            JSONObject json = (JSONObject) jsonTool.post(restURL.toString(), 15000,ImmutableMap.of(), new JSONObject(params).toString());

            json = json.getJSONObject("data");
            JSONArray arr = json.getJSONArray("translations");

            for (int i = 0; i < arr.length(); i++) {
                String translatedText = (String) arr.getJSONObject(i).get("translatedText");
                ret.add(StringEscapeUtils.unescapeHtml(translatedText));
            }

            return ret;
        } catch (Exception e) {
            throw new TranslationException(e);
        }
    }

    @Override
    public List<ServiceParameter> getServiceParameters() {
        return Collections.unmodifiableList(params);
    }

    @Override
    public void setServiceParameters(final List<ServiceParameter> params, final String hostId) {
        this.params = params;
        Map<String, ServiceParameter> paramsMap = params.stream().collect(
            Collectors.toMap(ServiceParameter::getKey, Function.identity()));
        String apiKeyValue = paramsMap.get(API_KEY_VAR).getValue();

        this.apiKey = !Strings.isNullOrEmpty(apiKeyValue)
            ?apiKeyValue
            :this.getFallbackApiKey(hostId);
    }

    /**
     * Get the API Key from the APPS if is set, if not set fallback to the dotmarketing-config.properties
     *
     * @param hostId hostId of the contentlet to translate, to get the configuration from apps, if exists.
     * @return APIKey
     */
    private String getFallbackApiKey (final String hostId) {

        AppSecrets appSecrets = null;
        final Host host = Try.of(() -> APILocator.getHostAPI().find(hostId, APILocator.systemUser(),false)).getOrElse(APILocator.systemHost());
        try {
            appSecrets = APILocator.getAppsAPI().getSecrets
                    (GOOGLE_TRANSLATE_APP_CONFIG_KEY, true, host, APILocator.systemUser()).get();

            return appSecrets.getSecrets().containsKey(API_KEY_VAR) ?
                    appSecrets.getSecrets().get(API_KEY_VAR).getString() :
                    Config.getStringProperty(GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY, StringPool.BLANK);

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting the API Key from the Apps Service: " + e.getMessage());
            return Config.getStringProperty(GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY, StringPool.BLANK);
        } finally {
            if(UtilMethods.isSet(appSecrets)){
                appSecrets.destroy();
            }
        }

    }

}
