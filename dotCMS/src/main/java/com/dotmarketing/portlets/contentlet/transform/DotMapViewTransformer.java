package com.dotmarketing.portlets.contentlet.transform;

import java.util.List;
import java.util.Map;

/**
 * This is an interface that basically describes a behavior on which stuff can be viewed as maps
 */
public interface DotMapViewTransformer {

    List<Map<String, Object>> toMaps();

}
