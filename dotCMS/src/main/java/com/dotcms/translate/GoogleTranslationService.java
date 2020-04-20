package com.dotcms.translate;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.viewtools.JSONTool;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.http.HttpServletRequest;

public class GoogleTranslationService extends AbstractTranslationService {

    private JSONTool jsonTool;
    public static final String BASE_URL = "https://www.googleapis.com/language/translate/v2";
    private String serviceUrl = Config.getStringProperty("GOOGLE_TRANSLATE_SERVICE_BASE_URL",
        BASE_URL);
    private String apiKey;
    private List<ServiceParameter> params;

    public GoogleTranslationService() {
        this(Config.getStringProperty("GOOGLE_TRANSLATE_SERVICE_API_KEY", ""), new JSONTool(), new ApiProvider());
    }

    public GoogleTranslationService(String apiKey, JSONTool jsonTool, ApiProvider apiProvider) {
        this.apiKey = apiKey;
        this.jsonTool = jsonTool;
        this.apiProvider = apiProvider;
        this.params = Collections.singletonList(new ServiceParameter("apiKey", "Service API Key", apiKey));
    }

    @VisibleForTesting
    protected GoogleTranslationService(JSONTool jsonTool) {
        this(Config.getStringProperty("GOOGLE_TRANSLATE_SERVICE_API_KEY", ""), jsonTool, new ApiProvider());
    }

    @VisibleForTesting
    protected GoogleTranslationService(ApiProvider apiProvider) {
        this(Config.getStringProperty("GOOGLE_TRANSLATE_SERVICE_API_KEY", ""), new JSONTool(), apiProvider);
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
            Logger.info(this.getClass(), "translating:" + restURL);
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
    public void setServiceParameters(final List<ServiceParameter> params, final Optional<String> hostOpt) {
        this.params = params;
        Map<String, ServiceParameter> paramsMap = params.stream().collect(
            Collectors.toMap(ServiceParameter::getKey, Function.identity()));
        String apiKeyValue = paramsMap.get("apiKey").getValue();

        this.apiKey = !Strings.isNullOrEmpty(apiKeyValue)
            ?apiKeyValue
            :this.getFallbackApiKey(hostOpt);
    }


    private String getFallbackApiKey (final Optional<String> hostIdOpt) {

        final String appsTranslationKey    = Config.getStringProperty("apps_translation_key", "app-translation");
        final String appsTranslationApiKey = Config.getStringProperty("apps_translation_apikey", "apiKey");

        Optional<AppSecrets> appSecretsOpt = Optional.empty();
        try {
            appSecretsOpt = APILocator.getAppsAPI().getSecrets
                    (appsTranslationKey, true, this.resolveHost(hostIdOpt), APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage());
        }

        if (appSecretsOpt.isPresent()) {

            final Secret secret = appSecretsOpt.get().getSecrets().get(appsTranslationApiKey);
            if (null != secret) {
                return secret.getString();
            }
        }

        return Config.getStringProperty("GOOGLE_TRANSLATE_SERVICE_API_KEY", StringPool.BLANK);
    }

    private Host resolveHost (final Optional<String> hostIdOpt) {

        Host host = hostIdOpt.isPresent()?
                Try.of(() -> APILocator.getHostAPI().find(hostIdOpt.get(), APILocator.systemUser(),false)).getOrNull():null;

        if (null == host) {

            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if (null != request) {
                host = Try.of(() -> WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request)).getOrNull();
            }

            if (null == host) {
                host = Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)).getOrNull();
            }
        }

        return null == host? APILocator.systemHost(): host;
    }

}
