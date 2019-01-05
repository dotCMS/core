package com.dotcms.graphql.datafetcher;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class ImageFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(DataFetchingEnvironment environment) throws Exception {

        // TODO: Remove duplication with https://github.com/dotCMS/core/blob/poc-transformers-more-than-meets-the-eye
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();

        final String imageIdentifier = (String) contentlet.get(var);

        final Contentlet imageContent = APILocator.getContentletAPI()
            .findContentletByIdentifier(imageIdentifier, contentlet.isLive(), contentlet.getLanguageId(),
                APILocator.systemUser(), false);

        final FileAsset imageFileAsset = APILocator.getFileAssetAPI().fromContentlet(imageContent);


        final Map<String, Object> map = new HashMap<>();

        map.put("name", imageFileAsset.getFileName());
        map.put("link", "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getIdentifier()) + "/"
            + imageFileAsset.getFileName() + "?language_id=" + imageFileAsset.getLanguageId());

        // metadata? check enterprise?
        map.put("size", imageFileAsset.getFileSize());
        map.put("width", imageFileAsset.getWidth());
        map.put("height", imageFileAsset.getHeight());
        map.put("contentType", imageFileAsset.getMimeType());
        map.put("metadata", imageFileAsset.getMetaData());

        return map;
    }
}
