package com.dotcms.rest.api.v1.browsertree;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for browser tree asset responses.
 * Contains asset results and i18n messages.
 * @deprecated This class is part of the deprecated BrowserTreeResource - use BrowserResource instead
 * 
 * @author Steve Bolton
 */
@Deprecated
public class ResponseEntityBrowserTreeView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityBrowserTreeView(final Map<String, Object> entity, final Map<String, String> i18nMessages) {
        super(entity, i18nMessages);
    }
}