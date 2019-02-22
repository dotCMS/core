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
import graphql.schema.GraphQLFieldDefinition;

import static com.dotcms.graphql.util.TypeUtil.BASE_TYPE_SUFFIX;

public class ContentletDataFetcher implements DataFetcher<List<Contentlet>> {
    @Override
    public List<Contentlet> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();

            String query = UtilMethods.isSet((String) environment.getArgument("query"))
                ? environment.getArgument("query")
                : "";

            final Integer limit = environment.getArgument("limit")!=null ? environment.getArgument("limit") : 100;
            final Integer offset = environment.getArgument("offset")!=null ? environment.getArgument("offset") : 0;
            final String sortBy = environment.getArgument("sortBy");

            final String queriedFieldName = environment.getField().getName();

            if(!queriedFieldName.equals("search")) {
                final boolean isBaseType = isBaseType(environment.getFieldDefinition());

                if(isBaseType) {
                    String typeName = TypeUtil.singularizeBaseTypeCollectionName(queriedFieldName);
                    query = "+baseType:" + BaseContentType.getBaseContentType(typeName.toUpperCase()).ordinal() + " " + query;
                } else {
                    final String typeName = TypeUtil.singularizeCollectionName(queriedFieldName);
                    query = "+contentType:" + TypeUtil.singularizeCollectionName(queriedFieldName) + " " + query;
                }
            }

            final List<Contentlet> contentletList = APILocator.getContentletAPI().search(query, limit, offset, sortBy,
                user, true);
            return new ContentletToMapTransformer(contentletList).hydrate();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    private boolean isBaseType(final GraphQLFieldDefinition fieldDefinition) {
        return UtilMethods.isSet(fieldDefinition.getDescription())
            && fieldDefinition.getDescription().equals(BASE_TYPE_SUFFIX);
    }
}
