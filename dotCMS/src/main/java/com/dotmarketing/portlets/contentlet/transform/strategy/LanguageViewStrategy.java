package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.liferay.portal.language.LanguageUtil.getLiteralLocale;

import com.dotcms.api.APIProvider;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Map;
import java.util.Set;

/**
 * Language view transformer strategy
 */
public class LanguageViewStrategy extends AbstractTransformStrategy<Contentlet>{

    /**
     * Main Constructor
     * @param toolBox
     */
    LanguageViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * transform method entry point
     * @param source
     * @param map
     * @param options
     * @param user
     * @return
     */
    @Override
    protected Map<String, Object> transform(final Contentlet source, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user) {

        final Language language = toolBox.languageAPI.getLanguage(source.getLanguageId());
        map.putAll(mapLanguage(language, true));
        return map;
    }

    /**
     * Lang functions now relocated here.
     * @param language
     * @param wrapAsMap
     * @return
     */
    public static Map<String, Object> mapLanguage(final Language language, final boolean wrapAsMap) {

        final Builder<String, Object> builder = new Builder<>();

        builder
                .put("languageId", language.getId())
                .put("language", language.getLanguage())
                .put("languageCode", language.getLanguageCode())
                .put("country", language.getCountry())
                .put("countryCode", language.getCountryCode())
                .put("languageFlag", getLiteralLocale(language.getLanguageCode(), language.getCountryCode()));

        final String iso = UtilMethods.isSet(language.getCountryCode())
                ? language.getLanguageCode() + StringPool.DASH + language.getCountryCode()
                : language.getLanguageCode();
        builder.put("isoCode", iso.toLowerCase());

        if(wrapAsMap){
            builder.put("id", language.getId());
            return ImmutableMap.of("languageMap", builder.build(), "language",language.getLanguage());
        }
        return builder.build();
    }
}
