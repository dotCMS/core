package com.dotmarketing.util;


import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Test;

import com.dotcms.UnitTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

public class ContentTypeUtilTest extends UnitTestBase {

    @Test
    public void testGetActionUrl() throws DotDataException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        LayoutAPI layoutAPI = mock(LayoutAPI.class);
        LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        LanguageAPI languageAPI = mock(LanguageAPI.class);
        User user = mock(User.class);
        Structure structure = mock(Structure.class);
        Language language = mock(Language.class);
        HttpServletRequestThreadLocal httpServletRequestThreadLocal = mock(HttpServletRequestThreadLocal.class);

        ContentTypeUtil contentTypeUtil = new ContentTypeUtil(layoutAPI, languageAPI,
                httpServletRequestThreadLocal, loginService);

        Layout layout = new Layout();
        layout.setPortletIds(list("1"));
        layout.setId("2");
        List<Layout> layouts = list(layout);

        when(structure.getStructureType()).thenReturn(1);
        when(structure.getInode()).thenReturn("38a3f133-85e1-4b07-b55e-179f38303b90");
        when(structure.getModDate()).thenReturn(new Date());
        when(structure.getIDate()).thenReturn(new Date());
        when(structure.getName()).thenReturn("testSt");
        when(structure.getVelocityVarName()).thenReturn("testSt");
        when(layoutAPI.loadLayoutsForUser(user)).thenReturn(layouts);
        when(request.getSession()).thenReturn(session);
        when(request.getServerName()).thenReturn("localhost");
        when(session.getAttribute(WebKeys.CTX_PATH)).thenReturn("/ctx");
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(user);
        when(languageAPI.getLanguage("en", "US")).thenReturn(language);
        when(language.getId()).thenReturn(1l);
        when(httpServletRequestThreadLocal.getRequest()).thenReturn(request);
        when(loginService.getLoggedInUser(request)).thenReturn(user);

        String expected = "http://localhost:0/ctx/portal_public/layout?p_l_id=2&p_p_id=1&p_p_action=1&p_p_state=maximized&_1_inode=&_1_cmd=new&_1_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&selectedStructure=38a3f133-85e1-4b07-b55e-179f38303b90&lang=1";

        String actionUrl = contentTypeUtil.getActionUrl(new StructureTransformer(structure).from());
        assertEquals(expected, actionUrl);
    }
}
