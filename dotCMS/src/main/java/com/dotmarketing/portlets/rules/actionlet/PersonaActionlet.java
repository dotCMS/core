package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.RestDropdownInput;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add Key/Value to the Request.
 * The exact names that had to be set in params are: requestKey and requestValue.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 */
public class PersonaActionlet extends RuleActionlet<PersonaActionlet.Instance> {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetPersona";

    public static final String PERSONA_ID_KEY = "personaIdKey";
    private final VisitorAPI visitorAPI;
    private final UserAPI userAPI;
    private PersonaAPI personaAPI;

    @SuppressWarnings("unused")
    public PersonaActionlet() {
        this(APILocator.getPersonaAPI(), APILocator.getUserAPI(), APILocator.getVisitorAPI());
    }

    @VisibleForTesting
    PersonaActionlet(PersonaAPI personaAPI, UserAPI userAPI, VisitorAPI visitorAPI) {
        super(I18N_BASE,
              new ParameterDefinition<>(1,
                                        PERSONA_ID_KEY,
                                        new RestDropdownInput("/api/v1/personas", "key", "name").minSelections(1)));

        this.personaAPI = personaAPI;
        this.userAPI = userAPI;
        this.visitorAPI = visitorAPI;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        boolean result;
        try {
            Optional<Visitor> opt = visitorAPI.getVisitor(request);
            if(opt.isPresent()){
                User user = userAPI.getSystemUser();
                Persona p = personaAPI.find(instance.personaId, user, false);
                if(p == null){
                    Logger.warn(PersonaActionlet.class, "Persona with id '" + instance.personaId + "' not be found. Could not execute action.");
                    result = false;
                } else {
                    opt.get().setPersona(p);
                    result = true;
                }
            } else{
                Logger.warn(PersonaActionlet.class, "No visitor was available on which to set a persona. Could not execute action.");
                result = false;
            }
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate action %s", this.getClass().getName());
        }
        return result;
    }

    static class Instance implements RuleComponentInstance {

        private final String personaId;

        public Instance(Map<String, ParameterModel> parameters) {
            personaId = parameters.get(PERSONA_ID_KEY).getValue();
            Preconditions.checkArgument(StringUtils.isNotBlank(personaId), "Set Persona Actionlet requires valid persona ID.");
        }
    }
}
