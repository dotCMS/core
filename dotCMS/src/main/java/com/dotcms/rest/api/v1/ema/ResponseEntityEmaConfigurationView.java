package com.dotcms.rest.api.v1.ema;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.util.json.JSONObject;

/**
 * Entity View for EMA configuration responses.
 * Contains Enterprise Mobile Application configuration data from app secrets.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityEmaConfigurationView extends ResponseEntityView<JSONObject> {
    public ResponseEntityEmaConfigurationView(final JSONObject entity) {
        super(entity);
    }
}