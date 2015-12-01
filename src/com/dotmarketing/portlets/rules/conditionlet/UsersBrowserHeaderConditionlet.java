package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.ValidationResult;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.portlets.rules.parameter.type.constraint.EnumerationConstraint;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull;
import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.conditionlet.Comparison.IS;

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
public class UsersBrowserHeaderConditionlet extends Conditionlet<UsersBrowserHeaderConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String HEADER_NAME_KEY = "browser-header";
    public static final String HEADER_VALUE_KEY = "header-value";

    private Map<String, ConditionletInput> inputValues = null;

    static {
        String[] KNOWN_HEADERS = {
            "Accept",
            "Accept-Charset",
            "Accept-Encoding",
            "Accept-Language",
            "Accept-Datetime",
            "Authorization",
            "Cache-Control",
            "Connection",
            "Cookie",
            "Content-Length",
            "Content-MD5",
            "Content-Type",
            "Date",
            "Expect",
            "From",
            "Host",
            "If-Match",
            "If-Modified-Since",
            "If-Modified-Since",
            "If-None-Match",
            "If-Range",
            "If-Unmodified-Since",
            "If-Unmodified-Since",
            "Max-Forwards",
            "Origin",
            "Pragma",
            "Proxy-Authorization",
            "Proxy-Authorization",
            "Range",
            "Referer",
            "TE",
            "User-Agent",
            "Upgrade",
            "Via",
            "Warning",
            "X-Requested-With",
            "DNT",
            "X-Forwarded-For",
            "X-Forwarded-Host",
            "Front-End-Https",
            "X-Http-Method-Override",
            "X-Http-Method-Override",
            "X-ATT-DeviceId",
            "X-Wap-Profile",
            "Proxy-Connection",
            "X-UIDH",
            "X-Csrf-Token"
        };
        ParameterDefinition headerKey = new ParameterDefinition(
            HEADER_NAME_KEY,
            new TextType.Builder()
                .i18nKey("text")
                .minLength(0)
                .maxLength(255)
                .defaultValue("Accept")
                .build(),
            new DropdownInput.Builder()
                .build()
        );
    }

    public UsersBrowserHeaderConditionlet() {
        super("api.ruleengine.system.conditionlet.RequestHeader", ImmutableSet.of(IS,
                                                                                  Comparison.IS_NOT,
                                                                                  Comparison.STARTS_WITH,
                                                                                  Comparison.ENDS_WITH,
                                                                                  Comparison.CONTAINS,
                                                                                  Comparison.REGEX));
    }

    protected ValidationResult validate(Comparison comparison, ConditionletInputValue inputValue) {
        ValidationResult validationResult = new ValidationResult();
        String inputId = inputValue.getConditionletInputId();
        if(UtilMethods.isSet(inputId)) {
            String selectedValue = inputValue.getValue();
            String comparisonId = comparison.getId();
            if(this.inputValues == null
               || this.inputValues.get(inputId) == null) {
                getInputs(comparisonId);
            }
            ConditionletInput inputField = this.inputValues.get(inputId);
            validationResult.setConditionletInputId(inputId);
            if(HEADER_NAME_KEY.equalsIgnoreCase(inputId)) {
                Set<EntryOption> inputOptions = inputField.getData();
                if(inputOptions != null) {
                    for (EntryOption option : inputOptions) {
                        if(option.getId().equalsIgnoreCase(selectedValue)) {
                            validationResult.setValid(true);
                            break;
                        }
                    }
                }
            } else {
                if(comparison == Comparison.IS
                   || comparison == Comparison.IS_NOT
                   || comparison == Comparison.STARTS_WITH
                   || comparison == Comparison.ENDS_WITH
                   || comparison == Comparison.CONTAINS) {
                    if(UtilMethods.isSet(selectedValue)) {
                        validationResult.setValid(true);
                    }
                } else if(comparison == Comparison.REGEX) {
                    try {
                        Pattern.compile(selectedValue);
                        validationResult.setValid(true);
                    } catch (PatternSyntaxException e) {
                        Logger.debug(this, "Invalid RegEx " + selectedValue);
                    }
                }
            }
            if(!validationResult.isValid()) {
                validationResult.setErrorMessage("Invalid value for input '"
                                                 + inputId + "': '" + selectedValue + "'");
            }
        }
        return validationResult;
    }

    @Override
    public Collection<ConditionletInput> getInputs(String comparisonId) {
        if(this.inputValues == null) {
            this.inputValues = new LinkedHashMap<>();
            // Set field #1 configuration and available options
            ConditionletInput inputField = new ConditionletInput();
            inputField.setId(HEADER_NAME_KEY);
            inputField.setMultipleSelectionAllowed(false);
            inputField.setDefaultValue("");
            inputField.setMinNum(1);
            Set<EntryOption> options = new LinkedHashSet<>();

            inputField.setData(options);
            this.inputValues.put(inputField.getId(), inputField);
            // Set field #2 configuration and available options
            ConditionletInput inputField2 = new ConditionletInput();
            inputField2.setId(HEADER_VALUE_KEY);
            inputField2.setMultipleSelectionAllowed(false);
            inputField2.setDefaultValue("");
            inputField2.setMinNum(1);
            this.inputValues.put(inputField2.getId(), inputField2);
        }
        return this.inputValues.values();
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        String headerActualValue = request.getHeader(instance.headerKey);
        boolean evalSuccess;
        if(instance.comparison == Comparison.EXISTS) {
            evalSuccess = Comparison.EXISTS.perform(headerActualValue);
        } else if(instance.comparison != Comparison.REGEX) {
            //noinspection unchecked
            evalSuccess = instance.comparison.perform(headerActualValue.toLowerCase(), instance.headerValue.toLowerCase());
        } else {
            evalSuccess = Comparison.REGEX.perform(headerActualValue, instance.headerValue);
        }
        return evalSuccess;
    }

    @Override
    public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
        return new Instance(comparison, values);
    }

    public static class Instance implements RuleComponentInstance {

        public final String headerKey;
        public final String headerValue;
        public final Comparison comparison;

        private Instance(Comparison comparison, List<ParameterModel> values) {
            comparison = checkNotNull(comparison, "Request header condition requires non-null comparison.");
            checkState(values != null && values.size() == 2, "Request header condition requires two values.");
            assert values != null;
            this.headerKey = values.get(0).getValue();
            this.headerValue = values.get(1).getValue();
            this.comparison = comparison;
        }
    }
}
