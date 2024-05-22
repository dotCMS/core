package com.dotcms.api.client.pull.language;

import com.dotcms.api.client.pull.GeneralPullHandler;
import com.dotcms.api.client.util.NamingUtils;
import com.dotcms.model.language.Language;
import com.dotcms.model.pull.PullOptions;
import java.util.List;
import jakarta.enterprise.context.Dependent;

@Dependent
public class LanguagePullHandler extends GeneralPullHandler<Language> {

    @Override
    public String title() {
        return "Languages";
    }

    @Override
    public String startPullingHeader(final List<Language> contents) {

        return String.format("\r@|bold,green [%d]|@ %s to pull",
                contents.size(),
                title()
        );
    }

    @Override
    public String displayName(final Language language) {
        return language.isoCode();
    }

    @Override
    public String fileName(final Language language) {
        return NamingUtils.languageFileName(language);
    }

    @Override
    public String shortFormat(final Language language, final PullOptions pullOptions) {

        return String.format(
                "isoCode: [@|bold,yellow %s|@]",
                language.isoCode()
        );
    }

}
