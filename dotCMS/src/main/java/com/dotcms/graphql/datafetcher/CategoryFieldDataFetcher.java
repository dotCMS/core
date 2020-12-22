package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.CategoryToMapTransformer;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class CategoryFieldDataFetcher implements DataFetcher<List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();

            final CategoryToMapTransformer transformer = new CategoryToMapTransformer(contentlet, user);
            return transformer.asMap().get("var") == null ? null : (List<Map<String, Object>>) ((Map) transformer.asMap().get(var)).get("categories");
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
