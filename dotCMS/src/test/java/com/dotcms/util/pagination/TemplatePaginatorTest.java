package com.dotcms.util.pagination;

import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.rest.api.v1.template.TemplateHelper;
import com.dotcms.rest.api.v1.template.TemplateView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.Map;
import org.junit.Test;

/**
 * test {@link TemplatePaginator}
 */
public class TemplatePaginatorTest {

    /**
     * Method to test: getItems
     * Given Scenario: Just recovery an empty list of elements
     * ExpectedResult: Perform ok, just with zero results
     *
     */
    @Test
    public void testGetEmptyTemplateList() throws DotDataException, DotSecurityException {

        final int totalRecords = 10;
        final User user = mock(User.class);
        final String filter = "filter";
        final Map<String, Object> params = map("filter", filter);
        final String hostId = "1";
        final int offset = 5;
        final int limit = 10;
        final String orderby = "title";

        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(hostId);

        final PaginatedArrayList<Template> templatesExpected = new PaginatedArrayList<>();
        templatesExpected.setTotalResults(totalRecords);

        final TemplateAPI templateAPI     = mock(TemplateAPI.class);
        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final RoleAPI       roleAPI       = mock(RoleAPI.class);
        final ContainerAPI containerAPI = mock(ContainerAPI.class);

        when(templateAPI.findTemplates(user, false, params, hostId,
                null, null, null, offset, limit, "title asc")).thenReturn(templatesExpected);

        final TemplatePaginator templatePaginator = new TemplatePaginator(templateAPI, new TemplateHelper(permissionAPI, roleAPI,containerAPI));

        final PaginatedArrayList<TemplateView> templateViews = templatePaginator.getItems(user, filter, limit, offset, orderby,
                OrderDirection.ASC, map(ContainerPaginator.HOST_PARAMETER_ID, hostId));

        assertEquals(templateViews.getTotalResults(), totalRecords);
    }

}
