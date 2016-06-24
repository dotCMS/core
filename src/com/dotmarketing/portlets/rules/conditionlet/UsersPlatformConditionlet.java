package com.dotmarketing.portlets.rules.conditionlet;

import eu.bitwalker.useragentutils.DeviceType;
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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;

/**
 * This conditionlet will allow CMS users to check the platform a user request
 * comes from, such as, mobile, tablet, desktop, etc. The information is
 * obtained by reading the {@code User-Agent} header in the
 * {@link HttpServletRequest} object. This {@link Conditionlet} provides a
 * drop-down menu with the available comparison mechanisms, and a drop-down menu
 * containing the different platforms that can be detected, where users can
 * select one or more values that will match the selected criterion.
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
 * @author Jose Castro
 * @version 1.0
 * @since 05-05-2015
 */
public class UsersPlatformConditionlet extends Conditionlet<UsersPlatformConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String PLATFORM_KEY = "platform";

    private static final ParameterDefinition<TextType> platform = new ParameterDefinition<>(
        3, PLATFORM_KEY,
        new DropdownInput()
            .minSelections(1)
            .option(DeviceType.COMPUTER.name())
            .option(DeviceType.MOBILE.name())
            .option(DeviceType.TABLET.name())
            .option(DeviceType.WEARABLE.name())
            .option(DeviceType.DMR.name())
            .option(DeviceType.GAME_CONSOLE.name())
    );

    @SuppressWarnings("unused")
    public UsersPlatformConditionlet() {
        super("api.ruleengine.system.conditionlet.VisitorsPlatform",
              new ComparisonParameterDefinition(2, IS, IS_NOT),
              platform);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String platform = lookupPlatform(request);
        return instance.comparison.perform(platform.toLowerCase(), instance.platform.name().toLowerCase());
    }

    private String lookupPlatform(HttpServletRequest request) {
        String platform = "unknown";
        try {
            String userAgentInfo = request.getHeader("User-Agent");
            UserAgent agent = UserAgent.parseUserAgentString(userAgentInfo);
            if (agent.getOperatingSystem() != null) {
                platform = agent.getOperatingSystem().getDeviceType().name();
            }
        } catch (Exception e) {
            Logger.error(UsersPlatformConditionlet.class, "Could not obtain platform from request. Using 'unknown': " + request.getRequestURL());
        }
        return platform;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final DeviceType platform;
        public final Comparison<String> comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 2, "Request Header Condition requires parameters %s and %s.", COMPARISON_KEY, PLATFORM_KEY);
            assert parameters != null;
            String platform = parameters.get(PLATFORM_KEY).getValue();
            this.platform = DeviceType.valueOf(platform);
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
