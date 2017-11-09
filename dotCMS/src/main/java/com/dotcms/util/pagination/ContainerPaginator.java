package com.dotcms.util.pagination;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Handle {@link com.dotmarketing.portlets.containers.model.Container} pagination
 */
public class ContainerPaginator implements Paginator<Container> {

    public static final String HOST_PARAMETER_ID = "host";

    private ContainerFactory containerFactory;

    public ContainerPaginator() {
        containerFactory = FactoryLocator.getContainerFactory();
    }

    @VisibleForTesting
    public ContainerPaginator(ContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    @Override
    public PaginatedArrayList<Container> getItems(User user, String filter, int limit, int offset, String orderby,
                                                  OrderDirection direction, Map<String, Object> extraParams) {
        String hostId = null;

        if (extraParams != null) {
            hostId = (String) extraParams.get(HOST_PARAMETER_ID);
        }

        Map<String, Object> params = map("title", filter);

        String orderByDirection = orderby;
        if (UtilMethods.isSet(direction) && UtilMethods.isSet(orderby)) {
            orderByDirection += " " + direction.toString().toLowerCase();
        }

        try {
            return (PaginatedArrayList) containerFactory.findContainers(user, false, params, hostId,
                    null, null, null, offset, limit, orderByDirection);
        } catch (DotSecurityException|DotDataException e) {
            throw new RuntimeException(e);
        }
    }
}
