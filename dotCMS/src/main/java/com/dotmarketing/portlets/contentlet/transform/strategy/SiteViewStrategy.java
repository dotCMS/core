package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.Map;
import java.util.Set;

/**
 * Strategy for Site View
 * Convert an object to Host in order to hydrated
 * @author jsanca
 */
public class SiteViewStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Main Constructor
     * @param toolBox
     */
    SiteViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * transform method entry point
     * @param contentlet
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     */
    @Override
    protected Map<String, Object> transform(final Contentlet contentlet,
                                            final Map<String, Object> map, final Set<TransformOptions> options, final User user) throws DotDataException {

        if (Host.HOST_VELOCITY_VAR_NAME.equals(contentlet.getContentType().variable())) {

            final Host host = new Host(contentlet);
            map.putAll(host.getMap());
        }
        return map;
    }
}
