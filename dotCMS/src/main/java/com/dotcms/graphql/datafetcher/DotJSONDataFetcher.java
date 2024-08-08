package com.dotcms.graphql.datafetcher;

import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_CODE_FIELD_VAR;
import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.parseAsJSON;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link DataFetcher} that fetches the data when the special <code>widgetCodeJSON<code/> GraphQL Field is requested.
 * <p>
 * The field takes the follow arguments:
 * <ul>
  * <li>render: {@link Scalars#GraphQLBoolean} that indicates whether to velocity-render the rederable fields.
 * </ul>
 */

public class DotJSONDataFetcher implements DataFetcher<Object> {

    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final boolean render = environment.getArgument("render") == null
                    || (Boolean) environment.getArgument("render");

            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            final String fieldValue = (String) contentlet.get(WIDGET_CODE_FIELD_VAR);

            Logger.debug(this, ()-> "Fetching widget code JSON for contentlet: " + contentlet.getIdentifier());
            if(render) {
                final Object renderedValue = parseAsJSON(request, response, fieldValue,
                        contentlet, WIDGET_CODE_FIELD_VAR);

                return UtilMethods.isSet(renderedValue) ? renderedValue : new HashMap<>();
            } else {
                return fieldValue;
            }

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
