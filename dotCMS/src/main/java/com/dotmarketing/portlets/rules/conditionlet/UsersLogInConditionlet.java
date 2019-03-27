package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;

/**
 * This conditionlet will allow dotCMS users to check whether the user that
 * issued the request is currently logged in or not. The login status of a user
 * is in the {@link HttpServletRequest} object, which is used to perform the
 * validation and is retrieved using our own API. This {@link Conditionlet}
 * provides a single drop-down menu with the available comparison mechanisms,
 * and it will check the login status with the back-end system, no other user
 * input is to be required or validated.
 *
 * @author Erick Gonzalez
 * @version 2.0
 * @since 02-17-2016
 *
 */
public class UsersLogInConditionlet extends Conditionlet<UsersLogInConditionlet.Instance> {

	private static final long serialVersionUID = 1L;
	
    @SuppressWarnings("unused")
    public UsersLogInConditionlet() {
        super("api.ruleengine.system.conditionlet.VisitorLoginStatus",
              new ComparisonParameterDefinition(2, IS, IS_NOT));
    }
    
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
    	boolean comparison = instance.comparison.getId() == Comparison.IS.getId();
        try{
        	User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
        	if((comparison && user!= null)||(!comparison && user == null)){
        		return true;
        	}
        } catch(DotRuntimeException | PortalException | SystemException e){
        	Logger.error(this, "Could not retrieved logged-in user from request: " + request.getRequestURL());
        }
        return false;
    }

    
    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }
    
    public static class Instance implements RuleComponentInstance {

        public final Comparison<String> comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
        	checkState(parameters != null && parameters.size() == 1, "User Log In Condition requires parameter %s.", COMPARISON_KEY);
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                //noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }
}
