package com.dotmarketing.portlets.contentlet.transform.poc;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
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

        Language l = APILocator.getLanguageAPI().getLanguage(con.getLanguageId());
        map.put("id", l.getId());
        map.put("language", l.getLanguage());
        map.put("languageCode", l.getLanguageCode());
        map.put("country", l.getCountry());
        map.put("countryCode", l.getCountryCode());
        String iso  = UtilMethods.isSet(l.getCountryCode())
                ?  l.getLanguageCode() + "-" + l.getCountryCode() 
                : l.getLanguageCode();
        map.put("isoCode", iso.toLowerCase());
        

        return map;
    }
}

