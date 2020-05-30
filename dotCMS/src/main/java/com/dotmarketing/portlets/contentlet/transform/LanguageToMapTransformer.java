package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * FieldsToMapTransformer that adds the language info on the Contentlet instances into a map
 */
public class LanguageToMapTransformer implements FieldsToMapTransformer {

    final Map<String, Object> mapOfMaps;

    public LanguageToMapTransformer(final Contentlet con) {
        if (con.getInode() == null) {
            throw new DotStateException("Contentlet needs an inode to get fields");
        }

        final List<Map<String, Object>> maps =
                new DotTransformerBuilder().languageToMapTransformer().content(con).build().toMaps();
        if (maps.isEmpty()) {
            this.mapOfMaps = Collections.emptyMap();
        } else {
            this.mapOfMaps = maps.get(0);
        }
    }

    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }

}

