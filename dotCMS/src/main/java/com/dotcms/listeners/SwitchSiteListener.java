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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;


import io.vavr.Lazy;
import java.io.Serializable;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.util.Objects;

/**
 * Trigger a {@link SystemEventType#SWITCH_SITE} when the current site is set in the {@link HttpSession}
 */
public class SwitchSiteListener implements HttpSessionAttributeListener {
    
    private final String CMS_SELECTED_HOST_ID_PREVIOUS="CMS_SELECTED_HOST_ID_PREVIOUS";
    private final Lazy<Boolean> THROW_WHEN_SESSION_NOT_SERIALIZABLE = Lazy.of(()-> Config.getBooleanProperty("THROW_WHEN_SESSION_NOT_SERIALIZABLE", false));

    private void checkSerializable(HttpSessionBindingEvent httpSessionBindingEvent){
        String name =httpSessionBindingEvent.getName();
        Object sessionObject = httpSessionBindingEvent.getValue();
        if(sessionObject ==null || sessionObject instanceof Serializable) {
            return;
        }
        DotRuntimeException dotRuntimeException = new DotRuntimeException("Session object is not serializable:" + name + ", object:" + sessionObject.getClass());
        if(THROW_WHEN_SESSION_NOT_SERIALIZABLE.get()){

            throw dotRuntimeException;
        }

        Logger.warnEveryAndDebug(this.getClass(),dotRuntimeException,5000);


    }

    
    
    @Override
    public void attributeAdded(final HttpSessionBindingEvent httpSessionBindingEvent) {
        checkSerializable(httpSessionBindingEvent);
        if (WebKeys.CMS_SELECTED_HOST_ID.equals(httpSessionBindingEvent.getName())) {
            final String value = (String) httpSessionBindingEvent.getValue();
            Logger.debug(this.getClass(),"selected site Added:" + value);
            this.triggerSwitchSiteEventIfNecessary(value,  httpSessionBindingEvent.getSession());
        }
    }

    @Override
    public void attributeRemoved(final HttpSessionBindingEvent httpSessionBindingEvent) {
        if (WebKeys.CMS_SELECTED_HOST_ID.equals(httpSessionBindingEvent.getName())) {
            final String value = (String) httpSessionBindingEvent.getValue();
            Logger.debug(this.getClass(),"selected site removed:" +  value);
        }
    }

    @Override
    public void attributeReplaced(final HttpSessionBindingEvent httpSessionBindingEvent) {
        checkSerializable(httpSessionBindingEvent);
        if (WebKeys.CMS_SELECTED_HOST_ID.equals(httpSessionBindingEvent.getName())) {
            final String value = (String) httpSessionBindingEvent.getValue();
            Logger.debug(this.getClass(),"selected site replace -> " + value);
            this.triggerSwitchSiteEventIfNecessary(value,  httpSessionBindingEvent.getSession());
        }
    }

    private void triggerSwitchSiteEventIfNecessary(
            final String newHosId,
            final HttpSession session) {
        if(session==null) return;
        synchronized (session) {
            final String oldHostId = (String) session.getAttribute(CMS_SELECTED_HOST_ID_PREVIOUS);

            if (!Objects.equals(newHosId, oldHostId)) {
                try {
                    final User loggedInUser = APILocator.getLoginServiceAPI().getLoggedInUser();
    
                    if (loggedInUser != null && loggedInUser.hasConsoleAccess()) {
                        final Host host = APILocator.getHostAPI().find(newHosId, loggedInUser, false);
    
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
                    Logger.warn(this, e.getMessage(), e);
                    throw new DotRuntimeException(e);
                }
            }
            session.setAttribute(CMS_SELECTED_HOST_ID_PREVIOUS, newHosId);
        }

    }
}
