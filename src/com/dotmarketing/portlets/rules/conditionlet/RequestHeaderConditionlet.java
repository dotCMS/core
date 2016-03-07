package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EXISTS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

/**
 * This conditionlet will allow CMS users to check the value of any of the HTTP
 * headers that are part of the {@link HttpServletRequest} object. The
 * comparison of header names and values is case-insensitive, except for the
 * regular expression comparison. This {@link Conditionlet} provides a drop-down
 * menu with the available comparison mechanisms, a drop-down menu with some of
 * the most common HTTP Headers, and a text field to enter the value to compare.
 *
 * @author Jose Castro
 * @version 1.0
 * @since 05-13-2015
 */
public class RequestHeaderConditionlet extends Conditionlet<RequestHeaderConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String HEADER_NAME_KEY = "browser-header";
    public static final String HEADER_VALUE_KEY = "header-value";

    private static final ParameterDefinition<TextType> headerKey = new ParameterDefinition<>(
        1, HEADER_NAME_KEY,
        new DropdownInput(new TextType().maxLength(255))
            .allowAdditions()
            .minSelections(1)
            .option("Accept")
            .option("Accept-Charset")
            .option("Accept-Encoding")
            .option("Accept-Language")
            .option("Accept-Datetime")
            .option("Authorization")
            .option("Cache-Control")
            .option("Connection")
            .option("Cookie")
            .option("Content-Length")
            .option("Content-MD5")
            .option("Content-Type")
            .option("Date")
            .option("Expect")
            .option("From")
            .option("Host")
            .option("If-Match")
            .option("If-Modified-Since")
            .option("If-Modified-Since")
            .option("If-None-Match")
            .option("If-Range")
            .option("If-Unmodified-Since")
            .option("If-Unmodified-Since")
            .option("Max-Forwards")
            .option("Origin")
            .option("Pragma")
            .option("Proxy-Authorization")
            .option("Proxy-Authorization")
            .option("Range")
            .option("Referer")
            .option("TE")
            .option("User-Agent")
            .option("Upgrade")
            .option("Via")
            .option("Warning")
            .option("X-Requested-With")
            .option("DNT")
            .option("X-Forwarded-For")
            .option("X-Forwarded-Host")
            .option("Front-End-Https")
            .option("X-Http-Method-Override")
            .option("X-Http-Method-Override")
            .option("X-ATT-DeviceId")
            .option("X-Wap-Profile")
            .option("Proxy-Connection")
            .option("X-UIDH")
            .option("X-Csrf-Token"),
        "Accept"
    );

    private static final ParameterDefinition<TextType> headerValue = new ParameterDefinition<>(
        2, HEADER_VALUE_KEY,
        new TextInput<>(new TextType())
    );

    public RequestHeaderConditionlet() {
        super("api.system.ruleengine.conditionlet.RequestHeader",
              headerKey,
              new ComparisonParameterDefinition(2, IS, IS_NOT, EXISTS, STARTS_WITH, ENDS_WITH, CONTAINS, REGEX),
              headerValue);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String headerActualValue = request.getHeader(instance.headerKey);
        boolean evalSuccess;
        if(instance.comparison == EXISTS) {
            evalSuccess = EXISTS.perform(headerActualValue);
        }
        else {
            if(headerActualValue == null) {
                // treat null and empty string the same, except for 'Exists' case.
                headerActualValue = "";
            }
            if(instance.comparison != REGEX) {
                //noinspection unchecked
                evalSuccess = instance.comparison.perform(headerActualValue.toLowerCase(), instance.headerValue.toLowerCase());
            } else {
                evalSuccess = REGEX.perform(headerActualValue, instance.headerValue);
            }
        }
        return evalSuccess;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final String headerKey;
        public final String headerValue;
        public final Comparison comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 3,
                       "Request Header Condition requires parameters %s, %s and %s.", HEADER_NAME_KEY, HEADER_VALUE_KEY, COMPARISON_KEY);
            assert parameters != null;
            this.headerKey = parameters.get(HEADER_NAME_KEY).getValue();
            this.headerValue = parameters.get(HEADER_VALUE_KEY).getValue();
            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                this.comparison = ((ComparisonParameterDefinition)definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                                                          comparisonValue,
                                                          definition.getId());
            }
        }
    }
}
