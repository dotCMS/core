package com.dotcms.graphql.datafetcher;

import static com.dotcms.graphql.business.GraphqlAPIImpl.TYPES_AND_FIELDS_VALID_NAME_REGEX;
import static com.dotcms.graphql.util.TypeUtil.BASE_TYPE_SUFFIX;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.util.TypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import java.util.List;
import java.util.stream.Collectors;

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

            final List<Contentlet> unfilteredContentletList = APILocator.getContentletAPI().search(query, limit, offset, sortBy,
                user, true);

            // filter out content whose content types don't stick to GraphQL naming convention
            final List<Contentlet> filteredContentletList = unfilteredContentletList.stream()
                .filter(contentlet -> contentlet.getContentType().variable().matches(TYPES_AND_FIELDS_VALID_NAME_REGEX))
                .collect(Collectors.toList());

            ((DotGraphQLContext) environment.getContext()).addFieldCount(queriedFieldName,
                    filteredContentletList.size());

            final DotContentletTransformer transformer = new DotTransformerBuilder()
                    .graphQLDataFetchOptions().content(filteredContentletList).build();
            return transformer.hydrate();
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
