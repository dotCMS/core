package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/** DBTransformer that converts DB objects into Contentlet instances */
public class LanguageToMapTransformer implements FieldsToMapTransformer {
  final Map<String, Object> mapOfMaps;

  public LanguageToMapTransformer(final Contentlet con) {
    if (con.getInode() == null) {
      throw new DotStateException("Contentlet needs an inode to get fields");
    }

    final Map<String, Object> newMap = new HashMap<>();
    newMap.put("languageMap", this.transform(con));
    newMap.put("language", con.getLanguageId());
    this.mapOfMaps = newMap;
  }

  @Override
  public Map<String, Object> asMap() {
    return this.mapOfMaps;
  }

  @NotNull
  private Map<String, Object> transform(final Contentlet con) {

    final Map<String, Object> map = new HashMap<>();

    final Language language = APILocator.getLanguageAPI().getLanguage(con.getLanguageId());
    map.put("id", language.getId());
    map.put("language", language.getLanguage());
    map.put("languageCode", language.getLanguageCode());
    map.put("country", language.getCountry());
    map.put("countryCode", language.getCountryCode());
    final String iso =
        UtilMethods.isSet(language.getCountryCode())
            ? language.getLanguageCode() + "-" + language.getCountryCode()
            : language.getLanguageCode();
    map.put("isoCode", iso.toLowerCase());

    return map;
  }
}
