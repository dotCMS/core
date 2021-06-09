package com.dotcms.graphql.datafetcher;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.RENDER_FIELDS;

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
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContentMapDataFetcher implements DataFetcher<Object> {

    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String key = environment.getArgument("key");
            final int depth = environment.getArgument("depth");
            final Boolean render = environment.getArgument("render");

            if(UtilMethods.isSet(key)) {
                return contentlet.get(key);
            }

            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            final User user = ((DotGraphQLContext) environment.getContext()).getUser();

            final DotTransformerBuilder transformerBuilder = new DotTransformerBuilder();

            if(render) {
                transformerBuilder.hydratedContentMapTransformer(RENDER_FIELDS);
            } else {
                transformerBuilder.hydratedContentMapTransformer();
            }

            final DotContentletTransformer myTransformer = transformerBuilder.content(contentlet).build();

            final Map<String, Object> hydratedMap =  myTransformer.toMaps().get(0);
            final JSONObject contentMapInJSON = new JSONObject();

            // this only adds relationships to any json. We would need to return them with the transformations already

            final JSONObject jsonWithRels = ContentResource.addRelationshipsToJSON(request, response,
                    render.toString(), user, depth, false, contentlet,
                    contentMapInJSON, null, 1, true, false,
                    true);

            HashMap<String,Object> result = new ObjectMapper().readValue(jsonWithRels.toString(), HashMap.class);
            hydratedMap.putAll(result);

            return hydratedMap;

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
