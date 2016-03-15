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

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This conditionlet will allow CMS users to check the browser name a user
 * request is issued from. The information is obtained by reading the
 * {@code User-Agent} header in the {@link HttpServletRequest} object. 
 * This {@link Conditionlet} provides a drop-down menu
 * with the available comparison mechanisms, and a dropdown field to select
 * the browser to compare. The version of the browser is indifferent since the
 * validation is against the name of the browser.
 * <p>
 * The format of the {@code User-Agent} is not standardized (basically free
 * format), which makes it difficult to decipher it. This conditionlet uses a
 * Java API called <a
 * href="http://www.bitwalker.eu/software/user-agent-utils">User Agent Utils</a>
 * which parses HTTP requests in real time and gather information about the user
 * agent, detecting a high amount of browsers, browser types, operating systems,
 * device types, rendering engines, and Web applications.
 * </p>
 * 
 *
 * @author Erick Gonzalez
 * @version 2.0
 * @since 03-08-2016
 *
 */
public class UsersBrowserConditionlet extends Conditionlet<UsersBrowserConditionlet.Instance> {
	
    private static final long serialVersionUID = 1L;

    public static final String BROWSER_KEY = "browser";

    private static final ParameterDefinition<TextType> browser = new ParameterDefinition<>(
        3, BROWSER_KEY,
        new DropdownInput()
            .minSelections(1)
            .option(Browser.CHROME.getName())
            .option(Browser.FIREFOX.getName())
            .option(Browser.IE.getName())
            .option(Browser.OPERA.getName())
            .option(Browser.SAFARI.getName())
            .option(Browser.EDGE.getName())

    );

    @SuppressWarnings("unused")
    public UsersBrowserConditionlet() {
        super("api.ruleengine.system.conditionlet.VisitorsBrowser",
              new ComparisonParameterDefinition(2, IS, IS_NOT),
              browser);
    }
    
    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String browser = lookupBrowser(request, instance);
        return instance.comparison.perform(browser.toLowerCase(), instance.browser.toLowerCase());
    }
    
    private String lookupBrowser(HttpServletRequest request, Instance instance) {
        String browser = "unknown";
        try {
            String userAgentInfo = request.getHeader("User-Agent");
            UserAgent agent = UserAgent.parseUserAgentString(userAgentInfo);
            if (agent != null && agent.getBrowser() != null) {
                browser = agent.getBrowser().getName().replaceAll("[0-9]*$", "").trim();//remove version number of the browser name e.g Firefox4
                if(browser.toLowerCase().contains(instance.browser.toLowerCase())){// avoid issues with the device e.g Chrome_Mobile
                	browser = instance.browser;
                }
            }
        } catch (Exception e) {
            Logger.error(UsersBrowserConditionlet.class, "Could not obtain browser from request. Using 'unknown': " + request.getRequestURL());
        }
        return browser;
    }
    
    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String browser;
        public final Comparison<String> comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 2, "Request Header Condition requires parameters %s and %s.", COMPARISON_KEY, BROWSER_KEY);
            assert parameters != null;
            this.browser = parameters.get(BROWSER_KEY).getValue();
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