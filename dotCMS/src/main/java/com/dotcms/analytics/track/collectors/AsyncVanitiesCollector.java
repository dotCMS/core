package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import io.vavr.control.Try;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This asynchronized collector collects the page/asset information based on the vanity URL previous loaded on the
 * {@link CollectionCollectorPayloadBean}
 * @author jsanca
 */
public class AsyncVanitiesCollector implements Collector {

    private final FileAssetAPI fileAssetAPI;
    private final HostAPI hostAPI;


    public AsyncVanitiesCollector() {
        this(APILocator.getFileAssetAPI(),
                APILocator.getHostAPI());
    }

    public AsyncVanitiesCollector(final FileAssetAPI fileAssetAPI,
                                  final HostAPI hostAPI) {

        this.fileAssetAPI = fileAssetAPI;
        this.hostAPI = hostAPI;
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return VanitiesRequestMatcher.VANITIES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }


    @Override
    public CollectionCollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectionCollectorPayloadBean collectionCollectorPayloadBean) {

        // this will be a new event
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBean();
        final String vanityUrl = collectorPayloadBean.get("vanity_url") != null?
                (String)collectorPayloadBean.get("vanity_url"):null;
        final String vanityQueryString = collectorPayloadBean.get("vanity_query_string") != null?   // get the query string
                (String)collectorPayloadBean.get("vanity_query_string"):null;

        final String uri = (String)collectorContextMap.get("uri");
        final String siteId = (String)collectorContextMap.get("host");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final CachedVanityUrl cachedVanityUrl = (CachedVanityUrl)collectorContextMap.get(Constants.VANITY_URL_OBJECT);
        final Map<String, String> pageObject = new HashMap<>();

        if (Objects.nonNull(uri) && Objects.nonNull(siteId) && Objects.nonNull(languageId)) {

            final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
            /*final IHTMLPage page = Try.of(()->this.pageAPI.getPageByPath(cachedVanityUrl, site, languageId, true)).get();
            pageObject.put("object_id", page.getIdentifier());
            pageObject.put("title", page.getTitle());*/
            pageObject.put("path", uri);
        }

        final StringWriter writer = new StringWriter();
        Try.run(()-> DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValue(writer, pageObject));
        collectorPayloadBean.put("objects",  writer.toString());
        collectorPayloadBean.put("path", uri);
        collectorPayloadBean.put("event_type", EventType.VANITY_REQUEST.getType());
        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("site", siteId);

        return collectionCollectorPayloadBean.add(collectorPayloadBean);
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
