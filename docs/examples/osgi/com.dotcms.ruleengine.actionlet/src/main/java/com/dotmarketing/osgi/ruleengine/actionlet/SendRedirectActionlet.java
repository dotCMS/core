package com.dotmarketing.osgi.ruleengine.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.portlets.rules.actionlet.ActionParameterDefinition;
import com.dotmarketing.portlets.rules.actionlet.InvalidActionInstanceException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SendRedirectActionlet extends RuleActionlet {

    private static final String I18N_BASE_KEY = "com.dotmarketing.osgi.ruleengine.actionlet.send_redirect";
    private static final String INPUT_URL_KEY = "URL";

    private static final List<ActionParameterDefinition> PARAMS = ImmutableList.of(new ActionParameterDefinition(INPUT_URL_KEY));

    /**
     * Don't forget that Actionlets are effectively singletons! Any required state should be initialized before exiting the constructor,
     * and assume long term caching with arbitrary refresh intervals.
     */
    public SendRedirectActionlet() {
        super(I18N_BASE_KEY, PARAMS);
    }

    @Override
    public void validateActionInstance(RuleAction actionInstance) {
        Map<String, RuleActionParameter> params = actionInstance.getParameterMap();
        RuleActionParameter urlParam = Preconditions.checkNotNull(params.get(INPUT_URL_KEY),
                                                                  "SendRedirectActionlet requires a 'URL' parameter to be provided.");
        try {
            URI uri = URI.create(urlParam.getValue());
        } catch (IllegalStateException | NullPointerException e) {
            /* It isn't necessary to wrap and re-throw exceptions, but doing so can help provide more readable stack traces. */
            throw new InvalidActionInstanceException(e, "SendRedirectActionlet: '%1$s' is not a valid URI", urlParam.getValue());
        }
        /* @todo ggranum: Blank shouldn't be allowed, but until we send out the validation info with the definitions there's no way to even know that on
        * the client side.  */
        /* A blank URI is a legitimate URI, but as far as redirects go it tends to generate infinite loops. */
//        Preconditions.checkArgument(StringUtils.isNotBlank(urlParam.getValue()), "URL parameter cannot be blank for SendRedirectActionlet.");
        /* While '.' might be valid, we probably shouldn't accept it as a redirect target either. */
        Preconditions.checkArgument(!".".equals(urlParam.getValue()), "URL parameter cannot refer to self for SendRedirectActionlet.");
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {
        try {
        	
   
            
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", params.get(INPUT_URL_KEY).getValue());
            
            response.flushBuffer();
            
        } catch (IOException e) {
            Logger.error(SendRedirectActionlet.class, "Error executing Redirect Actionlet.", e);
        }
    }
}
