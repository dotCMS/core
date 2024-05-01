package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Language}
 */
public class LanguageAssertionChecker implements AssertionChecker<Language> {
    @Override
    public Map<String, Object> getFileArguments(final Language language, final File file) {
        return Map.of(
            "id",language.getId(),
            "code", language.getLanguageCode(),
            "country_code", language.getCountryCode(),
            "language", language.getLanguage(),
            "country", language.getCountry()
        );
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/language/language.language.xml";
    }

    @Override
    public List<File> getFile(final Language language, final File bundleRoot) {
        try {
            return list(
                    FileBundlerTestUtil.getLanguageFilePath(language, bundleRoot),
                    FileBundlerTestUtil.getLanguageVariableFilePath(language, bundleRoot)
            );
        } catch (DotSecurityException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }
}
