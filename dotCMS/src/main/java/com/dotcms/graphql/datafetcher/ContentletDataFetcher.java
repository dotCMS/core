package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.util.TypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class ContentletDataFetcher implements DataFetcher<List<Contentlet>> {
    @Override
    public List<Contentlet> get(final DataFetchingEnvironment environment) throws Exception {
        final User user = ((DotGraphQLContext) environment.getContext()).getUser();

        String query = UtilMethods.isSet((String) environment.getArgument("query"))
            ? environment.getArgument("query")
            : "";

        final Integer limit = environment.getArgument("limit")!=null ? environment.getArgument("limit") : 100;
        final Integer offset = environment.getArgument("offset")!=null ? environment.getArgument("offset") : 0;
        final String sortBy = environment.getArgument("sortBy");

        final String queriedFieldName = environment.getField().getName();

        if(!queriedFieldName.equals("search")) {
            final String typeName = TypeUtil.singularizeName(queriedFieldName);
            if(isBaseType(typeName)) {
                query = "+baseType:" + BaseContentType.getBaseContentType(typeName.toUpperCase()).ordinal() + " " + query;
            } else {
                query = "+contentType:" + TypeUtil.singularizeName(queriedFieldName) + " " + query;
            }
        }

        final List<Contentlet> contentletList = APILocator.getContentletAPI().search(query, limit, offset, sortBy,
            user, true);
        return new ContentletToMapTransformer(contentletList).hydrate();
    }

    private boolean isBaseType(final String queriedFieldName) {
        boolean isBaseType = false;

        try {
            isBaseType = UtilMethods.isSet(BaseContentType.getBaseContentType(queriedFieldName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            Logger.debug(this, "Not a BaseContentType: " + queriedFieldName, e);
        }

        return isBaseType;
    }
}
