package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.RestDropdownInput;
import com.liferay.portal.model.User;
import java.util.List;
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
    public static final String REQUEST_VALUE = "requestValue";
    private PersonaAPI personaAPI;

    public PersonaActionlet() {
        super(I18N_BASE,
              new ParameterDefinition<>(1,
                                        PERSONA_ID_KEY,
                                        new RestDropdownInput("/api/v1/personas", "key", "name")));
        personaAPI = APILocator.getPersonaAPI();
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        try {
            Optional<Visitor> opt = APILocator.getVisitorAPI().getVisitor(request);
            if(opt.isPresent()){
                User user = APILocator.getUserAPI().getSystemUser();
                Persona p = personaAPI.find(instance.key, user, false);
                opt.get().setPersona(p);
            }
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate action %s", this.getClass().getName());
        }

        return true;
    }

    static class Instance implements RuleComponentInstance {

        private final String key;

        public Instance(Map<String, ParameterModel> parameters) {
            key = parameters.get(PERSONA_ID_KEY).getValue();
            Preconditions.checkArgument(StringUtils.isNotBlank(key), "Set Persona Actionlet requires valid persona ID.");
        }
    }
}
