package com.dotcms.visitor;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.WebKeys;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Optional;


import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class VisitorAPITest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetVisitor_WhenNullRequest_ThrowsException() {
        APILocator.getVisitorAPI().getVisitor(null);
    }

    @Test
    public void testGetVisitor_WhenNullSessionAndCreateEqualsFalse_ReturnEmptyVisitor() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getSession(false)).thenReturn(null);
        Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(mockRequest, false);
        assertFalse(visitor.isPresent());
    }

    @Test
    public void testGetVisitor_WhenCreateEqualsTrue_ReturnVisitor() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpSession mockSession = mock(HttpSession.class);
        when(mockRequest.getSession()).thenReturn(mockSession);

        LanguageWebAPI mockLanguageWebAPI = mock(LanguageWebAPI.class);
        when(mockLanguageWebAPI.getLanguage(mockRequest)).thenReturn(getLanguage());

        APILocator.getVisitorAPI().setLanguageWebAPI(mockLanguageWebAPI);
        Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(mockRequest, true);
        verify(mockRequest).getSession();
        assertTrue(visitor.isPresent());
        verify(mockSession).setAttribute(WebKeys.VISITOR, visitor.get());
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
