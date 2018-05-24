package com.dotcms.util.pagination;

import com.dotcms.notifications.NotificationConverter;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.UserNotificationPair;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.view.NotificationView;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;

public class NotificationsPaginator implements PaginatorOrdered<NotificationView> {

     static final String ALL_USERS = "allusers";

    private final NotificationAPI notificationAPI;
    private final NotificationConverter notificationConverter;
    private final ConversionUtils conversionUtils;

    public NotificationsPaginator() {
        notificationAPI = APILocator.getNotificationAPI();
        conversionUtils = ConversionUtils.INSTANCE;
        notificationConverter = new NotificationConverter();
    }

    @VisibleForTesting
    public NotificationsPaginator(final NotificationAPI notificationAPI,
            final ConversionUtils conversionUtils,
            final NotificationConverter notificationConverter) {
        this.notificationAPI = notificationAPI;
        this.conversionUtils = conversionUtils;
        this.notificationConverter = notificationConverter;
    }

    @Override
    public PaginatedArrayList<NotificationView> getItems(final User user, final String filter,
            final int limit, final int offset, final String orderBy, final OrderDirection direction,
            final Map<String, Object> extraParams)
            throws PaginationException {
        final PaginatedArrayList<NotificationView> notificationsResult = new PaginatedArrayList<>();
        try {
            final boolean allUsers = getAllUsersParam(extraParams);

            // Let's get the total count
            final Long notificationsCount = allUsers ?
                    this.notificationAPI.getNotificationsCount() :
                    this.notificationAPI.getNotificationsCount(user.getUserId());

            final List<Notification> notifications = allUsers ?
                    this.notificationAPI.getNotifications(offset, limit) :
                    this.notificationAPI.getNotifications(user.getUserId(), offset, limit);

            notificationsResult.setTotalResults(notificationsCount);
            // copy and doing some treatment.
            if (null != notifications) {

                notifications.forEach(notification -> {

                    final NotificationView notificationResult = this.conversionUtils.convert(
                            new UserNotificationPair(user, notification),
                            this.notificationConverter);

                    notificationsResult.add(notificationResult);
                });
            }

        } catch (DotDataException e) {
            throw new PaginationException(e);
        }
        return notificationsResult;
    }

    boolean getAllUsersParam(final Map<String, Object> extraParams){
        if(extraParams == null){
            return false;
        }
        return (Boolean) extraParams.getOrDefault(ALL_USERS, false);
    }

    /**
     * Need a slightly different pagination for notifications.
     *
     * @return Pagination
     */
    @Override
    public Pagination createPagination(final User user, final Map<String,Object> paginationValuesMap) throws DotDataException {
        final String link = (String)paginationValuesMap.get(Pagination.LINK);
        final Number pageSize = (Number)paginationValuesMap.get(Pagination.PER_PAGE);
        final Number currentPage = (Number)paginationValuesMap.get(Pagination.CURRENT_PAGE);
        final Number linkPages = (Number) paginationValuesMap.get(Pagination.LINK_PAGES);
        final Number totalRecords = (Number) paginationValuesMap.get(Pagination.TOTAL_RECORDS);
        final Boolean all = (Boolean) paginationValuesMap.get(ALL_USERS);
        Number count;
        if(all){
            count =  this.notificationAPI.getNotificationsCount();
        } else {
            count = this.notificationAPI.getNewNotificationsCount(user.getUserId());
        }
        return new NotificationsPagination(link, pageSize, currentPage, linkPages, totalRecords, count);
    }
}
