package com.dotcms.visitor;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class VisitorAPITest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetVisitorWhenNullRequestThrowsException() {
        APILocator.getVisitorAPI().getVisitor(null);
    }

    @Test
    public void testGetVisitorWhenNullSessionAndCreateEqualsFalseReturnEmptyVisitor() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getSession(false)).thenReturn(null);
        Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(mockRequest, false);
        assertFalse(visitor.isPresent());
    }

    @Test
    public Optional<Visitor> testGetVisitorWhenCreateEqualsTrueReturnVisitor() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpSession mockSession = mock(HttpSession.class);
        when(mockRequest.getSession()).thenReturn(mockSession);

        LanguageAPI mockLanguageAPI = mock(LanguageAPI.class);
        when(mockLanguageAPI.getDefaultLanguage()).thenReturn(getLanguage());
        when(mockLanguageAPI.getLanguage(1L)).thenReturn(getLanguage());

        APILocator.getVisitorAPI().setLanguageAPI(mockLanguageAPI);
        Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(mockRequest, true);
        verify(mockRequest).getSession();
        assertTrue(visitor.isPresent());
        verify(mockSession).setAttribute(WebKeys.VISITOR, visitor.get());
        return visitor;
    }

    private Language getLanguage() {
        Language language = new Language(1);
        language.setCountry("United States");
        language.setCountryCode("US");
        language.setLanguage("English");
        language.setLanguageCode("en");
        return language;
    }
}
