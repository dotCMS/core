package com.dotcms.device;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

/**
 * API for Device-related operations 
 */
public interface DeviceAPI {

    /**
     * Returns a {@link com.dotmarketing.portlets.contentlet.model.Contentlet} representing the Device
     * given a request and a user
     */
    Optional<Contentlet> getCurrentDevice(final HttpServletRequest request, final User user) throws
            DotDataException;
}
