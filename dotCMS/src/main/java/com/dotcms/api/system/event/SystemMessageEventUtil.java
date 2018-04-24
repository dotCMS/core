package com.dotcms.api.system.event;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ErrorEntity;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

import java.util.*;

/**
 * This class encapsulates the logic to create the System Message for the System Events.
 * @author jsanca
 */
public class SystemMessageEventUtil {

    private final SystemEventsAPI systemEventsAPI;

    @VisibleForTesting
    protected SystemMessageEventUtil(final SystemEventsAPI systemEventsAPI){
        this.systemEventsAPI = systemEventsAPI;
    }

    private SystemMessageEventUtil(){
        this(APILocator.getSystemEventsAPI());
    }

    private static class SingletonHolder {
        private static final SystemMessageEventUtil INSTANCE = new SystemMessageEventUtil();
    }

    public static SystemMessageEventUtil getInstance() {
        return SystemMessageEventUtil.SingletonHolder.INSTANCE;
    }

    /**
     * Sends a Simple text event (RAW MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param message String a simple message string
     */
    public void pushSimpleTextEvent (final String message) {

        this.pushSimpleTextEvent(message, Collections.emptyList());
    } // pushSimpleTextEvent.

    /**
     * Sends a Simple text event (RAW MESSAGE) based on the parameters
     * @param message String a simple message string
     * @param userId    String user to send the message
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushSimpleTextEvent (final String message, final String userId, final String... portletIds) {

        this.pushSimpleTextEvent(message, Arrays.asList(userId), portletIds);
    } // pushSimpleTextEvent.

    /**
     * Sends a Simple text event (RAW MESSAGE) based on the parameters
     * @param message String a simple message string
     * @param users   user or list of user you want to send the message (null or empty means, send the message to all users)
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushSimpleTextEvent (final String message, final List<String> users, final String... portletIds) {

        this.pushSimpleEvent (SystemEventType.RAW_MESSAGE, message, users, portletIds);
    } // pushSimpleTextEvent.


    /////////////////////////////////

    /**
     * Sends a Simple error event (RAW_ERROR_MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param error {@link ErrorEntity} an error
     */
    public void pushSimpleErrorEvent (final ErrorEntity error) {

        this.pushSimpleErrorEvent(error, Collections.emptyList());
    } // pushSimpleErrorEvent.

    /**
     * Sends a Simple error event (RAW_ERROR_MESSAGE) based on the parameters
     * @param error     {@link ErrorEntity} an error
     * @param userId    String user to send the message
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushSimpleErrorEvent (final ErrorEntity error, final String userId, final String... portletIds) {

        this.pushSimpleErrorEvent(error, Arrays.asList(userId), portletIds);
    } // pushSimpleErrorEvent.

    /**
     * Sends a Simple error event (RAW_ERROR_MESSAGE) based on the parameters
     * @param error   {@link ErrorEntity} error to send (code + raw message)
     * @param users   user or list of user you want to send the message (null or empty means, send the message to all users)
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushSimpleErrorEvent (final ErrorEntity error, final List<String> users, final String... portletIds) {

        this.pushSimpleEvent (SystemEventType.RAW_ERROR_MESSAGE, error, users, portletIds);
    } // pushSimpleErrorEvent.

    /////////////////////////////////

    /**
     * Sends a Simple rich media event (by now just html string) (RICH_MEDIA_MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param richMediaMessage {@link Object} a rich media message
     */
    public void pushRichMediaEvent (final Object richMediaMessage) {

        this.pushRichMediaEvent(richMediaMessage, Collections.emptyList());
    } // pushRichMediaEvent.

    /**
     * Sends a Simple rich media event (by now just html string) (RICH_MEDIA_MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param richMediaMessage {@link Object} a rich media message
     * @param userId    String user to send the message
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushRichMediaEvent (final Object richMediaMessage, final String userId, final String... portletIds) {

        this.pushRichMediaEvent(richMediaMessage, Arrays.asList(userId), portletIds);
    } // pushRichMediaEvent.

    /**
     * Sends a Simple rich media event (by now just html string) (RICH_MEDIA_MESSAGE) based on the parameters
     * @param richMediaMessage   {@link Object} a rich media message
     * @param users   user or list of user you want to send the message (null or empty means, send the message to all users)
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushRichMediaEvent (final Object richMediaMessage, final List<String> users, final String... portletIds) {

        this.pushSimpleEvent (SystemEventType.RICH_MEDIA_MESSAGE, richMediaMessage, users, portletIds);
    } // pushRichMediaEvent.

    /////////////////////////////////

    /**
     * Sends a Simple Text Confirmation message (CONFIRMATION_MESSAGE + RAW_MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param callbackOnYes String callback to call when users confirm the message (it is a must)
     * @param callbackOnNo String  callback to call when users does not confirm the message (it is an optional, could be null)
     * @param message String a simple message string
     */
    public void pushConfirmationSimpleTextEvent (final String callbackOnYes, final String callbackOnNo, final String message) {

        this.pushConfirmationSimpleTextEvent(callbackOnYes, callbackOnNo, message, Collections.emptyList());
    } // pushConfirmationSimpleTextEvent.

    /**
     * Sends a Simple Text Confirmation message (CONFIRMATION_MESSAGE + RAW_MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param callbackOnYes String callback to call when users confirm the message (it is a must)
     * @param callbackOnNo String  callback to call when users does not confirm the message (it is an optional, could be null)
     * @param message  {@link String} a simple raw message
     * @param userId    String user to send the message
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushConfirmationSimpleTextEvent (final String callbackOnYes, final String callbackOnNo, final String message, final String userId, final String... portletIds) {

        this.pushConfirmationSimpleTextEvent(callbackOnYes, callbackOnNo, message, Arrays.asList(userId), portletIds);
    } // pushConfirmationSimpleTextEvent.

    /**
     * Sends a Simple Text Confirmation message (CONFIRMATION_MESSAGE + RAW_MESSAGE) based on the parameters
     * @param callbackOnYes String callback to call when users confirm the message (it is a must)
     * @param callbackOnNo String  callback to call when users does not confirm the message (it is an optional, could be null)
     * @param message            {@link String} a rich media message
     * @param users   user or list of user you want to send the message (null or empty means, send the message to all users)
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushConfirmationSimpleTextEvent (final String callbackOnYes, final String callbackOnNo,
                                                 final String message, final List<String> users, final String... portletIds) {

        final Set<String> portletIdSet = (null != portletIds) ? new HashSet<>(Arrays.asList(portletIds)) : null;
        final Payload payload = this.createPayload(new SystemConfirmationMessage(callbackOnYes, callbackOnNo,
                        SystemEventType.RAW_MESSAGE, new SystemMessage(message, portletIdSet)), users);

        try {

            this.systemEventsAPI.push(new SystemEvent(SystemEventType.CONFIRMATION_MESSAGE,
                    payload));
        } catch (DotDataException e) {
            throw new CanNotPushSystemEventException(e);
        }
    } // pushConfirmationSimpleTextEvent.

    /////////////////////////////////

    /**
     * Sends a Simple rich media Confirmation event (by now just html string) (CONFIRMATION_MESSAGE + RICH_MEDIA_MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param callbackOnYes String callback to call when users confirm the message (it is a must)
     * @param callbackOnNo String  callback to call when users does not confirm the message (it is an optional, could be null)
     * @param message Object a rich media
     */
    public void pushConfirmationRichMediaEvent (final String callbackOnYes, final String callbackOnNo, final Object message) {

        this.pushConfirmationRichMediaEvent(callbackOnYes, callbackOnNo, message, Collections.emptyList());
    } // pushConfirmationRichMediaEvent.

    /**
     * Sends a Simple rich media Confirmation event (by now just html string) (CONFIRMATION_MESSAGE + RICH_MEDIA_MESSAGE), it will be send to all user, not matter what portlet is looking.
     * @param callbackOnYes String callback to call when users confirm the message (it is a must)
     * @param callbackOnNo String  callback to call when users does not confirm the message (it is an optional, could be null)
     * @param message  {@link Object} a rich media
     * @param userId    String user to send the message
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushConfirmationRichMediaEvent (final String callbackOnYes, final String callbackOnNo, final Object message, final String userId, final String... portletIds) {

        this.pushConfirmationRichMediaEvent(callbackOnYes, callbackOnNo, message, Arrays.asList(userId), portletIds);
    } // pushConfirmationRichMediaEvent.

    /**
     * sends a simple rich media confirmation event (by now just html string) (confirmation_message + rich_media_message) based on the parameters
     * @param callbackOnYes String callback to call when users confirm the message (it is a must)
     * @param callbackOnNo String  callback to call when users does not confirm the message (it is an optional, could be null)
     * @param message            {@link Object} a rich media message
     * @param users   user or list of user you want to send the message (null or empty means, send the message to all users)
     * @param portletIds String array of portlet id that the message applies, could be null (null means all of them)
     *                   the concept of portlet basically means, if the user is working on a specific portlet when the message is delivered, the message will be showed to the user,
     *                   otherwise even if the message is for that particular user, if it is not in the portlet will be skipped. (note: this validation will happens on the client, Angular or whatever is the consumer)
     */
    public void pushConfirmationRichMediaEvent (final String callbackOnYes, final String callbackOnNo,
                                                 final Object message, final List<String> users, final String... portletIds) {

        final Set<String> portletIdSet = (null != portletIds) ? new HashSet<>(Arrays.asList(portletIds)) : null;
        final Payload payload = this.createPayload(new SystemConfirmationMessage(callbackOnYes, callbackOnNo,
                SystemEventType.RICH_MEDIA_MESSAGE, new SystemMessage(message, portletIdSet)), users);

        try {

            this.systemEventsAPI.push(new SystemEvent(SystemEventType.CONFIRMATION_MESSAGE,
                    payload));
        } catch (DotDataException e) {
            throw new CanNotPushSystemEventException(e);
        }
    } // pushConfirmationRichMediaEvent.

    ///**************

    private Payload createPayload (final Object message,
                                   final List<String> users, final String... portletIds) {

        final Set<String> portletIdSet = (null != portletIds) ? new HashSet<>(Arrays.asList(portletIds)) : null;
        return this.createPayload(new SystemMessage(message, portletIdSet), users);
    }

    private Payload createPayload (final SystemMessage message,
                                   final List<String> users) {

        final Visibility visibility = (null == users || users.isEmpty())  ? Visibility.GLOBAL : Visibility.USERS;
        final Object visibilityValue = (null == users || users.isEmpty()) ? null : users;

        return new Payload(message, visibility, visibilityValue);
    }
    private void pushSimpleEvent (final SystemEventType systemEventType, final Object message,
                                  final List<String> users, final String... portletIds) {

        try {

            this.systemEventsAPI.push(new SystemEvent(systemEventType,
                    this.createPayload(message, users, portletIds)));
        } catch (DotDataException e) {
            throw new CanNotPushSystemEventException(e);
        }
    } // pushSimpleTextEvent.
} // E:O:F:SystemMessageEventUtil
