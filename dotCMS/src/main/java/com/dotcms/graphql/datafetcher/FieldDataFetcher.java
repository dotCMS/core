package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FieldDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();

        Object fieldValue = contentlet.get(var);

        if(!UtilMethods.isSet(fieldValue)) {
            // null value - maybe a constant field
            final Field field = contentlet.getContentType().fieldMap().get(var);

            if(field instanceof ConstantField) {
                fieldValue = field.values();
            } else if(field instanceof TextField && field.dataType().equals(DataTypes.INTEGER)) {
                fieldValue = Integer.parseInt(field.values());
            } else if(field instanceof TextField && field.dataType().equals(DataTypes.FLOAT)) {
                fieldValue = Float.parseFloat(field.values());
            }
        }

        return fieldValue;
    }
}
