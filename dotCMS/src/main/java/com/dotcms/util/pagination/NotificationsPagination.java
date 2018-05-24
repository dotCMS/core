package com.dotcms.util.pagination;

/***
 * This class includes an additional field specifically needed on the notifications
 */
public class NotificationsPagination extends Pagination {

    private final Number unreadNotifications;

    NotificationsPagination(final String link, final Number perPage, final Number currentPage,
            final Number linkPages, final Number totalRecords, final Number unreadNotifications) {
        super(link, perPage, currentPage, linkPages, totalRecords);
        this.unreadNotifications = unreadNotifications;
    }

    public Number getUnreadNotifications() {
        return unreadNotifications;
    }
}
