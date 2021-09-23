package com.dotmarketing.portlets.contentlet.business.json.transfomer;

import com.dotcms.contenttype.model.field.DateField;
import com.dotmarketing.portlets.contentlet.business.json.JsonFieldTransformer;
import com.dotmarketing.portlets.contentlet.business.json.types.DateSerializer;
import com.dotmarketing.portlets.contentlet.business.json.types.SerializerProvider;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang.time.FastDateFormat;

public class DateFieldTransformer implements JsonFieldTransformer<DateField, Date> {

    @Override
    public Map<String, Serializable> toJsonMap(final DateField field, final Date object) {

        return ImmutableMap.of(field.variable(),
               ImmutableMap.of(type(field), SerializerProvider.INSTANCE.get().getSerializer(Date.class).write(object))
        );

    }
}
