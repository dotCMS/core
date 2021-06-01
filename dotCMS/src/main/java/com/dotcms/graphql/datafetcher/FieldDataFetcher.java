package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vavr.control.Try;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

            final boolean renderField = Try.of(()-> (boolean) environment.getArgument("render"))
                    .getOrElse(false);

            return renderField && isFieldRenderable(field)
                    ? renderFieldValue(environment, fieldValue, contentlet)
                    : fieldValue;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    private Object renderFieldValue(final DataFetchingEnvironment environment,
            final Object fieldValue,
            final Contentlet contentlet) {
        final String fieldValueAsStr = fieldValue.toString();

        final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                .getHttpServletRequest();

        final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                .getHttpServletResponse();

        final org.apache.velocity.context.Context context = VelocityUtil
                .getInstance().getContext(request, response);

        context.put("content", contentlet);
        context.put("contentlet", contentlet);

        final StringWriter evalResult = new StringWriter();

        com.dotmarketing.util.VelocityUtil
                .getEngine()
                .evaluate(context, evalResult, "", fieldValueAsStr);

        return evalResult.toString();
    }

    private boolean isFieldRenderable(final Field field) {
        return field instanceof WysiwygField || field instanceof TextField ||
                field instanceof TextAreaField || field instanceof CustomField
                || field instanceof ConstantField;
    }
}