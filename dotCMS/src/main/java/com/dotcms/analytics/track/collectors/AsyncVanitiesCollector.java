package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.visitor.filter.characteristics.BaseCharacter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Map;

/**
 * This asynchronized collector collects the page/asset information based on the vanity URL previous loaded on the
 * {@link CollectionCollectorPayloadBean}
 * @author jsanca
 */
public class AsyncVanitiesCollector implements Collector {

    private final FileAssetAPI fileAssetAPI;
    private final HTMLPageAssetAPI pageAPI;
    private final HostAPI hostAPI;
    private Map<CMSFilter.IAm, Collector> match = new HashMap<>();

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

        match.put(CMSFilter.IAm.PAGE, new PagesCollector());
        match.put(CMSFilter.IAm.FILE, new FilesCollector());
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return VanitiesRequestMatcher.VANITIES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) ; // should compare with the id
    }


    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        // this will be a new event

        final String vanityUrl = (String) collectorContextMap.get(Constants.CMS_FILTER_URI_OVERRIDE);

        final String siteId = (String)collectorContextMap.get("siteId");
        final Long languageId = (Long)collectorContextMap.get("langId");

        final Host site = Try.of(()->this.hostAPI.find(siteId, APILocator.systemUser(), false)).get();
        final CMSFilter.IAm whoIAM = BaseCharacter.resolveResourceType(vanityUrl, site, languageId);
        match.get(whoIAM).collect(collectorContextMap, collectorPayloadBean);

        collectorPayloadBean.put("comoFromVanityURL", "true");
        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
