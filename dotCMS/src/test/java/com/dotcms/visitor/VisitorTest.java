package com.dotcms.visitor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import com.dotcms.UnitTestBase;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.business.VisitorAPIImpl;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPIImpl;
import com.dotmarketing.portlets.personas.model.Persona;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VisitorTest extends UnitTestBase {



    private Language getLanguage() {
        Language language = new Language(1);
        language.setCountry("United States");
        language.setCountryCode("US");
        language.setLanguage("English");
        language.setLanguageCode("en");
        return language;
    }



    @Test
    public void test_visitor_serializer() {

        final Persona persona1 = mock(Persona.class);
        when(persona1.getKeyTag()).thenReturn("persona1");
        when(persona1.getIdentifier()).thenReturn("123");
        when(persona1.getTitleImage()).thenReturn(Optional.empty());
        HttpServletRequest mockRequest = new MockHttpRequest("testing", "/").request();



        LanguageWebAPI mockLanguageWebAPI = mock(LanguageWebAPI.class);
        when(mockLanguageWebAPI.getLanguage(mockRequest)).thenReturn(getLanguage());

        VisitorAPI vapi = new VisitorAPIImpl(mockLanguageWebAPI, new PersonaAPIImpl());
        Optional<Visitor> visitorOpt = vapi.getVisitor(mockRequest, true);

        assertTrue("Visitor is present", visitorOpt.isPresent());

        Visitor visitor = visitorOpt.get();



        ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

        try {
            mapper.writeValueAsString(visitor);
        } catch (JsonProcessingException e) {
            assertTrue("We should not have an exception here", e == null);
        }

        visitor.setPersona(persona1);
        String value=null;
        try {
            value = mapper.writeValueAsString(visitor);
        } catch (Exception e) {
            assertTrue("We should not have an exception here, got:" + e.getMessage(), e == null);
        }
        
        assertNotNull("We have a serialzied persona", value);

        assertTrue("We have a serialzied persona", value.length()>10);
    }

}
