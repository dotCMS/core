package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;
import java.util.Map;

public interface DotTransformer {

    List<Map<String, Object>> toMaps();

    List<Contentlet> hydrate();

}
