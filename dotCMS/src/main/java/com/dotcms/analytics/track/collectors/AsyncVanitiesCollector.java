package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.visitor.filter.characteristics.BaseCharacter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import io.vavr.control.Try;

import java.util.HashMap;

/**
 * This asynchronized collector collects the page/asset information based on the vanity URL previous loaded on the
 * {@link CollectionCollectorPayloadBean}
 * @author jsanca
 */
public class AsyncVanitiesCollector implements Collector {

    private final FileAssetAPI fileAssetAPI;
    private final HTMLPageAssetAPI pageAPI;
    private final HostAPI hostAPI;


    public AsyncVanitiesCollector() {
        this(APILocator.getFileAssetAPI(),
                APILocator.getHTMLPageAssetAPI(),
                APILocator.getHostAPI());
    }

    public AsyncVanitiesCollector(final FileAssetAPI fileAssetAPI,
                                  final HTMLPageAssetAPI pageAPI,
                                  final HostAPI hostAPI) {

        this.fileAssetAPI = fileAssetAPI;
        this.hostAPI = hostAPI;
        this.pageAPI = pageAPI;
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
        final CollectorPayloadBean firstCollectorPayloadBean = collectionCollectorPayloadBean.first();
        final String vanityUrl = (String)firstCollectorPayloadBean.get("vanity_url");
        final String siteId = (String)collectorContextMap.get("siteId");
        final String uri = (String)collectorContextMap.get("uri");
        final String host = (String)collectorContextMap.get("host");
        final Long languageId = (Long)collectorContextMap.get("langId");
        final String language = (String)collectorContextMap.get("lang");
        final String requestId = (String)collectorContextMap.get("requestId");
        final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
        final CMSFilter.IAm whoIAM =BaseCharacter.resolveResourceType(vanityUrl, site, languageId);
        final HashMap<String, String> vanityReferrerObject = new HashMap<>();
        collectorPayloadBean.put("event_type", EventType.VANITY_REQUEST.getType());

        switch (whoIAM) {

            case PAGE:
                final IHTMLPage page = Try.of(()->this.pageAPI.getPageByPath(vanityUrl, site, languageId, true)).get();
                vanityReferrerObject.put("id", page.getIdentifier());
                vanityReferrerObject.put("title", page.getTitle());
                vanityReferrerObject.put("path", uri);
                collectorPayloadBean.put("event_type", EventType.VANITY_PAGE_REQUEST.getType());
                break;
            case FILE:
                final FileAsset fileAsset = Try.of(()->this.fileAssetAPI.getFileByPath(vanityUrl, site, languageId, true)).get();
            /*vanityReferrerObject.put("id", fileAsset.getIdentifier());
            vanityReferrerObject.put("title", fileAsset.getTitle());
            vanityReferrerObject.put("url", uri);*/
                collectorPayloadBean.put("event_type", EventType.VANITY_FILE_REQUEST.getType());
                break;
        }


        collectorPayloadBean.put("request_id", requestId);
        collectorPayloadBean.put("objects",  vanityReferrerObject);
        collectorPayloadBean.put("path", uri);
        collectorPayloadBean.put("site", host);

        collectorPayloadBean.put("language", language);
        collectorPayloadBean.put("siteId", siteId);

        return collectionCollectorPayloadBean.add(collectorPayloadBean);
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
