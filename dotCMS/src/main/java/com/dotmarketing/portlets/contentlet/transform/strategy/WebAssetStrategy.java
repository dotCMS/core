package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;

/**
 * This a super class intended to model anything that can reside on the we and therefore have a url
 * It it intended to calculate the url associated to the identifier and avoid caching issues.
 * @param <T>
 */
public abstract class WebAssetStrategy <T extends Contentlet>  extends
        AbstractTransformStrategy<T> {

    WebAssetStrategy(final TransformToolbox toolBox) {
        super(toolBox);
    }

    public final void apply(final T contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, User user) {
        super.apply(contentlet, map, options, user);
        urlOverride(contentlet, map);
    }

    /**
     * This is intended to force the calculation of the URL on all the descendants of WeAsset (Pages,Files)
     * @param contentlet
     * @param map
     */
    private void urlOverride(final Contentlet contentlet, final Map<String, Object> map){
        final String url = toolBox.contentHelper.getUrl(contentlet);
        if (null != url) {
            map.put(URL_FIELD, url);
        }
        map.put(URL_FIELD, url);
    }

}
