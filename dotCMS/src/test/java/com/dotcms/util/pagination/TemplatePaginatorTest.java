package com.dotcms.util.pagination;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test {@link TemplatePaginator}
 */
public class TemplatePaginatorTest {

    @Test
    public void testGetEmptyTemplateList() throws DotDataException, DotSecurityException {

        final int totalRecords = 10;
        final User user = mock(User.class);
        final String filter = "filter";
        final Map<String, Object> params = map("title", filter);
        final String hostId = "1";
        final int offset = 5;
        final int limit = 10;
        final String orderby = "title";

        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(hostId);

        final PaginatedArrayList<Template> templatesExpected = new PaginatedArrayList<>();
        templatesExpected.setTotalResults(totalRecords);

        final TemplateAPI templateAPI = mock(TemplateAPI.class);

        when(templateAPI.findTemplates(user, false, params, hostId,
                null, null, null, offset, limit, "title asc")).thenReturn(templatesExpected);

        final TemplatePaginator templatePaginator = new TemplatePaginator(templateAPI);

        final PaginatedArrayList<TemplateView> templateViews = templatePaginator.getItems(user, filter, limit, offset, orderby,
                OrderDirection.ASC, map(ContainerPaginator.HOST_PARAMETER_ID, hostId));

        assertEquals(templateViews.getTotalResults(), totalRecords);
    }

    @Test
    public void testGetTemplateList() throws DotDataException, DotSecurityException {

        final int totalRecords = 10;
        final User user = mock(User.class);
        final String filter = "filter";
        final Map<String, Object> params = map("title", filter);
        final String hostId = "1";
        final int offset = 5;
        final int limit = 10;
        final String orderby = "title";

        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(hostId);

        final PaginatedArrayList<Template> templatesExpected = new PaginatedArrayList<>();
        templatesExpected.add(newTemplate(1));
        templatesExpected.add(newTemplate(2));
        templatesExpected.add(newTemplate(3));
        templatesExpected.setTotalResults(totalRecords);

        final TemplateAPI templateAPI = mock(TemplateAPI.class);

        when(templateAPI.findTemplates(user, false, params, hostId,
                null, null, null, offset, limit, "title asc")).thenReturn(templatesExpected);

        final TemplatePaginator templatePaginator = new TemplatePaginator(templateAPI);

        final PaginatedArrayList<TemplateView> templateViews = templatePaginator.getItems(user, filter, limit, offset, orderby,
                OrderDirection.ASC, map(ContainerPaginator.HOST_PARAMETER_ID, hostId));

        assertEquals(templateViews.getTotalResults(), totalRecords);
        int i = 0;
        for (final TemplateView templateView : templateViews) {

            final Template template = templatesExpected.get(i++);
            assertEquals(templateView.getIdentifier(), template.getIdentifier());
            assertEquals(templateView.getInode(),      template.getInode());
            assertEquals(templateView.getBody(),       template.getBody());
            assertEquals(templateView.getTitle(),      template.getTitle());
        }
    }

    private Template newTemplate (final int sortOrder) {

        final Template template = new Template();

        template.setIdentifier(UUIDUtil.uuid());
        template.setInode(UUIDUtil.uuid());
        template.setBody("<p>This is a template: " + template.getIdentifier() + "</p>");
        template.setDrawed(false);
        template.setDrawedBody((String)null);
        template.setShowOnMenu(true);
        template.setCountAddContainer(0);
        template.setCountContainers(0);
        template.setFriendlyName("");
        template.setModDate(new Date());
        template.setModUser("dotcms.org.1");
        template.setTitle("template" + sortOrder);
        template.setSortOrder(sortOrder);

        return template;
    }

}
