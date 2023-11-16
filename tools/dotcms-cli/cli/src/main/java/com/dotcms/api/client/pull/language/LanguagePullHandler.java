package com.dotcms.api.client.pull.language;

import com.dotcms.api.client.pull.PullHandler;
import com.dotcms.model.language.Language;
import javax.enterprise.context.Dependent;

@Dependent
public class LanguagePullHandler implements PullHandler<Language> {

    @Override
    public String title() {
        return "Languages";
    }

    @Override
    public String displayName(final Language language) {
        return language.isoCode();
    }

    @Override
    public String fileName(final Language language) {
        return language.isoCode();
    }

    @Override
    public String shortFormat(final Language language) {

        return String.format(
                "language: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] code: [@|bold,underline,green %s|@] country:[@|bold,yellow %s|@] countryCode: [@|bold,yellow %s|@] isoCode: [@|bold,yellow %s|@]",
                language.language().orElse(""),
                language.id().isPresent() ? language.id().get() : "",
                language.languageCode().orElse(""),
                language.country().orElse(""),
                language.countryCode().orElse(""),
                language.isoCode()
        );
    }

}
