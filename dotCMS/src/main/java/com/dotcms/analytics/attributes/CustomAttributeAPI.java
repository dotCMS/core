package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.exception.DotDataException;

import java.util.Map;

public interface CustomAttributeAPI {

    void checkCustomPayloadValidation(String eventTypeName, Map<String, Object> customPayload)
            throws MaxCustomAttributesReachedException, DotDataException;


    Map<String, Object> translateToDatabase(String eventTypeName, Map<String, Object> customPayload);
}
