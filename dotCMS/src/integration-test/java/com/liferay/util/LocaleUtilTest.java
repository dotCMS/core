package com.liferay.util;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserFirstNameException;
import com.liferay.portal.UserLastNameException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerImpl;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

/**
 * Locale Util test.
 *
 * @author jsanca
 */
public class LocaleUtilTest  {

    @Test
    public void testFromLanguageId() {

        final Locale defaultLocale = LocaleUtil.fromLanguageId(null);

        assertNotNull(defaultLocale);
        assertEquals(Locale.getDefault(), defaultLocale);

        final Locale emptyLocale = LocaleUtil.fromLanguageId("");

        assertNotNull(emptyLocale);
        assertEquals(Locale.getDefault(), emptyLocale);

        final Locale invalidLocale = LocaleUtil.fromLanguageId("invalid");

        assertNotNull(invalidLocale);
        assertEquals(Locale.getDefault(), invalidLocale);

        final Locale enLocale = LocaleUtil.fromLanguageId("en");

        assertNotNull(enLocale);
        assertEquals(Locale.getDefault(), enLocale);

        final Locale usLocale = LocaleUtil.fromLanguageId("US");

        assertNotNull(usLocale);
        assertEquals(Locale.getDefault(), usLocale);

        final Locale enUsLocale = LocaleUtil.fromLanguageId("en_US");

        assertNotNull(enUsLocale);
        assertEquals("en_US", enUsLocale.toString());
    }




    @Test
    public void testGetLocale() {

        final UserAPI userAPI = mock(UserAPI.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final com.dotcms.util.security.Encryptor encryptor =
                mock(com.dotcms.util.security.Encryptor.class);

        LocaleUtil.setUserAPI(userAPI);
        LocaleUtil.setUserWebAPI(userWebAPI);
        LocaleUtil.setEncryptor(encryptor);


        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpSession session  = mock(HttpSession.class);

        when(request.getLocale()).thenReturn(Locale.getDefault());
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(null); // no locale

        final Locale defaultLocale = LocaleUtil.getLocale(request, null, null);
        assertNotNull(defaultLocale);
        assertEquals(Locale.getDefault(), defaultLocale);

        final HttpServletRequest request2  = mock(HttpServletRequest.class);
        final HttpSession session2  = mock(HttpSession.class);

        when(request2.getLocale()).thenReturn(Locale.getDefault());
        when(request2.getSession(false)).thenReturn(session2);
        when(session2.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale("en","UK")); // no locale

        final Locale enUKLocale = LocaleUtil.getLocale(request2, null, null);
        assertNotNull(enUKLocale);
        assertEquals("en_UK", enUKLocale.toString());


        final Locale usLocale = LocaleUtil.getLocale(request, "US", null);
        assertNotNull(usLocale);
        assertEquals("_US", usLocale.toString());


        final Locale enLocale = LocaleUtil.getLocale(request, null, "en");
        assertNotNull(enLocale);
        assertEquals("en", enLocale.toString());


        final Locale enUsLocale = LocaleUtil.getLocale(request, "US", "en");
        assertNotNull(enUsLocale);
        assertEquals("en_US", enUsLocale.toString());

        final Locale wrongLocale = LocaleUtil.getLocale(request, "XXXXX", "YYYYYYYY");
        assertNotNull(wrongLocale);
        assertEquals(Locale.getDefault(), wrongLocale);
    }
}
