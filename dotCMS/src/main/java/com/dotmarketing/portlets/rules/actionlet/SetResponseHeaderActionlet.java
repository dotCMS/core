package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.google.common.base.Preconditions;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

/**
 * Actionlet to add Key/Value to the Response header.
 * The exact names that had to be set in params are: headerKey and headerValue.
 *
 * @author Geoff M. Granum
 * @version 1.0
 * @since 09-22-2015
 */
public class SetResponseHeaderActionlet extends RuleActionlet<SetResponseHeaderActionlet.Instance> {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetResponseHeader";

    public static final String HEADER_KEY = "headerKey";
    public static final String HEADER_VALUE = "headerValue";

    /**
     * Cookie-setting response headers, which only dotCMS sets. Header names are matched ignoring
     * letter case and surrounding whitespace.
     */
    private static final List<String> RESERVED_HEADERS = List.of("Set-Cookie", "Set-Cookie2");

    /**
     * Reported, instead of the stored text, for a header name that is not a valid HTTP field name.
     */
    private static final String INVALID_HEADER_NAME = "a header name that is not a valid HTTP token";

    /**
     * A valid HTTP field name: one or more token characters (RFC 9110, section 5.6.2).
     */
    private static final Pattern HTTP_TOKEN = Pattern.compile("[!#$%&'*+\\-.^_`|~0-9A-Za-z]+");

    public SetResponseHeaderActionlet() {
        super(I18N_BASE,
              new ParameterDefinition<>(1, HEADER_KEY, new TextInput<>(new TextType().required())),
              new ParameterDefinition<>(2, HEADER_VALUE, new TextInput<>(new TextType())));
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        response.setHeader(instance.key, instance.value);
        return true;
    }

    /**
     * Refuses the cookie-setting headers {@code Set-Cookie} and {@code Set-Cookie2}, in any letter
     * case and with surrounding whitespace ignored, and any header name that is not a valid HTTP
     * token. Every other header is accepted.
     *
     * @param parameters The action parameters, keyed by parameter name.
     * @return The canonical name of the reserved header, a fixed description of an invalid name, or
     * empty when the header is accepted.
     */
    @Override
    public Optional<String> getRefusedName(final Map<String, ParameterModel> parameters) {
        return refusedName(parameters);
    }

    /**
     * Returns the canonical name of the reserved header that the given parameters name, or a fixed
     * description when the name is not a valid HTTP token, never the stored text; empty when the
     * header is accepted or has no name.
     */
    private static Optional<String> refusedName(final Map<String, ParameterModel> parameters) {
        final ParameterModel keyParameter = parameters == null ? null : parameters.get(HEADER_KEY);
        final String name = keyParameter == null ? null : keyParameter.getValue();
        if (name == null) {
            return Optional.empty();
        }
        final String trimmedName = name.trim();
        final Optional<String> reserved = RESERVED_HEADERS.stream().filter(trimmedName::equalsIgnoreCase).findFirst();
        if (reserved.isPresent()) {
            return reserved;
        }
        return trimmedName.isEmpty() || HTTP_TOKEN.matcher(trimmedName).matches()
                ? Optional.empty() : Optional.of(INVALID_HEADER_NAME);
    }

    static class Instance implements RuleComponentInstance {

        private final String key;
        private final String value;

        /**
         * Builds the action from its parameters, refusing a cookie-setting header or a header name
         * that is not a valid HTTP token with a client error. The header name is used without
         * surrounding whitespace.
         */
        public Instance(Map<String, ParameterModel> parameters) {
            if (refusedName(parameters).isPresent()) {
                throw new InvalidRuleParameterException("The header name is not allowed");
            }
            key = StringUtils.trimToEmpty(parameters.get(HEADER_KEY).getValue());
            String v = parameters.get(HEADER_VALUE).getValue();
            value = v != null ? v : "";
            Preconditions.checkArgument(StringUtils.isNotBlank(key), "SetResponseHeaderActionlet requires valid key.");
        }
    }
}
