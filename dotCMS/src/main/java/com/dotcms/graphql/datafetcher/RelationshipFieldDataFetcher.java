package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Collections;
import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class RelationshipFieldDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet nonCachedContentlet = environment.getSource();

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

            List<Contentlet> relatedContent = contentlet.getRelated(fieldVar, user,
                    true, isChildField, contentlet.getLanguageId(), contentlet.isLive());
            
            if (UtilMethods.isSet(relatedContent)) {
                objectToReturn = records.doesAllowOnlyOne()
                    ? new ContentletToMapTransformer(relatedContent).hydrate().get(0)
                    : new ContentletToMapTransformer(relatedContent).hydrate();
            }

            return objectToReturn;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }

    }
}
