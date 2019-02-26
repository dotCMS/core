package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FieldDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();

            Object fieldValue = contentlet.get(var);

            final Field field = contentlet.getContentType().fieldMap().get(var);

            if(!UtilMethods.isSet(fieldValue)) {
                // null value - maybe a constant field

                if(field instanceof ConstantField) {
                    fieldValue = field.values();
                } else if(field instanceof TextField && field.dataType().equals(DataTypes.INTEGER)) {
                    fieldValue = Integer.parseInt(field.values());
                } else if(field instanceof TextField && field.dataType().equals(DataTypes.FLOAT)) {
                    fieldValue = Float.parseFloat(field.values());
                } else if(UtilMethods.isSet(field)) {
                    fieldValue = field.defaultValue();
                }
            }

            return fieldValue;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
