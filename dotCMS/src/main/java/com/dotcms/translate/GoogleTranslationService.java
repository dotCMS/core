package com.dotcms.translate;

import com.dotcms.rendering.velocity.viewtools.JSONTool;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;

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
    public void setServiceParameters(List<ServiceParameter> params) {
        this.params = params;
        Map<String, ServiceParameter> paramsMap = params.stream().collect(
            Collectors.toMap(ServiceParameter::getKey, Function.identity()));
        String apiKeyValue = paramsMap.get("apiKey").getValue();

        this.apiKey = !Strings.isNullOrEmpty(apiKeyValue)
            ?apiKeyValue
            :Config.getStringProperty("GOOGLE_TRANSLATE_SERVICE_API_KEY", "");
    }

}
