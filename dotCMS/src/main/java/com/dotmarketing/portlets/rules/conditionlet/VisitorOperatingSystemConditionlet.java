package com.dotmarketing.portlets.rules.conditionlet;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.viewtools.UserAgentTool;
import eu.bitwalker.useragentutils.OperatingSystem;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;

/**
 * This conditionlet will allow CMS users to check the SO name a user
 * request is issued from. The information is obtained by {@link com.dotmarketing.viewtools.UserAgentTool},
 * the list of possible values is in {@link eu.bitwalker.useragentutils.OperatingSystem}.
 * 
 *
 * @author Freddy Rodriguez
 *
 */
public class VisitorOperatingSystemConditionlet extends Conditionlet<VisitorOperatingSystemConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String OS_NAME__KEY = "os_name";

    public VisitorOperatingSystemConditionlet() {
        super("api.ruleengine.system.conditionlet.VisitorOperatingSystem",
              new ComparisonParameterDefinition(2, IS, IS_NOT),
              getOS());
    }

    private static ParameterDefinition getOS() {
        DropdownInput dropdownInput = new DropdownInput().minSelections(1);

        OperatingSystem[] operatingSystems = OperatingSystem.values();

        for (OperatingSystem operatingSystem : operatingSystems) {
            dropdownInput.option( operatingSystem.getName() );
        }

        return new ParameterDefinition<>(3, OS_NAME__KEY, dropdownInput);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        UserAgentTool userAgentTool = new UserAgentTool();
        userAgentTool.init( request );
        OperatingSystem os = userAgentTool.getOs();

        return instance.comparison.perform(os.getName().toLowerCase(), instance.os.toLowerCase());
    }

    
    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String os;
        public final Comparison<String> comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 2, "Request Header Condition requires parameters %s and %s.", COMPARISON_KEY, OS_NAME__KEY);
            assert parameters != null;
            this.os = parameters.get(OS_NAME__KEY).getValue();
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