package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import io.vavr.control.Try;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FilesCollector implements Collector {

    private final FileAssetAPI fileAssetAPI;
    private final HostAPI hostAPI;


    public FilesCollector() {
        this(APILocator.getFileAssetAPI(),
                APILocator.getHostAPI());
    }

    public FilesCollector(final FileAssetAPI fileAssetAPI,
                          final HostAPI hostAPI) {

        this.fileAssetAPI = fileAssetAPI;
        this.hostAPI = hostAPI;
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return FilesRequestMatcher.FILES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }


    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        final String uri = (String)collectorContextMap.get("uri");
        final String siteId = (String)collectorContextMap.get("host");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final Map<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {

            final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
            final FileAsset fileAsset = Try.of(()->this.fileAssetAPI.getFileByPath(uri, site, languageId, true)).get();
            pageObject.put("object_id", fileAsset.getIdentifier());
            pageObject.put("title", fileAsset.getTitle());
            pageObject.put("path", uri);
        }

        final StringWriter writer = new StringWriter();
        Try.run(()-> DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValue(writer, pageObject));
        collectorPayloadBean.put("objects",  writer.toString());
        collectorPayloadBean.put("path", uri);
        collectorPayloadBean.put("event_type", "FILE_REQUEST");
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("site", siteId);

        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
