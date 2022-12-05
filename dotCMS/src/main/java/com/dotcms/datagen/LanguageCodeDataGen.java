package com.dotcms.datagen;

import java.util.Locale;
import java.util.Random;

/**
 * Generater a language code
 * @author jsanca
 */
public class LanguageCodeDataGen implements DataGen<String> {

    final String[] languageCodes = Locale.getISOLanguages();
    final Random   random        = new Random();

    @Override
    public String next() {

        final int    languageIndex = this.random.nextInt(languageCodes.length);
        final String languageCode  = this.languageCodes[languageIndex];
        return languageCode;
    }

    @Override
    public String persist(String object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String nextPersisted() {
        throw new UnsupportedOperationException();
    }
}
