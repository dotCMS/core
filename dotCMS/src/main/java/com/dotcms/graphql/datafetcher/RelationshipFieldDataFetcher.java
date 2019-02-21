package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
            final Contentlet contentlet = environment.getSource();
            final String fieldVar = environment.getField().getName();

            final Field
                field =
                APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(contentlet.getContentTypeId(), fieldVar);

            Relationship relationship;

            try {
                final User user = ((DotGraphQLContext) environment.getContext()).getUser();
                relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field,
                    user);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotRuntimeException(e);
            }

            final ContentletRelationships contentletRelationships = new ContentletRelationships(null);
            final ContentletRelationships.ContentletRelationshipRecords
                records = contentletRelationships.new ContentletRelationshipRecords(
                relationship,
                APILocator.getRelationshipAPI().isParent(relationship, contentlet.getContentType()));

            Object objectToReturn = records.doesAllowOnlyOne() ? null : Collections.emptyList();

            if (UtilMethods.isSet(contentlet.getRelated(fieldVar))) {
                objectToReturn = records.doesAllowOnlyOne()
                    ? contentlet.getRelated(fieldVar).get(0)
                    : contentlet.getRelated(fieldVar);
            }

            return objectToReturn;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }

    }
}
