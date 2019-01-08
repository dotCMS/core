package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class RelationshipFieldDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String fieldVar = environment.getField().getName();
        Field field = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(contentlet.getContentTypeId(), fieldVar);
        Relationship relationship = APILocator.getRelationshipAPI()
                .getRelationshipFromField(field, APILocator.getUserAPI().getSystemUser());

        return APILocator.getRelationshipAPI().dbRelatedContent(relationship, contentlet);
    }
}
