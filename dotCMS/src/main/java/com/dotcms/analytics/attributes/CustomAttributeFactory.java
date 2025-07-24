package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.exception.DotDataException;

import java.util.Map;

public interface CustomAttributeFactory {

    void save(String eventTypeName, Map<String, String> attributes) throws DotDataException;

    Map<String, Map<String, String>> getAll() throws DotDataException;
}
