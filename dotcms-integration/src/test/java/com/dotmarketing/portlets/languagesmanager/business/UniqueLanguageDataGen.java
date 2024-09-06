package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.datagen.AbstractDataGen;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.StringUtils;
import io.vavr.Tuple2;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;

public class UniqueLanguageDataGen extends AbstractDataGen<Language> {

  //These are the languages that we have identified as restricted cause they are used in the tests
  private static final Set<String> restrictedLanguageCodes = Set.of("de", "en", "es", "fr", "fi", "nl", "ru", "zh", "ep", "sg");

  @Override
  public Language next() {
    Tuple2<String, String> langCountry =  uniqueLangAndCountry();
    final Language lan = new Language();
    lan.setCountry(langCountry._2);
    lan.setCountryCode(langCountry._2.substring(0, 2));
    lan.setLanguageCode(langCountry._1.substring(0, 2));
    lan.setLanguage(langCountry._1);
    return lan;
  }

  @WrapInTransaction
  @Override
  public Language persist(final Language language) {
    APILocator.getLanguageAPI().saveLanguage(language);
    return APILocator.getLanguageAPI().getLanguage(language.getLanguageCode(), language.getCountryCode());
  }
  
  
  private Tuple2<String, String> uniqueLangAndCountry(){
    
    int count =1;
    
    String language = null;
    String country = null;
    
    while(count>0) {
      language = RandomStringUtils.randomAlphabetic(2);
      // Skip if the country is one of the ones that we already have
      if (isRestrictedLanguageCode(language)) {
        continue;
      }
      country = RandomStringUtils.randomAlphabetic(2);
      count = testLanguageExist(language, country);

    }

    return new Tuple2<>(language, country);

  }

  /**
   * Test if a language with that code exists
   * @param languageCode
   * @return
   */
  public static  boolean isRestrictedLanguageCode(final String languageCode) {
    return restrictedLanguageCodes.contains(languageCode.toLowerCase());
  }

  /**
   * Test if a language with that code and country exists
   * @param languageCode
   * @param countryCode
   * @return
   */
  public static int testLanguageExist(final String languageCode, final String countryCode) {

    if (!StringUtils.isSet(countryCode)) {
      return new DotConnect().setSQL(
              "select count(*) as test from language where lower(language_code)=lower(?) and country_code is null "
      ).addParam(languageCode).getInt("test");
    }

    return new DotConnect().setSQL(
            "select count(*) as test from language where lower(language_code)=lower(?) and lower(country_code)=lower(?)"
    ).addParam(languageCode).addParam(countryCode).getInt("test");

  }

}
