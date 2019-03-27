package com.dotmarketing.portlets.languagesmanager.transform;

import static com.dotcms.util.ConversionUtils.toLong;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    private static Language transform(final Map<String, Object> resultSet)  {
        final long id               = toLong(resultSet.get("id"), 0L);
        final String langCode       = (resultSet.get("language_code")!=null)    ? String.valueOf(resultSet.get("language_code")): null;
        final String countryCode    = (resultSet.get("country_code")!=null)     ? String.valueOf(resultSet.get("country_code")) : null;
        final String language       = (resultSet.get("language")!=null)         ? String.valueOf(resultSet.get("language"))     : null;
        final String country        = (resultSet.get("country")!=null)          ? String.valueOf(resultSet.get("country"))      : null;
        return new Language(id, langCode, countryCode, language, country);
    }

}
