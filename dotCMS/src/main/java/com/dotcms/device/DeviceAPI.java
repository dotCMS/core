package com.dotcms.device;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

public interface DeviceAPI {
    Contentlet getCurrentDevice(final HttpServletRequest request, final User user) throws
            DotDataException;
}
