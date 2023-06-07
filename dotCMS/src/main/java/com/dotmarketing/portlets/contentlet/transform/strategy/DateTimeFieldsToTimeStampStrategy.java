package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This strategy will transform all DateTime fields into Timestamps.
 * We had an issue where old ContentResource would show Datetime in an incorrect format.
 * The error was originated by the fields loaded from contentlet as json which are mapped into a Date object.
 * But the JSONObject formats Dates and TimeStamps differently.
 */
public class DateTimeFieldsToTimeStampStrategy extends AbstractTransformStrategy<Contentlet> {

    DateTimeFieldsToTimeStampStrategy(APIProvider toolBox) {
        super(toolBox);
    }

    @Override
    protected Map<String, Object> transform(Contentlet source, Map<String, Object> map,
                                            Set<TransformOptions> options, User user)
            throws DotDataException, DotSecurityException {
        final ContentType contentType = source.getContentType();
        convertFieldsToTimestamp(contentType.fields(TimeField.class), map);
        convertFieldsToTimestamp(contentType.fields(DateField.class), map);
        convertFieldsToTimestamp(contentType.fields(DateTimeField.class), map);
        return map;
    }

    /**
     * This method will convert all Date fields into Timestamps.
     * @param fields list of fields to convert
     * @param map map containing the values to convert
     */
    private void convertFieldsToTimestamp(final List<Field> fields, Map<String, Object> map){
        fields.forEach(field -> {
            final Object o = map.get(field.variable());
            if (o instanceof Date) {
                map.put(field.variable(), new Timestamp(((Date) o).getTime()));
            }
        });
    }

}
