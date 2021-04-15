package com.dotcms.device;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

public class DeviceAPIImpl implements DeviceAPI {

    @Override
    public Optional<Contentlet> getCurrentDevice(final HttpServletRequest request, final User user)
            throws DotDataException {
        final String currentDeviceId = (String) request.getSession().
                getAttribute(WebKeys.CURRENT_DEVICE);
        Optional<Contentlet> currentDevice = Optional.empty();

        try {
            if (currentDeviceId != null) {
                currentDevice = Optional.ofNullable(APILocator.getContentletAPI().find(currentDeviceId, user,
                        false));

                if (!currentDevice.isPresent()) {
                    request.getSession().removeAttribute(WebKeys.CURRENT_DEVICE);
                }
            }
        } catch (DotSecurityException e) {
            Logger.debug(this.getClass(),
                    "Exception on createViewAsMap exception message: " + e.getMessage(), e);
        }

        return currentDevice;
    }
}
