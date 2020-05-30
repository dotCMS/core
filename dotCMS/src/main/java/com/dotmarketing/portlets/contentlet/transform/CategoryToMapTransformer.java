package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Map;

/**
 * FieldsToMapTransformer that converts contentlet objects into Maps
 */
public class CategoryToMapTransformer implements FieldsToMapTransformer {
    final Map<String, Object> mapOfMaps;

    public CategoryToMapTransformer(final Contentlet con, final User user) {
        if (con.getInode() == null) {
            throw new DotStateException("Contentlet needs an inode to get fields");
        }
        final DotTransformer transformer = new DotTransformerBuilder().categoryToMapTransformer().forUser(user)
                .content(con).build();

        this.mapOfMaps = transformer.toMaps().get(0);
    }

    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }

}

