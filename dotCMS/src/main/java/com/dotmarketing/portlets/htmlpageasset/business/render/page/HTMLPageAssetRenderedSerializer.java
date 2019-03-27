package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import java.util.Map;

/**
 * Json serializer of {@link HTMLPageAssetRendered}
 */
public class HTMLPageAssetRenderedSerializer extends PageViewSerializer {

    @Override
    protected Map<String, Object> getObjectMap(PageView pageView) {
        final Map<String, Object> objectMap = super.getObjectMap(pageView);
        final HTMLPageAssetRendered htmlPageAssetRendered = (HTMLPageAssetRendered) pageView;

        final Map<String, Object> pageMap = (Map<String, Object>) objectMap.get("page");
        pageMap.put("rendered", htmlPageAssetRendered.getHtml());


        return objectMap;
    }

}
