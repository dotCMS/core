package com.dotcms.graphql.datafetcher;

import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_CODE_FIELD_VAR;
import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.isFieldRenderable;
import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.parseAsJSON;
import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.renderFieldValue;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.RENDER_FIELDS;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.ContentResource;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link DataFetcher} that fetches the data when the special <code>_map<code/> GraphQL Field is requested.
 * <p>
 * The field takes the follow arguments:
 * <ul>
 * <li>key: {@link Scalars#GraphQLString} that represents the variable name of a field of the contentlet. Using this argument makes the field to return the value of only that specific field from the
 * contentlet's map. If not specified it will return all the properties in the contentlet's map
 * <li>depth: {@link Scalars#GraphQLInt} value that specifies how to return the related content. Has the same behavior as the `depth` argument in the Content REST API for related content
 * <li>render: {@link Scalars#GraphQLBoolean} that indicates whether to velocity-render the rederable fields.
 * </ul>
 */

public class DotJSONDataFetcher implements DataFetcher<Object> {

    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final Boolean render = environment.getArgument("render");

            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            final String fieldValue = (String) contentlet.get(WIDGET_CODE_FIELD_VAR);

            final String renderedValue = (String) parseAsJSON(request, response, fieldValue,
                    contentlet, WIDGET_CODE_FIELD_VAR);

            return new ObjectMapper().readValue(renderedValue, HashMap.class);

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
