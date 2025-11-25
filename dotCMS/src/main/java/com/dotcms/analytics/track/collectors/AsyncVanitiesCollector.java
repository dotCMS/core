package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.visitor.filter.characteristics.BaseCharacter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Map;

/**
 * This asynchronized collector collects the page/asset information based on the vanity URL previous loaded on the
 * @author jsanca
 */
public class AsyncVanitiesCollector implements Collector {

    private final HostAPI hostAPI;
    private final Map<CMSFilter.IAm, Collector> match = new HashMap<>();

    public AsyncVanitiesCollector() {
        this(APILocator.getHostAPI());
    }

    public AsyncVanitiesCollector(final HostAPI hostAPI) {

        this.hostAPI = hostAPI;

        match.put(CMSFilter.IAm.PAGE, new PagesCollector());
        match.put(CMSFilter.IAm.FILE, new FilesCollector());
    }

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        final CachedVanityUrl cachedVanityUrl = (CachedVanityUrl)collectorContextMap.get(Constants.VANITY_URL_OBJECT);

        return VanitiesRequestMatcher.VANITIES_MATCHER_ID.equals(collectorContextMap.getRequestMatcher().getId()) &&
                UtilMethods.isSet(cachedVanityUrl) && cachedVanityUrl.isForward();
    }


    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        // this will be a new event
        final CachedVanityUrl cachedVanityUrl = (CachedVanityUrl) collectorContextMap.get(Constants.VANITY_URL_OBJECT);

        final Host currentHost = (Host)collectorContextMap.get("currentHost");
        final Long languageId = (Long)collectorContextMap.get("langId");

        final Host site = Try.of(()->this.hostAPI.find(currentHost.getIdentifier(), APILocator.systemUser(),
                false)).get();
        final CMSFilter.IAm whoIAM = BaseCharacter.resolveResourceType(cachedVanityUrl.forwardTo, site, languageId);

        if (UtilMethods.isSet(whoIAM)) {

            final CollectorContextMap innerCollectorContextMap = new WrapperCollectorContextMap(collectorContextMap,
                    Map.of("uri", cachedVanityUrl.forwardTo));
            match.get(whoIAM).collect(innerCollectorContextMap, collectorPayloadBean);
        }

        collectorPayloadBean.put("comeFromVanityURL", "true");
        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
