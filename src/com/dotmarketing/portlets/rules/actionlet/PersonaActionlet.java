package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.apache.commons.collections.functors.ExceptionClosure;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
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

    public PersonaActionlet() {
        super(I18N_BASE);
    }

    @Override
    public Map<String, ParameterDefinition> getParameterDefinitions() {
        Map<String, ParameterDefinition> map = Maps.newLinkedHashMap();
        map.put(PERSONA_ID_KEY, new ParameterDefinition<>(1, PERSONA_ID_KEY,
                                                          getDropdown()));



        return super.getParameterDefinitions();
    }

    private DropdownInput getDropdown() {
        DropdownInput input = new DropdownInput();

        try {
            HostAPI hostAPI = new ApiProvider().hostAPI();

            PersonaAPI personaAPI = APILocator.getPersonaAPI();
            User user = APILocator.getUserAPI().getSystemUser();
            Host systemHost = hostAPI.findSystemHost();
            List<Persona> personas = personaAPI.getPersonas(systemHost, true, false, user, true);

            Logger.error(PersonaActionlet.class, "weeee!");
        } catch (DotSecurityException | DotDataException e) {
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }

        return input;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {

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
