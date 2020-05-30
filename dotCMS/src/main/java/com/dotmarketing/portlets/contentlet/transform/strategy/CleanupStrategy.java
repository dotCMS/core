package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolbox.privateInternalProperties;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;

public class CleanupStrategy extends AbstractTransformStrategy<Contentlet> {

    CleanupStrategy(final TransformToolbox toolBox) {
        super(toolBox);
    }

    @Override
    public Contentlet fromContentlet(final Contentlet contentlet) {
        return contentlet;
    }

    @Override
    public Map<String, Object> transform(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, User user) {
        removePrivateProperties(map);
        return map;
    }

    /**
     * Removes all private internal properties from the contentlet
     * @param map input map
     * @return the same contentlet without the private properties
     */
    private void removePrivateProperties(final Map<String, Object> map) {
        map.keySet().removeAll(privateInternalProperties);
    }
}
