package com.dotcms.api.system.event;

import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

/**
 * This Util class provided methods to record different events link with the several types of
 * {@link Contentlet}
 *
 * @see SystemEventsAPI
 * @see SystemEvent
 */
public class ContentletSystemEventUtil {

    private static final String DELETE_EVENT_PREFIX = "DELETE";
    private static final String SAVE_EVENT_PREFIX = "SAVE";
    private static final String UPDATE_EVENT_PREFIX = "UPDATE";
    private static final String ARCHIVED_EVENT_PREFIX = "ARCHIVE";
    private static final String PUBLISH_EVENT_PREFIX = "PUBLISH";
    private static final String UN_PUBLISH_EVENT_PREFIX = "UN_PUBLISH";
    private static final String UN_ARCHIVED_EVENT_PREFIX = "UN_ARCHIVE";
    private static final String COPY_EVENT_PREFIX = "COPY";
    private static final String MOVE_EVENT_PREFIX = "MOVE";
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

    /**
     * Push a save or update event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     * The isNew argument set the prefix of the event name, if it is true then the prefix would be SAVE, in otherwise
     * the prefix would be UPDATE.
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a HOST then
     * it would be SITE, for example: if isNew is equals to true and the contentlet is a Host then the event pushed would be
     * SAVE_SITE.
     * If not exist any event with the name built then no event is pushed.
     *
     * @param contentlet is the Payload data
     * @param isNew
     */
    public void pushSaveEvent(Contentlet contentlet, boolean isNew){
        String actionName = getActionName(contentlet, isNew);
        sendEvent(contentlet, actionName);
    }

    /**
     * Push a delete event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     *
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a HOST then
     * it would be SITE, and the event's name would be DELETE_SITE.
     *
     * If not exist any event with the name built then no event is pushed.
     *
     * @param contentlet is the Payload data
     */
    public void pushDeleteEvent(Contentlet contentlet){
        sendEvent(contentlet, DELETE_EVENT_PREFIX);
    }

    /**
     * Push a publish event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     *
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a File then
     * it would be FILE_ASSET, and the event's name would be PUBLISH_FILE_ASSET.
     *
     * If not exist any event with the name built then no event is pushed.
     *
     * @param contentlet is the Payload data
     */
    public void pushPublishEvent(Contentlet contentlet){
        sendEvent(contentlet, PUBLISH_EVENT_PREFIX);
    }

    /**
     * Push a unpublish event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     *
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a File then
     * it would be FILE_ASSET, and the event's name would be UN_PUBLISH_FILE_ASSET.
     *
     * If not exist any event with the name built then no event is pushed.
     *
     * @param contentlet is the Payload data
     */
    public void pushUnpublishEvent(Contentlet contentlet){
        sendEvent(contentlet, UN_PUBLISH_EVENT_PREFIX);
    }

    /**
     * Push a copy event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     *
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a File then
     * it would be FILE_ASSET, and the event's name would be COPY_FILE_ASSET.
     *
     * If not exist any event with the name built then no event is pushed.
     *
     * @param contentlet is the Payload data
     */
    public void pushCopyEvent(Contentlet contentlet){
        sendEvent(contentlet, COPY_EVENT_PREFIX);
    }

    /**
     * Push a move event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     *
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a File then
     * it would be FILE_ASSET, and the event's name would be MOVE_FILE_ASSET.
     *
     * If not exist any event with the name built then no event is pushed.
     * @param contentlet is the Payload data
     */
    public void pushMoveEvent(Contentlet contentlet){
        sendEvent(contentlet, MOVE_EVENT_PREFIX);
    }

    /**
     * Push a archive event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     *
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a File then
     * it would be FILE_ASSET, and the event's name would be ARCHIVE_FILE_ASSET.
     *
     * If not exist any event with the name built then no event is pushed.
     *
     * @param contentlet is the Payload data
     */
    public void pushArchiveEvent(Contentlet contentlet){
        sendEvent(contentlet, ARCHIVED_EVENT_PREFIX);
    }

    /**
     * Push a unarchived event, the event that is pushed depends of the {@link Contentlet}'s Content Type.
     *
     * The suffix of the event's name is set by the {@link Contentlet}'s Content Type, so if the contentlet is a File then
     * it would be FILE_ASSET, and the event's name would be UN_ARCHIVED_FILE_ASSET.
     *
     * If not exist any event with the name built then no event is pushed.
     *
     * @param contentlet is the Payload data
     */
    public void pushUnArchiveEvent(Contentlet contentlet){
        sendEvent(contentlet, UN_ARCHIVED_EVENT_PREFIX);
    }

    /**
     * Return the event's name prefix for a SAVE or UPDATE action.
     *
     * @param contentlet
     * @param isNew
     * @return
     */
    private String getActionName(Contentlet contentlet, boolean isNew) {
        return isNew ? SAVE_EVENT_PREFIX : UPDATE_EVENT_PREFIX;
    }

    /**
     * Return the event's name according to a {@link Contentlet} and a methodName
     *
     * @param contentlet
     * @param methodName
     * @return
     */
    private SystemEventType getSystemEventType(Contentlet contentlet, String methodName) {

        try {

            final String contentType = getType(contentlet);
            final String eventName = String.format("%s_%s", methodName, contentType);

            return SystemEventType.valueOf(eventName.toUpperCase());
        }catch(IllegalArgumentException | IllegalStateException e) {
            Logger.debug(this, e.getMessage(), e);
            return null;
        }
    }

    private String getType(Contentlet contentlet) {
        if (contentlet.isHost()){
            return SITE_EVENT_SUFFIX;
        }else {
            try {

                final ContentType contentType = contentlet.getContentType();
                if (contentType != null && contentType.name() != null) { // todo: double check if this is the same of contentlet.getStructure().getName()
                    return contentType.name().replace(" ", "_").toUpperCase();
                } else {
                    throw new IllegalStateException("The Content type is null");
                }
            } catch (DotStateException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    private void sendEvent(Contentlet contentlet, String action) {
        SystemEventType systemEventType = getSystemEventType(contentlet, action);

        if (systemEventType != null) {

            Payload payload = this.getPayload(contentlet);

            try {
                systemEventsAPI.push(new SystemEvent(systemEventType, payload));
            } catch (DotDataException e) {
                throw new CanNotPushSystemEventException(e);
            }
        }
    }

    private Payload getPayload(Contentlet contentlet){
        if (contentlet.isHost()){
            return new Payload(contentlet, Visibility.PERMISSION, PermissionAPI.PERMISSION_READ);
        }else{
            return new Payload(contentlet, Visibility.EXCLUDE_OWNER,
                    new ExcludeOwnerVerifierBean(contentlet.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION));
        }
    }
}

