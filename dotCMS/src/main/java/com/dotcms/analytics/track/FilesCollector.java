package com.dotcms.analytics.track;

public class FilesCollector implements Collector {

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        return "file" == collectorContextMap.getRequestMatcher(); // should compare with the id
    }


    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        collectorPayloadBean.put("timestamp", System.currentTimeMillis());
        collectorPayloadBean.put("objects", "tbd");
        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
