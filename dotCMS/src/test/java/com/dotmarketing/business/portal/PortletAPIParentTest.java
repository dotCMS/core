package com.dotmarketing.business.portal;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.CompanyUtils;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Saving custom definitions preserves the parent and refreshes navigation only after a successful write. */
public class PortletAPIParentTest {

    /** Both a grouped tool and an explicitly cleared parent survive the business save unchanged. */
    @Test
    public void save_retainsParentAndNotifiesNavigation() throws Exception {
        for (final String parent : List.of("Identity-Access", "")) {
            final PortletFactory factory = mock(PortletFactory.class);
            final Company company = mock(Company.class);
            final LanguageAPI languages = mock(LanguageAPI.class);
            final SystemEventsAPI events = mock(SystemEventsAPI.class);
            when(company.getCompanyId()).thenReturn("dotcms.org");
            when(languages.getLanguages()).thenReturn(List.of());
            when(factory.insertPortlet(any(Portlet.class))).thenAnswer(call -> call.getArgument(0));
            try (MockedStatic<CompanyUtils> companies = mockStatic(CompanyUtils.class);
                    MockedStatic<APILocator> locator = mockStatic(APILocator.class);
                    MockedStatic<SystemMessageEventUtil> messages = mockStatic(SystemMessageEventUtil.class)) {
                companies.when(CompanyUtils::getDefaultCompany).thenReturn(company);
                locator.when(APILocator::getLanguageAPI).thenReturn(languages);
                locator.when(APILocator::getSystemEventsAPI).thenReturn(events);
                messages.when(SystemMessageEventUtil::getInstance).thenReturn(mock(SystemMessageEventUtil.class));
                final PortletAPIImpl api = new PortletAPIImpl(factory, null);
                final Portlet tool = new Portlet("c_custom", "com.liferay.portlet.StrutsPortlet",
                        Map.of("name", "Custom", "baseTypes", "CONTENT", "contentTypes", "",
                                "dataViewMode", "list", "parent", parent));

                final Portlet saved = api.savePortlet(tool, new User());
                assertEquals(parent, saved.getInitParams().get("parent"));
                assertEquals("db", saved.getInitParams().get("portletSource"));
                verify(events).push(eq(SystemEventType.UPDATE_PORTLET_LAYOUTS), any(Payload.class));
            }
        }
    }

    /** A failed write must not advertise a definition that was never saved. */
    @Test
    public void save_failureDoesNotNotifyNavigation() throws Exception {
        final PortletFactory factory = mock(PortletFactory.class);
        final Company company = mock(Company.class);
        final SystemEventsAPI events = mock(SystemEventsAPI.class);
        when(company.getCompanyId()).thenReturn("dotcms.org");
        when(factory.insertPortlet(any(Portlet.class))).thenThrow(new DotDataException("save failed"));
        try (MockedStatic<CompanyUtils> companies = mockStatic(CompanyUtils.class);
                MockedStatic<APILocator> locator = mockStatic(APILocator.class)) {
            companies.when(CompanyUtils::getDefaultCompany).thenReturn(company);
            locator.when(APILocator::getSystemEventsAPI).thenReturn(events);
            final PortletAPIImpl api = new PortletAPIImpl(factory, null);
            final Portlet tool = new Portlet("c_custom", "com.liferay.portlet.StrutsPortlet",
                    Map.of("name", "Custom", "baseTypes", "CONTENT", "contentTypes", "",
                            "dataViewMode", "list", "parent", "Identity-Access"));
            assertThrows(DotDataException.class, () -> api.savePortlet(tool, new User()));
            verify(events, never()).push(eq(SystemEventType.UPDATE_PORTLET_LAYOUTS), any(Payload.class));
        }
    }
}
