package com.dotcms.util.pagination;

import com.liferay.portal.model.User;

import java.util.Collection;

/**
 * Define the methods to get handle a pagination request
 */
public abstract class Paginator<T> {

    public abstract long getTotalRecords(String condition);

    public abstract Collection<T> getItems(User user, String filter, boolean showArchived, int limit, int offset,
                                  String orderby, OrderDirection direction);
}