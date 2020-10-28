package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Map;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class FolderToMapTransformer implements FieldsToMapTransformer {
    final Map<String, Object> mapOfMaps;



    public FolderToMapTransformer(final Contentlet con, final User user) {

        final DotMapViewTransformer transformer = new DotFolderTransformerBuilder().withFolders(con.getFolder()).build();
        mapOfMaps = transformer.toMaps().get(0);

    }

    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }



}

