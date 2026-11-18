package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.api.web.HttpServletRequestImpersonator;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Collections;
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
     * Adds the {@code widgetCodeJSON} property, which holds whatever the widget put into
     * {@code $dotJSON} while it rendered. Producing it renders the widget the way a page would,
     * field values included, so when the caller asked for the fields to be rendered it follows the
     * same rule the fields do: a widget whose field values must stay as text reports an empty map,
     * the same answer a widget that wrote no {@code $dotJSON} gives.
     *
     * @param widget the widget {@link Contentlet} being transformed
     * @param map the content map built so far, modified in place
     * @param options the options the caller asked the transformer for
     * @param user the {@link User} the transformation runs for
     * @return the content map, carrying {@code widgetCodeJSON} unless the widget rule says otherwise
     * @throws DotSecurityException if the transformation is not allowed for this user
     * @throws DotDataException if the underlying data cannot be read
     */
    @Override
    protected Map<String, Object> transform(final Contentlet widget, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotSecurityException, DotDataException {

        if (options.contains(TransformOptions.SKIP_WIDGET_CODE_RENDERING)) {
            Logger.debug(this, "Skipping widget code rendering");
            return map;
        }

        if (options.contains(TransformOptions.RENDER_FIELDS)
                && !RenderFieldStrategy.rendersFieldValues(widget)) {
            Logger.debug(this, () -> String.format(
                    "Widget %s: empty widgetCodeJSON, widget field scripting is off for this widget",
                    widget.getIdentifier()));
            // The key stays, the way it does for a widget that produced no $dotJSON
            map.put(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR, Collections.emptyMap());
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
