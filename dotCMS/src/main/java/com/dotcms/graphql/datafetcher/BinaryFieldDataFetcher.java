package com.dotcms.graphql.datafetcher;

import static com.dotcms.contenttype.model.type.BaseContentType.DOTASSET;

import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;
import com.dotmarketing.util.Logger;

import java.util.Collections;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class BinaryFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = contentlet.getContentType().baseType() == DOTASSET ?"asset"
                    :environment.getField().getName();

            Logger.debug(this, ()-> "Fetching binary field for contentlet: " + contentlet.getIdentifier());
            final BinaryToMapTransformer transformer = new BinaryToMapTransformer(contentlet);
            return (Map<String, Object>) transformer.asMap().get(var + "Map");
        } catch (IllegalArgumentException e) {
            Logger.warn(this, "Binary is null");
            return Collections.emptyMap();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
