package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.api.web.HttpServletRequestImpersonator;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Strategy to handle Contentlets of type Widget. It simply adds a new field to the map called
 * `widgetCodeJSON` which in case of having a $dotJSON.put will parse the field as JSON
 */
public class WidgetViewStrategy extends WebAssetStrategy<Contentlet> {

    /**
     * Main constructor
     * @param toolBox
     */
    WidgetViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Transform main method - entry point
     * @param widget
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    protected Map<String, Object> transform(final Contentlet widget, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotSecurityException, DotDataException {

        if (options.contains(TransformOptions.SKIP_WIDGET_CODE_RENDERING)) {
            Logger.debug(this, "Skipping widget code rendering");
            return map;
        }

        final HttpServletRequestImpersonator impersonator = HttpServletRequestImpersonator.newInstance();
        // do not allow the real request/response to be modified here.
        final HttpServletRequest request = impersonator.request();
        final HttpServletResponse response = impersonator.response();
        final String fieldValue = (String) map.get(WidgetContentType.WIDGET_CODE_FIELD_VAR);
        final Object parsedValue = RenderFieldStrategy.parseAsJSON(request, response, fieldValue, widget,
                WidgetContentType.WIDGET_CODE_FIELD_VAR);
        map.put(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR, parsedValue);
        return map;
    }
}
