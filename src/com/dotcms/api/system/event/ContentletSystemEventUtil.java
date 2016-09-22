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

    private final SystemEventsAPI systemEventsAPI;

    @VisibleForTesting
    public ContentletSystemEventUtil(SystemEventsAPI systemEventsAPI){
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
        Payload payload = new Payload(contentlet, Visibility.PERMISSION, String.valueOf(PermissionAPI.PERMISSION_READ),
                user.getUserId());

        try {
            systemEventsAPI.push(new SystemEvent(systemEventType, payload));
        } catch (DotDataException e) {
            throw new BaseRuntimeInternationalizationException(e);
        }
    }

    private SystemEventType getSystemEventType(Contentlet contentlet, boolean isNew) {

        String methodName = isNew ? "SAVE" : "UPDATE";
        String contentType = getType(contentlet);
        String eventName = String.format("%s_%s", methodName, contentType);

        try {
            return SystemEventType.valueOf(eventName);
        }catch(IllegalArgumentException e){
            return null;
        }
    }

    private String getType(Contentlet contentlet) {
        return contentlet.isHost() ? "SITE" : contentlet.getStructure().getName();
    }

}
