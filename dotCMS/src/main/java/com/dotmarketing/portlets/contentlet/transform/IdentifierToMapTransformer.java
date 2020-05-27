package com.dotmarketing.portlets.contentlet.transform;

import static java.util.Collections.emptyMap;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;
import java.util.Map;

/**
 * ContentletTransformer that converts Contentlet objects into Map instances.
 */
public class IdentifierToMapTransformer implements FieldsToMapTransformer {

    private final Map<String, Object> mapOfMaps;

    public IdentifierToMapTransformer(final Contentlet con) {
        if (con.getInode() == null || con.getIdentifier() == null) {
            throw new DotStateException("Contentlet needs an identifier to get properties");
        }

        final List<Map<String, Object>> maps = //new ContentletToMapTransformer2(EnumSet.of(IDENTIFIER_AS_MAP), Collections.singletonList(con)).toMaps();
                new DotContentletTransformer.Builder().identifierToMapTransformer().content(con).build().toMaps();
        if (!maps.isEmpty()) {
            this.mapOfMaps = maps.get(0);
        } else {
            this.mapOfMaps = emptyMap();
        }
    }

    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }


}

