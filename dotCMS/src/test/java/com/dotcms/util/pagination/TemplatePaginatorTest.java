package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.template.TemplateHelper;
import com.dotcms.rest.api.v1.template.TemplateView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        final Map<String, Object> params = Map.of("filter", filter);
        final String hostId = "1";
        final int offset = 5;
        final int limit = 10;
        final String orderby = "title";

        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(hostId);

        final TemplateAPI templateAPI     = mock(TemplateAPI.class);
        final TemplateHelper templateHelper = mock(TemplateHelper.class);
        final TemplateView templateView = mock(TemplateView.class);

        final PaginatedArrayList<Template> templatesExpected = new PaginatedArrayList<>();
        for(int i=0;i<10;i++){
            final Template template = mock(Template.class);
            templatesExpected.add(template);
            when(templateHelper.toTemplateView(template,user)).thenReturn(templateView);
        }
        templatesExpected.setTotalResults(totalRecords);


        when(templateAPI.findTemplates(user, false, params, host.getIdentifier(),
                null, null, null, offset, limit, "title asc")).thenReturn(templatesExpected);
        when(templateAPI.findTemplates(user, false, params, host.getIdentifier(),
                null, null, null, 0, -1, "title asc")).thenReturn(templatesExpected);

        final TemplatePaginator templatePaginator = new TemplatePaginator(templateAPI, templateHelper);

        final PaginatedArrayList<TemplateView> templateViews = templatePaginator.getItems(user, filter, limit, offset, orderby,
                OrderDirection.ASC, Map.of(ContainerPaginator.HOST_PARAMETER_ID, host.getIdentifier()));

        assertEquals(totalRecords, templateViews.getTotalResults());
    }

}
