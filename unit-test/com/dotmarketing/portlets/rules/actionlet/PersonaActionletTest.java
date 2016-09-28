package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.liferay.portal.model.User;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotmarketing.portlets.rules.actionlet.PersonaActionlet.PERSONA_ID_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class PersonaActionletTest {

    @Test
    public void testActionletSetsPersonaOnHappyPath() throws Exception {
        TestCase test = new TestCase();
        test.withPersonaId(test.persona.getIdentifier()).withMockPersona(test.persona).withMockVisitor(test.visitor);
        PersonaActionlet pa = new PersonaActionlet(test.personaAPI, test.userAPI, test.visitorAPI);
        PersonaActionlet.Instance foo = pa.instanceFrom(test.params);
        boolean result = pa.evaluate(test.request, test.response, foo);
        assertThat("Result should be true.", result, is(true));
        verify(test.visitor, times(1)).setPersona(test.persona);
    }

    @Test
    public void testActionletSetsPersonaFailsGracefullyWhenVisitorAbsent() throws Exception {
        TestCase test = new TestCase();
        test.withPersonaId(test.persona.getIdentifier())
            .withMockPersona(test.persona)
            .withVisitorAbsent();
        PersonaActionlet pa = new PersonaActionlet(test.personaAPI, test.userAPI, test.visitorAPI);
        PersonaActionlet.Instance foo = pa.instanceFrom(test.params);
        boolean result = pa.evaluate(test.request, test.response, foo);
        assertThat("Result should be false because no persona was set.", result, is(false));
        assertThat("Persona should have been set.", test.visitor.getPersona(), nullValue());
    }

    @Test
    public void testActionletSetsPersonaFailsGracefullyWhenPersonaMissing() throws Exception {
        TestCase test = new TestCase();
        test.withPersonaId(test.persona.getIdentifier() + "Nope")
            .withMockPersona(test.persona);
        PersonaActionlet pa = new PersonaActionlet(test.personaAPI, test.userAPI, test.visitorAPI);
        PersonaActionlet.Instance foo = pa.instanceFrom(test.params);
        boolean result = pa.evaluate(test.request, test.response, foo);
        assertThat("Result should be false because no persona was set.", result, is(false));
        assertThat("Persona should have been set.", test.visitor.getPersona(), nullValue());
    }

    public static class TestCase {

        private final User user = new User("000000");
        private final Persona persona = new Persona(new Contentlet());
        private final Visitor visitor = mock(Visitor.class);
        private final HttpServletRequest request = mock(HttpServletRequest.class);
        private final HttpServletResponse response= mock(HttpServletResponse.class);
        private PersonaAPI personaAPI = mock(PersonaAPI.class);
        private UserAPI userAPI = mock(UserAPI.class);
        private VisitorAPI visitorAPI = mock(VisitorAPI.class);
        private Map<String, ParameterModel> params = Maps.newHashMap();


        public TestCase() {
            persona.setIdentifier("x-" + new Random().nextInt());
            when(visitorAPI.getVisitor(request)).thenReturn(Optional.of(visitor));
            try {
                when(userAPI.getSystemUser()).thenReturn(user);
            } catch (DotDataException e) {
                throw new RuntimeException(e);
            }
        }

        public TestCase withPersonaId(String id){
            params.put(PERSONA_ID_KEY, new ParameterModel(PERSONA_ID_KEY, id));
            return this;
        }

        public TestCase withVisitorAbsent(){
            when(visitorAPI.getVisitor(request)).thenReturn(Optional.empty());
            return this;
        }

        public TestCase withMockPersona(Persona persona) {
            try {
                when(personaAPI.find(persona.getIdentifier(), user, false)).thenReturn(persona);
            } catch (DotDataException | DotSecurityException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public TestCase withMockVisitor(Visitor visitor) {
            when(visitorAPI.getVisitor(request)).thenReturn(Optional.of(visitor));
            return this;
        }
    }



}