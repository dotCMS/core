package com.dotcms.analytics.track;

public class PageCollectorStrategy implements CollectorStrategy {

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return "page" == collectorContextMap.getRequestMatcher(); // should compare with the id
    }

    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        collectorPayloadBean.put("timestamp", System.currentTimeMillis());
        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
