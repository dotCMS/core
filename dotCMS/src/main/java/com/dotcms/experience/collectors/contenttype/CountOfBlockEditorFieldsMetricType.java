package com.dotcms.experience.collectors.contenttype;

import java.util.Map;

public class CountOfBlockEditorFieldsMetricType extends ContentTypeFieldsMetricType {
    boolean filterCondition(Map<String, Object> map) {
        return "com.dotcms.contenttype.model.field.StoryBlockField".equals(map.get("field_type"));
    }

    @Override
    public String getName() {
        return "COUNT_BLOCK_EDITOR_FIELDS";
    }

    @Override
    public String getDescription() {
        return "Count the number of block editor fields";
    }
}
