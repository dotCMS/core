package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.exception.RuleConstructionFailedException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.NumericInput;
import com.dotmarketing.portlets.rules.parameter.display.RestDropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.NumericType;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

public class PersonaConditionlet extends Conditionlet<PersonaConditionlet.Instance>{

	private static final long serialVersionUID = 1L;

    public static final String PERSONA_ID_KEY = "personaIdKey";
    private final VisitorAPI visitorAPI;
    private final UserAPI userAPI;
    private PersonaAPI personaAPI;


    private static final ParameterDefinition<TextType> persona = new ParameterDefinition<>(3,
            PERSONA_ID_KEY,
            new RestDropdownInput("/api/v1/personas", "key", "name").minSelections(1));


    public PersonaConditionlet() {
        this(APILocator.getPersonaAPI(), APILocator.getUserAPI(), APILocator.getVisitorAPI());
    }

    @VisibleForTesting
    PersonaConditionlet(PersonaAPI personaAPI, UserAPI userAPI, VisitorAPI visitorAPI) {
        super("api.ruleengine.system.conditionlet.Persona", new ComparisonParameterDefinition(2, IS, IS_NOT), persona);
        this.personaAPI = personaAPI;
        this.userAPI = userAPI;
        this.visitorAPI = visitorAPI;
    }
    
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        boolean result;
        try {
            Optional<Visitor> opt = visitorAPI.getVisitor(request);
            if(opt.isPresent()){
                Visitor visitor = opt.get();
                Persona currentPersona = (Persona) visitor.getPersona();
                User user = userAPI.getSystemUser();
                Persona inputPersona = personaAPI.find(instance.personaId, user, false);

                if(inputPersona == null){
                    Logger.warn(this, "Persona with id '" + instance.personaId + "' not be found. Could not evaluate condition.");
                    result = false;
                } else if(currentPersona == null) {
                    Logger.warn(this, "Persona is not set in Visitor object. Could not evaluate condition.");
                    result = false;
                } else {
                    result = instance.comparison.perform(currentPersona, inputPersona);
                }
            } else{
                Logger.warn(this, "No visitor was available. Could not evaluate condition.");
                result = false;
            }
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate condition %s", this.getClass().getName());
        }
        return result;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public class Instance implements RuleComponentInstance {

        private final String personaId;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 2,
                    "Persona Condition requires parameters %s and %s", COMPARISON_KEY, PERSONA_ID_KEY );
            assert parameters != null;

            personaId = parameters.get(PERSONA_ID_KEY).getValue();

            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                        comparisonValue,
                        definition.getId());
            }

            Preconditions.checkArgument(StringUtils.isNotBlank(personaId), "Persona Conditionlet requires valid persona ID.");

        }
    }


}
