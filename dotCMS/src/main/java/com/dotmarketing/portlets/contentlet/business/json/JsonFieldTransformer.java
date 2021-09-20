package com.dotmarketing.portlets.contentlet.business.json;

import com.dotcms.contenttype.model.field.Field;
import java.io.Serializable;
import java.util.Map;
import org.apache.commons.text.CaseUtils;

public interface JsonFieldTransformer <T extends Field, V extends  Serializable>{

    Map<String, Serializable> toJsonMap(T field, V object);

    default String type(final Field field)  {
        return CaseUtils.toCamelCase(field.dataType() + " Value", true,' ');
    }

}
