package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.strategy.BinaryViewStrategy;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * FieldsToMapTransformer that converts contentlets with any binary fields into a Map
 */
public class BinaryToMapTransformer implements FieldsToMapTransformer {

    final Map<String, Object> mapOfMaps;

    public BinaryToMapTransformer(final Contentlet contentlet) {

        if (contentlet.getInode() == null) {
            throw new DotStateException("Contentlet needs an inode to get fields");
        }

        final List<Map<String, Object>> maps =
                new DotTransformerBuilder().binaryToMapTransformer().content(contentlet).build().toMaps();

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


    @NotNull
    public static Map<String, Object> transform(final Field field, final Contentlet con) {
        return BinaryViewStrategy.transform(field, con);
    }

    public static Map<String, Object> transform(final File file, final Contentlet con,
            final Field field) {
        return BinaryViewStrategy.transform(file, con, field);
    }
}

