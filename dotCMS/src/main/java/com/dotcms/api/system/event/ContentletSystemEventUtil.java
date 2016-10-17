package com.dotcms.api.system.event;

import com.dotcms.exception.BaseInternationalizationException;
import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.validation.*;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * This Util class provided methods to record different events link with the several types of
 * {@link Contentlet}
 *
 * @see SystemEventsAPI
 * @see SystemEvent
 */
public class ContentletSystemEventUtil {

    private static final String SAVE_EVENT_PREFIX = "SAVE";
    private static final String UPDATE_EVENT_PREFIX = "UPDATE";

    private static final String SITE_EVENT_SUFFIX= "SITE";

    private final SystemEventsAPI systemEventsAPI;

    @VisibleForTesting
    protected ContentletSystemEventUtil(SystemEventsAPI systemEventsAPI){
        this.systemEventsAPI = systemEventsAPI;
    }

    private ContentletSystemEventUtil(){
        this(APILocator.getSystemEventsAPI());
    }

    private static class SingletonHolder {
        private static final ContentletSystemEventUtil INSTANCE = new ContentletSystemEventUtil();
    }

    public static ContentletSystemEventUtil getInstance() {
        return ContentletSystemEventUtil.SingletonHolder.INSTANCE;
    }

    public void pushSaveEvent(User user, Contentlet contentlet, boolean isNew){
        SystemEventType systemEventType = getSystemEventType(contentlet, isNew);

        if (systemEventType != null) {
            Payload payload = new Payload(contentlet, Visibility.PERMISSION, String.valueOf(PermissionAPI.PERMISSION_READ));

            try {
                systemEventsAPI.push(new SystemEvent(systemEventType, payload));
            } catch (DotDataException e) {
                throw new CanNotPushSystemEventException(e);
            }
        }
    }

    private SystemEventType getSystemEventType(Contentlet contentlet, boolean isNew) {

        String methodName = isNew ? SAVE_EVENT_PREFIX : UPDATE_EVENT_PREFIX;
        String contentType = getType(contentlet);
        String eventName = String.format("%s_%s", methodName, contentType);

        try {
            return SystemEventType.valueOf(eventName.toUpperCase());
        }catch(IllegalArgumentException e){
            return null;
        }
    }

    private String getType(Contentlet contentlet) {
        return contentlet != null && contentlet.isHost() ? SITE_EVENT_SUFFIX : contentlet.getStructure().getName();
    }

}
