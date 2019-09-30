package com.dotcms.listeners;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.UserSessionBean;
import com.dotcms.api.system.event.Visibility;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;


import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.util.Objects;

/**
 * Trigger a {@link SystemEventType#SWITCH_SITE} when the current site is set in the {@link HttpSession}
 */
public class SwitchSiteListener implements HttpSessionAttributeListener {
    @Override
    public void attributeAdded(final HttpSessionBindingEvent httpSessionBindingEvent) {
        if (WebKeys.CMS_SELECTED_HOST_ID.equals(httpSessionBindingEvent.getName())) {
            final String currentValue = (String) httpSessionBindingEvent.getValue();
            this.triggerSwitchSiteEventIfNecessary(currentValue, null, httpSessionBindingEvent.getSession());
        }
    }

    @Override
    public void attributeRemoved(final HttpSessionBindingEvent httpSessionBindingEvent) {

    }

    @Override
    public void attributeReplaced(final HttpSessionBindingEvent httpSessionBindingEvent) {
        if (WebKeys.CMS_SELECTED_HOST_ID.equals(httpSessionBindingEvent.getName())) {
            final String currentValue = (String) httpSessionBindingEvent.getSession().getAttribute(WebKeys.CMS_SELECTED_HOST_ID);
            final String oldValue = (String) httpSessionBindingEvent.getValue();
            this.triggerSwitchSiteEventIfNecessary(currentValue, oldValue, httpSessionBindingEvent.getSession());
        }
    }

    private void triggerSwitchSiteEventIfNecessary(
            final String currentHostNewValue,
            final String currentHostOldValue,
            final HttpSession session) {

        if (!Objects.equals(currentHostNewValue, currentHostOldValue)) {
            try {
                final User loggedInUser = APILocator.getLoginServiceAPI().getLoggedInUser();

                if (loggedInUser != null && loggedInUser.hasConsoleAccess()) {
                    final Host host = APILocator.getHostAPI().find(currentHostNewValue, loggedInUser, false);

                    APILocator.getSystemEventsAPI().push(SystemEventType.SWITCH_SITE,
                            new Payload(
                                    host,
                                    Visibility.USER_SESSION,
                                    new UserSessionBean(
                                            loggedInUser.getUserId(),
                                            session.getId()
                                    )
                            )
                    );
                }
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(this, e.getMessage(), e);
                throw new DotRuntimeException(e);
            }
        }

    }
}
