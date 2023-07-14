package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Set;

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

        final String fieldValue = (String) map.get(WidgetContentType.WIDGET_CODE_FIELD_VAR);


        if(fieldValue.contains("$dotJSON.put(")){
            final Object parsedValue = RenderFieldStrategy.parseAsJSON(null, null,
                    fieldValue, widget, WidgetContentType.WIDGET_CODE_FIELD_VAR);
            map.put(WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR, parsedValue);
        }


        return map;
    }
}
