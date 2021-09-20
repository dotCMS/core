package com.dotmarketing.portlets.contentlet.business.json.transfomer;


import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.portlets.contentlet.business.json.JsonFieldTransformer;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;

public class TextFieldTransformer implements JsonFieldTransformer<TextField, Serializable> {

    @Override
    public Map<String, Serializable> toJsonMap(TextField field, Serializable object) {
        return ImmutableMap.of(field.variable(),
               ImmutableMap.of(type(field), object)
        );
    }
}
