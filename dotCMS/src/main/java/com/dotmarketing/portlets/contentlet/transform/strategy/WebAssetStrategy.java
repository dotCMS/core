package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;

import com.dotcms.api.APIProvider;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
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

    /**
     * ToolBox taker constructor
     * @param toolBox
     */
    WebAssetStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Strategy Apply method
     * @param source
     * @param targetMap
     * @param options
     * @param user
     */
    public final void apply(final Contentlet source, final Map<String, Object> targetMap,
            final Set<TransformOptions> options, final User user) {
        super.apply(source, targetMap, options, user);
        urlOverride(source, targetMap);
        addPath(source, targetMap);
    }

    /**
     * This is intended to force the calculation of the URL on all the descendants of WeAsset (Pages,Files)
     * @param contentlet
     * @param map
     */
    private void urlOverride(final Contentlet contentlet, final Map<String, Object> map){
        if (!Boolean.TRUE.equals(contentlet.isHTMLPage()) && contentlet.getMap().get(URL_FIELD) != null) {
            return;
        }
        final String url = toolBox.contentHelper.getUrl(contentlet);
        if (null != url) {
            map.put(URL_FIELD, url);
        }
        map.put(URL_FIELD, url);

    }

    /**
     * Identifier path
     * @param contentlet
     * @param map
     */
    private void addPath(final Contentlet contentlet, final Map<String, Object> map){
        final String identifierValue = contentlet.getIdentifier();
        try {
            final Identifier identifier = toolBox.identifierAPI.find(identifierValue);
            map.put("path", identifier.getPath());
        } catch (DotDataException e) {
            Logger.warn(WebAssetStrategy.class,String.format("Error adding path from identifier: `%s`", identifierValue), e);
        }
    }

}
