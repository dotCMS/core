package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.business.WrapInTransaction;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;

import com.dotcms.datagen.AbstractDataGen;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import io.vavr.Tuple2;;

public class LanguageDataGen extends AbstractDataGen<Language> {

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
      if (Set.of("de", "en", "es", "fr", "fi", "nl", "ru", "zh", "ep", "sg").contains(language)) {
        continue;
      }
      country = RandomStringUtils.randomAlphabetic(2);
      count = new DotConnect().setSQL("select count(*) as test from language where language_code=? and country_code=?").addParam(language).addParam(country).getInt("test");

    }
    
    return new Tuple2<>(language, country);
    
  }

}
