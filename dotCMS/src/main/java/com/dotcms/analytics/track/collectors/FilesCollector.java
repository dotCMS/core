package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This collector collects the file information
 * @author jsanca
 */
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
    public CollectionCollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectionCollectorPayloadBean collectionCollectorPayloadBean) {

        // we use the same event just collect more information async
        final CollectorPayloadBean collectorPayloadBean = collectionCollectorPayloadBean.first();
        final String uri = (String)collectorContextMap.get("uri");
        final String host = (String)collectorContextMap.get("host");
        final String siteId = (String)collectorContextMap.get("siteId");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final Map<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {

            final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
            final FileAsset fileAsset = Try.of(()->this.fileAssetAPI.getFileByPath(uri, site, languageId, true)).get();
            /*pageObject.put("id", fileAsset.getIdentifier());
            pageObject.put("title", fileAsset.getTitle());
            pageObject.put("url", uri);*/
        }

        final StringWriter writer = new StringWriter();
        Try.run(()-> DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValue(writer, pageObject));
        collectorPayloadBean.put("object",  writer.toString());
        collectorPayloadBean.put("url", uri);
        collectorPayloadBean.put("host", host);
        collectorPayloadBean.put("event_type", EventType.FILE_REQUEST.getType());

        if (UtilMethods.isSet(language)) {
            collectorPayloadBean.put("language", language);
        }

        if (UtilMethods.isSet(siteId)) {
            collectorPayloadBean.put("site", siteId);
        }

        return collectionCollectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
