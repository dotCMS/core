package com.dotmarketing.portlets.languagesmanager.transform;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.ConversionUtils.toLong;

/**
 * This transformer takes the information of one or more Languages in dotCMS from the database, and
 * transforms it into a list of Language objects. This mechanism provides a standardized way of
 * loading Languages stored in the database into a Java object.
 *
 * @author Fabrizzio Araya
 * @since Nov 21th, 2018
 */
public class LanguageTransformer implements DBTransformer <Language> {

    private final List<Language> list;

    public LanguageTransformer(final List<Map<String, Object>> initList){
        List<Language> list = Collections.emptyList();
        if (initList != null){
            list = initList.stream().map(LanguageTransformer::transform).collect(CollectionsUtils.toImmutableList());
        }
        this.list = list;
    }

    @Override
    public List<Language> asList() {
        return this.list;
    }

    /**
     * Transforms a Map of key-value pairs into a Language object.
     *
     * @param resultSet The Map of key-value pairs to transform.
     *
     * @return A {@link Language} object.
     */
    @NotNull
    private static Language transform(final Map<String, Object> resultSet)  {
        final long id               = toLong(resultSet.get("id"), 0L);
        final String langCode       = (resultSet.get("language_code")!=null)    ? String.valueOf(resultSet.get("language_code")): null;
        final String countryCode    = (resultSet.get("country_code")!=null)     ? String.valueOf(resultSet.get("country_code")) : null;
        final String language       = (resultSet.get("language")!=null)         ? String.valueOf(resultSet.get("language"))     : null;
        final String country        = (resultSet.get("country")!=null)          ? String.valueOf(resultSet.get("country"))      : null;
        final String isoCode = StringUtils.isNotBlank(countryCode) ? langCode + "-" + countryCode : langCode;
        return new Language(id, langCode, countryCode, language, country, null != isoCode ? isoCode.toLowerCase() : null);
    }

}
