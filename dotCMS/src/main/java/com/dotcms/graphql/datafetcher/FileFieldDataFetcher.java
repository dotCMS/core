package com.dotcms.graphql.datafetcher;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FileFieldDataFetcher implements DataFetcher<Contentlet> {
    @Override
    public Contentlet get(DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();
        final String fileAssetIdentifier = (String) contentlet.get(var);
        final Contentlet fileAsContent = APILocator.getContentletAPI()
            .findContentletByIdentifier(fileAssetIdentifier, contentlet.isLive(), contentlet.getLanguageId(),
                APILocator.systemUser(), false);
        return  APILocator.getFileAssetAPI().fromContentlet(fileAsContent);
    }
}
