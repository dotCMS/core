package com.dotcms.contenttype.transform.contenttype;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class JsonContentTypeFieldLayoutTransformer extends JsonContentTypeTransformer {
    public JsonContentTypeFieldLayoutTransformer(ContentType type) {
        super(type);
    }

    public JsonContentTypeFieldLayoutTransformer(String json) {
        super(json);
    }

    public JsonContentTypeFieldLayoutTransformer(List<ContentType> list) {
        super(list);
    }


    @Override
    protected List<Field> getFields (final ContentType contentType)  {
        final FieldLayout fieldLayout = new FieldLayout(contentType.fields());
        return fieldLayout.getFields();
    }

    protected Object getFieldsEntryJsonObject (final ContentType contentType)  {
        return new JSONObject(getEntryMap(contentType));
    }

    private ImmutableMap<String, Object> getEntryMap(ContentType contentType) {
        final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();

        final FieldLayout fieldLayout = new FieldLayout(contentType.fields());
        builder.put("layout", fieldLayout.getRows());

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fieldLayout.getFields());
        builder.put("items", jsonFieldTransformer.mapList());
        return builder.build();
    }

    protected Object getFieldsEntryObject (final ContentType contentType)  {
        return this.getEntryMap(contentType);
    }

}
