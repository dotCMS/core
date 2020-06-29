package com.dotcms.visitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.UnitTestBase;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.business.VisitorAPIImpl;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPIImpl;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.WebKeys;

public class VisitorAPITest extends UnitTestBase {


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetVisitor_WhenNullRequest_ThrowsException() {
      new VisitorAPIImpl().getVisitor(null);
    }

    @Test
    public void testGetVisitor_WhenNullSessionAndCreateEqualsFalse_ReturnEmptyVisitor() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getSession(false)).thenReturn(null);
        Optional<Visitor> visitor = new VisitorAPIImpl().getVisitor(mockRequest, false);
        assertFalse(visitor.isPresent());
    }

    @Test
    public void testGetVisitor_WhenCreateEqualsTrue_ReturnVisitor() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpSession mockSession = mock(HttpSession.class);
        when(mockRequest.getSession()).thenReturn(mockSession);

        LanguageWebAPI mockLanguageWebAPI = mock(LanguageWebAPI.class);
        when(mockLanguageWebAPI.getLanguage(mockRequest)).thenReturn(getLanguage());

        new VisitorAPIImpl(mockLanguageWebAPI,new PersonaAPIImpl());
        Optional<Visitor> visitor = new VisitorAPIImpl().getVisitor(mockRequest, true);
        verify(mockRequest).getSession();
        assertTrue(visitor.isPresent());
        verify(mockSession).setAttribute(WebKeys.VISITOR, visitor.get());
    }

    /**
     * Should remove {@link WebKeys#VISITOR} from session
     */
    @Test
    public void testRemoveVisitor() {
        HttpSession mockSession = mock(HttpSession.class);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getSession(false)).thenReturn(mockSession);

        new VisitorAPIImpl().removeVisitor(mockRequest);
        verify(mockSession).removeAttribute(WebKeys.VISITOR);
    }

    private Language getLanguage() {
        Language language = new Language(1);
        language.setCountry("United States");
        language.setCountryCode("US");
        language.setLanguage("English");
        language.setLanguageCode("en");
        return language;
    }
    
    /**
     * this tests that we are accuring the personas that have been associated to the user
     * and are giving the correct values (percentage of persona) back
     */
    @Test
    public void test_get_visitor_personas() {
        HttpServletRequest mockRequest = new MockHttpRequest("testing", "/").request();

        final Persona persona1 = mock(Persona.class);
        when(persona1.getKeyTag()).thenReturn("persona1");

            
        final Persona persona2 = mock(Persona.class);
        when(persona2.getKeyTag()).thenReturn("persona2");

        
        final Persona persona3 = mock(Persona.class);
        when(persona3.getKeyTag()).thenReturn("persona3");


        LanguageWebAPI mockLanguageWebAPI = mock(LanguageWebAPI.class);
        when(mockLanguageWebAPI.getLanguage(mockRequest)).thenReturn(getLanguage());

        VisitorAPI vapi = new VisitorAPIImpl(mockLanguageWebAPI,new PersonaAPIImpl());
        Optional<Visitor> visitorOpt = vapi.getVisitor(mockRequest, true);

        assertTrue("Visitor is present", visitorOpt.isPresent());
        
        Visitor visitor=visitorOpt.get();
        
        assertTrue("we should not have a persona",visitor.getPersona()==null);
        assertTrue("we should not have any personas",visitor.getPersonas().size()==0);
        
        visitor.setPersona(persona1);
        assertTrue("persona1 should be set in the visitor",visitor.getPersona()==persona1);
        
        visitor.setPersona(persona2);
        assertTrue("persona2 should be set in the visitor",visitor.getPersona()==persona2);
        
        assertTrue("persona1 should be 1/2  in the visitor.personas",visitor.getWeightedPersonas().get("persona1") == .5f);
        assertTrue("persona1 should be 1 in the visitor.personas",visitor.getPersonaCounts().get("persona1") == 1);
        
        
        
        assertTrue("persona2 should be 1/2  in the visitor.personas",visitor.getWeightedPersonas().get("persona2") == .5f);
        assertTrue("persona2 should be 1  in the visitor.personas",visitor.getPersonaCounts().get("persona2") == 1);
        
        
        visitor.setPersona(persona3);
        assertTrue("persona3 should be set in the visitor",visitor.getPersona()==persona3);
        assertTrue("personas should 3",visitor.getPersonas().size() == 3);
        
        // add them up
        visitor.setPersona(persona2);
        visitor.setPersona(persona2);
        visitor.setPersona(persona3);
        visitor.setPersona(persona3);
        visitor.setPersona(persona3);

        assertTrue("persona1 should be 1  in the visitor.personas",visitor.getPersonaCounts().get("persona1") == 1);
        assertTrue("persona2 should be 3  in the visitor.personas",visitor.getPersonaCounts().get("persona2") == 3);
        assertTrue("persona3 should be 4  in the visitor.personas",visitor.getPersonaCounts().get("persona3") == 4);
        
        
        assertTrue("persona1 should be 1/8  in the visitor.personas",visitor.getWeightedPersonas().get("persona1") == .125f);
        assertTrue("persona2 should be 3/8  in the visitor.personas",visitor.getWeightedPersonas().get("persona2") == .375f);
        assertTrue("persona3 should be 1/2  in the visitor.personas",visitor.getWeightedPersonas().get("persona3") == .5f);
        visitor.accruePersona(persona3);
        assertTrue("persona3 should be 5  in the visitor.personas",visitor.getPersonaCounts().get("persona3") == 5);
    }
    
    
    
    
    
}
