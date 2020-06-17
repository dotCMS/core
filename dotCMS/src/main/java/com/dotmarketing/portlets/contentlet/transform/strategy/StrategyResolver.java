package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.contenttype.model.type.ContentType;
import java.util.List;
import java.util.Set;

/**
 * This intends to serve strategies for a given combination of CT and options
 */
public interface StrategyResolver {

    /**
     * Serve a list of strategies to be used to modify the contentlet
     * @param contentType
     * @param options
     * @return
     */
    List<AbstractTransformStrategy> resolveStrategies(ContentType contentType,
            Set<TransformOptions> options);
}
