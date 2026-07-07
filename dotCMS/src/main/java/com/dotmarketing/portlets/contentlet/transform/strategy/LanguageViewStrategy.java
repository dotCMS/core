package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.AVOID_MAP_SUFFIX_FOR_VIEWS;
import static com.liferay.portal.language.LanguageUtil.getLiteralLocale;
import static com.liferay.util.StringPool.BLANK;

/**
 * Language view transformer strategy.
 * <p>
 * Maps a Contentlet's {@link Language} into the response Map used by the various content
 * transformer presets (e.g. {@code webAssetOptions}, {@code dotAssetOptions}) as well as
 * {@link HistoryViewStrategy}. Locales are allowed to be language-only (no country code), so
 * the {@code country}/{@code countryCode} entries default to an empty string rather than
 * {@code null} to avoid tripping Guava's {@link Builder}, which rejects null values.
 *
 * @author Fabrizzio Araya
 * @since Jun 11th, 2020
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
        map.putAll(mapLanguage(language, true, options));
        return map;
    }

    public static Map<String, Object> mapLanguage(final Language language, final boolean wrapAsMap) {
        return mapLanguage(language, wrapAsMap, Collections.emptySet());
    }

    /**
     * Maps a {@link Language} into a Map of view-friendly properties (id, language/country names
     * and codes, ISO code, flag). Since a Language may be defined with only a language code and
     * no country (e.g. {@code es}), {@code country} and {@code countryCode} default to an empty
     * string when unset rather than {@code null} — a null value would blow up the underlying
     * {@link Builder}, which does not accept null entries.
     *
     * @param language  the Language to map; its {@code country}/{@code countryCode} may be null
     * @param wrapAsMap if {@code true}, wraps the result under a {@code language}/{@code languageMap}
     *                  key (see {@code AVOID_MAP_SUFFIX_FOR_VIEWS}); if {@code false}, returns the
     *                  properties unwrapped
     * @param options   the active {@link TransformOptions}, used to decide the wrapper key suffix
     * @return a Map of the language's view properties
     */
    public static Map<String, Object> mapLanguage(final Language language, final boolean wrapAsMap,
            final Set<TransformOptions> options) {

        final Builder<String, Object> builder = new Builder<>();

        builder
                .put("languageId", language.getId())
                .put("language", language.getLanguage())
                .put("languageCode", language.getLanguageCode())
                .put("country", CollectionsUtils.orElseGet(language.getCountry(), BLANK))
                .put("countryCode", CollectionsUtils.orElseGet(language.getCountryCode(), BLANK))
                .put("languageFlag", getLiteralLocale(language.getLanguageCode(), language.getCountryCode()));

        final String iso = UtilMethods.isSet(language.getCountryCode())
                ? language.getLanguageCode() + StringPool.DASH + language.getCountryCode()
                : language.getLanguageCode();
        builder.put("isoCode", iso.toLowerCase());

        if(wrapAsMap){
            builder.put("id", language.getId());
            final String suffix = options.contains(AVOID_MAP_SUFFIX_FOR_VIEWS)
                    ? "" : "Map";
            return ImmutableMap.of("language" +  suffix, builder.build());
        }
        return builder.build();
    }
}
