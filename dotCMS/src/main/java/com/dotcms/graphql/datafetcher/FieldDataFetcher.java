package com.dotcms.graphql.datafetcher;

import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.isFieldRenderable;
import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.renderFieldValue;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vavr.control.Try;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FieldDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();

            Logger.debug(this, ()-> "Fetching field for contentlet: " + contentlet.getIdentifier() + " field: " + var);
            Object fieldValue = contentlet.get(var);

            final ContentType contentType = contentlet.getContentType();
            if (contentType == null) {
                Logger.debug(this, ()-> "No ContentType on contentlet " + contentlet.getIdentifier() + ", returning raw value");
                return fieldValue;
            }
            final Field field = contentType.fieldMap().get(var);

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

            final boolean renderField = Try.of(()-> (boolean) environment.getArgument("render"))
                    .getOrElse(false);

            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            return renderField && isFieldRenderable(field)
                    ? renderFieldValue(request, response, (String) fieldValue, contentlet, field.variable())
                    : fieldValue;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
