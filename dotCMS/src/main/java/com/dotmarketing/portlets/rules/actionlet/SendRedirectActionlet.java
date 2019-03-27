package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.exception.InvalidActionInstanceException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;

public class SendRedirectActionlet extends RuleActionlet<SendRedirectActionlet.Instance> {

    private static final String INPUT_URL_KEY = "URL";


    public SendRedirectActionlet() {
        super("api.system.ruleengine.actionlet.send_redirect",
              new ParameterDefinition<>(1, INPUT_URL_KEY, new TextInput<>(new TextType().required())));
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        boolean success = false;
        try {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", instance.redirectToUrl);
            response.flushBuffer();
            success = true;
        } catch (IOException e) {
            Logger.error(SendRedirectActionlet.class, "Error executing Redirect Actionlet.", e);
        }
        return success;
    }

    public class Instance implements RuleComponentInstance {

        private final String redirectToUrl;

        public Instance(Map<String, ParameterModel> parameters) {
            checkState(parameters != null && parameters.size() == 1,
                       "Send Redirect Condition Type requires parameter '%s'.", INPUT_URL_KEY);
            assert parameters != null;
            this.redirectToUrl = parameters.get(INPUT_URL_KEY).getValue();

            try {
                //noinspection unused
                URI uri = URI.create(this.redirectToUrl);
            } catch (IllegalStateException | NullPointerException e) {
                /* It isn't necessary to wrap and re-throw exceptions, but doing so can help provide more readable stack traces. */
                throw new InvalidActionInstanceException(e, "SendRedirectActionlet: '%s' is not a valid URI", redirectToUrl);
            }
            /* A blank URI is a legitimate URI, but as far as redirects go it tends to generate infinite loops. */
            /* While '.' might be valid, we probably shouldn't accept it as a redirect target either. */
            Preconditions.checkArgument(!".".equals(this.redirectToUrl), "URL parameter cannot refer to self for SendRedirectActionlet.");
        }
    }
}
