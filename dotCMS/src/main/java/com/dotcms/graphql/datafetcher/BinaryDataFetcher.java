package com.dotcms.graphql.datafetcher;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class BinaryDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(DataFetchingEnvironment environment) throws Exception {

        // TODO: Remove duplication with https://github.com/dotCMS/core/blob/poc-transformers-more-than-meets-the-eye
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();

        final Map<String, Object> map = new HashMap<>();
        File f;
        try {
            f = contentlet.getBinary(var);
        } catch (IOException e) {
            throw new DotStateException(e);
        }
        map.put("versionPath", "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getInode()) + "/" + var + "/" + f.getName());
        map.put("idPath", "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getIdentifier()) + "/" + var + "/" + f.getName());
        map.put("name", f.getName());
        map.put("size", f.length());
        map.put("mime", Config.CONTEXT.getMimeType(f.getName()));
        map.put("isImage", UtilMethods.isImage(f.getName()));

        return map;
    }
}
