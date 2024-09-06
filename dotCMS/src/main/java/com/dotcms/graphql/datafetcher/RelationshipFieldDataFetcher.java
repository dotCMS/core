package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class RelationshipFieldDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet nonCachedContentlet = environment.getSource();

            Logger.debug(this, ()-> "Fetching relationship field for contentlet: " + nonCachedContentlet.getIdentifier());

            // let's use the cache content so relationships are also cached
            final Contentlet contentlet = APILocator.getContentletAPI()
                .findContentletByIdentifier(nonCachedContentlet.getIdentifier(), nonCachedContentlet.isLive(),
                nonCachedContentlet.getLanguageId(), APILocator.systemUser(), true);

            final String fieldVar = environment.getField().getName();

            final Field
                field =
                APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(contentlet.getContentTypeId(), fieldVar);

            Relationship relationship;
            User user;

            try {
                user = ((DotGraphQLContext) environment.getContext()).getUser();
                relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field,
                    user);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotRuntimeException(e);
            }

            final boolean isChildField =  APILocator.getRelationshipAPI().isChildField(relationship, field);
            final ContentletRelationships contentletRelationships = new ContentletRelationships(null);
            final ContentletRelationships.ContentletRelationshipRecords
                records = contentletRelationships.new ContentletRelationshipRecords(
                relationship,isChildField);

            Object objectToReturn = records.doesAllowOnlyOne() ? null : Collections.emptyList();

            final String query = StringUtils.defaultString(environment.getArgument("query"), "");

            final int limit = environment.getArgument("limit")!=null
                    ? environment.getArgument("limit") : -1;

            final int offset = environment.getArgument("offset")!=null
                    ? environment.getArgument("offset") : 0;

            final String sort = StringUtils.defaultString(environment.getArgument("sort"), "");

            final boolean pullParents = APILocator.getRelationshipAPI().isParentField(relationship, field);

            List<Contentlet> relatedContent = ContentUtils
                    .pullRelatedField(relationship, contentlet.getIdentifier(),
                            query, limit, offset, sort, user, null, pullParents,
                            nonCachedContentlet.getLanguageId(), nonCachedContentlet.isLive());
            
            if (UtilMethods.isSet(relatedContent)) {
                final DotContentletTransformer transformer = new DotTransformerBuilder()
                        .graphQLDataFetchOptions().content(relatedContent).build();
                objectToReturn = records.doesAllowOnlyOne()
                    ? transformer.hydrate().get(0)
                    : transformer.hydrate();
            }

            return objectToReturn;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }

    }
}
