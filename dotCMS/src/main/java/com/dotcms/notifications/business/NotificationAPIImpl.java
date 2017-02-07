package com.dotcms.notifications.business;

import com.dotcms.api.system.event.*;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.notifications.bean.*;
import com.dotcms.notifications.dto.NotificationDTO;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.I18NMessage;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Concrete implementation of the {@link NotificationAPI} class.
 *
 * @author Daniel Silva
 * @version 3.0, 3.7
 * @since Feb 3, 2014
 *
 */
public class NotificationAPIImpl implements NotificationAPI {

    public static final String NOTIFICATIONS_THREAD_POOL_SUBMITTER_NAME = "notifications";
    private final NotificationFactory notificationFactory;
    private final SystemEventsAPI systemEventsAPI;
    private final MarshalUtils marshalUtils;
    private final ConversionUtils conversionUtils;
    private final DotConcurrentFactory dotConcurrentFactory;
    private final DotSubmitter dotSubmitter;
    private final NewNotificationCache newNotificationCache;
    private RoleAPI roleAPI;//Don't access this attribute directly, use the getRoleAPI method instead

    /**
     * Retrieve the factory class that interacts with the database.
     */
    public NotificationAPIImpl() {

        this(FactoryLocator.getNotificationFactory(),
            APILocator.getSystemEventsAPI(),
            MarshalFactory.getInstance().getMarshalUtils(),
            ConversionUtils.INSTANCE,
            DotConcurrentFactory.getInstance(),
            CacheLocator.getNewNotificationCache(),
            null);
    }

    @VisibleForTesting
    public NotificationAPIImpl(final NotificationFactory notificationFactory,
                               final SystemEventsAPI systemEventsAPI,
                               final MarshalUtils marshalUtils,
                               final ConversionUtils conversionUtils,
                               final DotConcurrentFactory dotConcurrentFactory,
                               final NewNotificationCache newNotificationCache,
                               final RoleAPI roleAPI) {

        this.notificationFactory = notificationFactory;
        this.systemEventsAPI = systemEventsAPI;
        this.marshalUtils = marshalUtils;
        this.conversionUtils = conversionUtils;
        this.dotConcurrentFactory = dotConcurrentFactory;

        // getting the thread pool for notifications.
        this.dotSubmitter = this.dotConcurrentFactory.getSubmitter(NOTIFICATIONS_THREAD_POOL_SUBMITTER_NAME); // by default use the standard configuration, but it can be override via properties config.

        this.newNotificationCache = newNotificationCache;
        this.roleAPI = roleAPI;
    }

    /**
     * Utility method to lazy load the RoleAPI when needed and to avoid to instantiate it on the creation of this
     * NotificationAPI object in order to avoid cyclic references with another APIs(java.lang.StackOverflowError).
     * <p>
     * <br>
     * <strong>Example of a cyclic reference:</strong> This NotificationAPI obtains an instance of the RoleAPI that
     * at the same time obtains an instance of the UserAPI that uses the NotificationAPI resulting on a
     * java.lang.StackOverflowError error.
     *
     * @return The RoleAPI reference
     */
    private RoleAPI getRoleAPI() {

        if ( null == this.roleAPI ) {
            synchronized (this) {
                if ( null == this.roleAPI ) {
                    this.roleAPI = APILocator.getRoleAPI();
                }
            }
        }

        return roleAPI;
    }

    @Override
    public void info(String message, String userId) {
        try {
            generateNotification(message, NotificationLevel.INFO, userId);
        } catch (DotDataException e) {
            Logger.error(this, "Error generating INFO Notification", e);
        }
    }

    @Override
    public void error(String message, String userId) {
        try {
            generateNotification(message, NotificationLevel.ERROR, userId);
        } catch (DotDataException e) {
            Logger.error(this, "Error generating ERROR Notification", e);
        }
    }

    @Override
    public void generateNotification(String message, NotificationLevel level, String userId) throws DotDataException {

        this.generateNotification(message, level, NotificationType.GENERIC, userId);
    }

    @Override
    public void generateNotification(String message, NotificationLevel level, NotificationType type, String userId) throws DotDataException {

        this.generateNotification(StringUtils.EMPTY, message, null, level, NotificationType.GENERIC, userId);
    }

    @Override
    public void generateNotification(String title, String message, List<NotificationAction> actions,
                                     NotificationLevel level, NotificationType type, String userId) throws DotDataException {

        this.generateNotification(new I18NMessage(title), new I18NMessage(message), actions, level, type, Visibility.USER, userId, userId, null);
    }

    @Override
    public void generateNotification(final I18NMessage title, final I18NMessage message, final List<NotificationAction> actions,
                                     final NotificationLevel level, final NotificationType type, final String userId, final Locale locale) throws DotDataException {
        this.generateNotification(title, message, actions, level, type, Visibility.USER, userId, userId, locale);
    }

    @Override
    public void generateNotification(final String title, final String message, final List<NotificationAction> actions,
                                     final NotificationLevel level, final NotificationType type, Visibility visibility,
                                     final String visibilityId, final String userId, final Locale locale) throws DotDataException {

        this.generateNotification(new I18NMessage(title), new I18NMessage(message), actions, level, type, visibility, visibilityId, userId, locale);
    }

    @Override
    public void generateNotification(final I18NMessage title, final I18NMessage message, final List<NotificationAction> actions,
                                     final NotificationLevel level, final NotificationType type, Visibility visibility,
                                     final String visibilityId, final String userId, final Locale locale) throws DotDataException {

        // since the notification is not a priory process on the current thread, we decided to execute it async
        this.dotSubmitter.execute(() -> {

            final NotificationData data = new NotificationData(title, message, actions);
            final String messageJson = this.marshalUtils.marshal(data);
            final NotificationDTO dto = new NotificationDTO(StringUtils.EMPTY, messageJson,
                type.name(), level.name(), userId, null, Boolean.FALSE);

            try {

                //Start a transaction
                HibernateUtil.startTransaction();

                Logger.debug(NotificationAPIImpl.class, "Storing the notification: " + dto);

                //If the is Visibility.ROLE we need to create a notification for each user under that role
                if ( visibility.equals(Visibility.ROLE) ) {
                    //Search for all the users in the given role
                    Collection<User> foundUsers = getRoleAPI().findUsersForRole(visibilityId);
                    if ( foundUsers != null && !foundUsers.isEmpty() ) {
                        this.notificationFactory.saveNotificationsForUsers(dto, foundUsers);
                    }
                } else if ( visibility.equals(Visibility.USER) ) {
                    this.notificationFactory.saveNotification(dto);
                } else {
                    throw new NotImplementedException("Visibility no implemented [" + visibility + "]");
                }

                // Adding notification to System Events table
                final Notification notificationBean = new Notification(level, userId, data);
                final Payload payload = new Payload(notificationBean, visibility, visibilityId);

                notificationBean.setGroupId(dto.getGroupId());
                notificationBean.setTimeSent(new Date());
                notificationBean.setPrettyDate(DateUtil.prettyDateSince(notificationBean.getTimeSent(), locale));

                final SystemEvent systemEvent = new SystemEvent(SystemEventType.NOTIFICATION, payload);
                this.systemEventsAPI.push(systemEvent);
                Logger.debug(NotificationAPIImpl.class, "Pushing the event: " + systemEvent);

                //Everything ok..., committing the transaction
                HibernateUtil.commitTransaction();

            } catch (Exception e) {

                try {
                    //On error rolling back the changes
                    HibernateUtil.rollbackTransaction();
                } catch (DotHibernateException hibernateException) {
                    if ( Logger.isErrorEnabled(NotificationAPIImpl.class) ) {
                        Logger.error(NotificationAPIImpl.class, hibernateException.getMessage(), hibernateException);
                    }
                }

                if ( Logger.isErrorEnabled(NotificationAPIImpl.class) ) {
                    Logger.error(NotificationAPIImpl.class, e.getMessage(), e);
                }
            }

        });
    } // generateNotification.

    @Override
    public Notification findNotification(final String userId, final String groupId) throws DotDataException {

        Notification notification =
            this.newNotificationCache.getNotification(userId, groupId);

        if (null == notification) {

            synchronized (this) {

                if (null == notification) {

                    final NotificationDTO dto =
                        this.notificationFactory.findNotification(userId, groupId);

                    notification = this.conversionUtils.convert(dto, (NotificationDTO record) -> {
                        return convertNotificationDTO(record);
                    });

                    if(notification!=null){
                        this.newNotificationCache.addNotification(notification);
                    }
                }
            }
        }

        return notification;
    } // findNotification.

    @Override
    public void deleteNotification(final String userId, final String groupId) throws DotDataException {

        synchronized (this) {

            boolean localTransaction = false;

            try {

                //Check for a transaction and start one if required
                localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

                this.notificationFactory.deleteNotification(userId, groupId);
                this.newNotificationCache.removeNotification(userId, groupId);
                this.newNotificationCache.remove(userId);

                //Everything ok..., committing the transaction
                if ( localTransaction ) {
                    HibernateUtil.commitTransaction();
                }
            } catch (Exception e) {

                try {
                    //On error rolling back the changes
                    if ( localTransaction ) {
                        HibernateUtil.rollbackTransaction();
                    }
                } catch (DotHibernateException hibernateException) {
                    Logger.error(NotificationAPIImpl.class, hibernateException.getMessage(), hibernateException);
                }

                final String msg = "An error occurred when deleting Notification.";
                Logger.error(this, msg, e);
                throw new DotDataException(msg, e);
            }
        }
    } // deleteNotification.


    @Override
    public void deleteNotifications(final String userId, final String... groupsId) throws DotDataException {

        synchronized (this) {

            boolean localTransaction = false;

            try {

                //Check for a transaction and start one if required
                localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

                this.notificationFactory.deleteNotification(userId, groupsId);

                for ( String groupId : groupsId ) {
                    this.newNotificationCache.removeNotification(userId, groupId);
                }

                this.newNotificationCache.remove(userId);

                //Everything ok..., committing the transaction
                if ( localTransaction ) {
                    HibernateUtil.commitTransaction();
                }
            } catch (Exception e) {

                try {
                    //On error rolling back the changes
                    if ( localTransaction ) {
                        HibernateUtil.rollbackTransaction();
                    }
                } catch (DotHibernateException hibernateException) {
                    Logger.error(NotificationAPIImpl.class, hibernateException.getMessage(), hibernateException);
                }

                final String msg = "An error occurred when deleting Notifications for user [" + userId + "]";
                Logger.error(this, msg, e);
                throw new DotDataException(msg, e);
            }
        }
    } // deleteNotifications.

    @Override
    public void deleteNotifications(final String userId) throws DotDataException {

        synchronized (this) {

            boolean localTransaction = false;

            try {

                //Check for a transaction and start one if required
                localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

                this.notificationFactory.deleteNotifications(userId);
                this.newNotificationCache.remove(userId);

                //Everything ok..., committing the transaction
                if ( localTransaction ) {
                    HibernateUtil.commitTransaction();
                }
            } catch (Exception e) {

                try {
                    //On error rolling back the changes
                    if ( localTransaction ) {
                        HibernateUtil.rollbackTransaction();
                    }
                } catch (DotHibernateException hibernateException) {
                    Logger.error(NotificationAPIImpl.class, hibernateException.getMessage(), hibernateException);
                }

                final String msg = "An error occurred when deleting Notifications for user [" + userId + "]";
                Logger.error(this, msg, e);
                throw new DotDataException(msg, e);
            }
        }
    } // deleteNotifications.

    @Override
    public List<Notification> getNotifications(final long offset,
                                               final long limit) throws DotDataException {

        List<Notification> notifications =
            this.newNotificationCache.getNotifications(offset, limit);

        if (null == notifications) {

            synchronized (this) {

                if (null == notifications) {

                    final List<NotificationDTO> dtos = this.notificationFactory.getNotifications(offset, limit);
                    notifications = this.conversionUtils.convert(dtos, (NotificationDTO record) -> {
                        return convertNotificationDTO(record);
                    });

                    this.newNotificationCache.addNotifications(offset, limit, notifications);
                }
            }
        }

        return notifications;
    } // getNotifications.

    @Override
    public Long getNotificationsCount() throws DotDataException {

        Long count = this.newNotificationCache.getAllCount();

        if (!UtilMethods.isSet(count)) {

            synchronized (this) {

                if (!UtilMethods.isSet(count)) {

                    count = this.notificationFactory.getNotificationsCount(null);
                    this.newNotificationCache.addAllCount(count);
                }
            }
        }

        return count;
    } // getNotificationsCount.

    @Override
    public Long getNotificationsCount(final String userId) throws DotDataException {

        Long count = this.newNotificationCache.getUserCount(userId);

        if (!UtilMethods.isSet(count)) {

            synchronized (this) {

                if (!UtilMethods.isSet(count)) {

                    count = notificationFactory.getNotificationsCount(userId);
                    this.newNotificationCache.addUserCount(userId, count);
                }
            }
        }

        return count;
    } // getNotificationsCount.

    @Override
    public List<Notification> getAllNotifications(final String userId) throws DotDataException {

        List<Notification> notifications =
            this.newNotificationCache.getAllNotifications(userId);

        if (null == notifications) {

            synchronized (this) {

                if (null == notifications) {

                    final List<NotificationDTO> dtos = this.notificationFactory.getAllNotifications(userId);
                    notifications = this.conversionUtils.convert(dtos, (NotificationDTO record) -> {
                        return convertNotificationDTO(record);
                    });

                    this.newNotificationCache.addAllNotifications(userId, notifications);
                }
            }
        }

        return notifications;
    } // getAllNotifications.

    @Override
    public List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException {

        List<Notification> notifications =
            this.newNotificationCache.getNotifications(userId, offset, limit);

        if (null == notifications) {

            synchronized (this) {

                if (null == notifications) {

                    final List<NotificationDTO> dtos = this.notificationFactory.getNotifications(userId, offset, limit);
                    notifications = this.conversionUtils.convert(dtos, (NotificationDTO record) -> {
                        return convertNotificationDTO(record);
                    });

                    this.newNotificationCache.addNotifications(userId, offset, limit, notifications);
                }
            }
        }

        return notifications;
    } // getNotifications.

    @Override
    public Long getNewNotificationsCount(final String userId) throws DotDataException {

        Long count = this.newNotificationCache.getCount(userId);

        if (!UtilMethods.isSet(count)) {

            synchronized (this) {

                if (!UtilMethods.isSet(count)) {

                    count = notificationFactory.getNewNotificationsCount(userId);
                    this.newNotificationCache.addCount(userId, count);
                }
            }
        }

        return count;
    } // getNewNotificationsCount.

    @Override
    public void markNotificationsAsRead(final String userId) throws DotDataException {

        synchronized (this) {

            boolean localTransaction = false;

            try {

                //Check for a transaction and start one if required
                localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

                this.notificationFactory.markNotificationsAsRead(userId);
                this.newNotificationCache.remove(userId);

                //Everything ok..., committing the transaction
                if ( localTransaction ) {
                    HibernateUtil.commitTransaction();
                }
            } catch (Exception e) {

                try {
                    //On error rolling back the changes
                    if ( localTransaction ) {
                        HibernateUtil.rollbackTransaction();
                    }
                } catch (DotHibernateException hibernateException) {
                    Logger.error(NotificationAPIImpl.class, hibernateException.getMessage(), hibernateException);
                }

                final String msg = "An error occurred marking Notifications as read for user [" + userId + "]";
                Logger.error(this, msg, e);
                throw new DotDataException(msg, e);
            }
        }
    } // markNotificationsAsRead.

    /**
     * Converts the physical representation of a Notification (i.e., the
     * information as stored in the database) to the logical representation.
     *
     * @param record
     *            - The {@link NotificationDTO} object.
     * @return The {@link Notification} object.
     */
    private Notification convertNotificationDTO(final NotificationDTO record) {

        final String group_id = record.getGroupId();
        final String typeStr = record.getType();
        final String levelStr = record.getLevel();
        final NotificationType type = (UtilMethods.isSet(typeStr)) ? NotificationType.valueOf(typeStr)
            : NotificationType.GENERIC;
        final NotificationLevel level = (UtilMethods.isSet(levelStr)) ? NotificationLevel.valueOf(levelStr)
            : NotificationLevel.INFO;
        final String userId = record.getUserId();
        final Date timeSent = record.getTimeSent();
        final boolean wasRead = record.getWasRead();
        NotificationData data;

        try {

            data = this.marshalUtils.unmarshal(record.getMessage(), NotificationData.class);
        } catch (Exception e) {
            // If JSON cannot be parsed, just put the message
            data = new NotificationData(
                new I18NMessage(StringUtils.EMPTY),
                new I18NMessage(record.getMessage()),
                null);
        }
        return new Notification(group_id, type, level, userId, timeSent, wasRead, data);
    } // convertNotificationDTO.

} // E:O:F:NotificationAPIImpl.