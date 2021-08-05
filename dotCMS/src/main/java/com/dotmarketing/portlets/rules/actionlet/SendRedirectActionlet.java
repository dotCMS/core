package com.dotmarketing.portlets.rules.actionlet;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.vanityurl.filters.VanityUrlRequestWrapper;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.conditionlet.VisitorsCurrentUrlConditionlet;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import io.vavr.control.Try;

public class SendRedirectActionlet extends RuleActionlet<SendRedirectActionlet.Instance> {

    private static final long serialVersionUID = 1L;
    private static final String INPUT_URL_KEY = "URL";
    private static final String INPUT_REDIRECT_METHOD = "REDIRECT_METHOD";

    enum REDIRECT_METHOD{
        FORWARD(200),
        MOVED_TEMP(302),
        MOVED_PERM(301);
        
        private final int responseCode;
        
        REDIRECT_METHOD(int responseCode){
            this.responseCode= responseCode;
        }

        static int getResponse(final String method) {
            return FORWARD.name().equalsIgnoreCase(method) 
                ? FORWARD.responseCode 
                : MOVED_TEMP.name().equalsIgnoreCase(method) 
                    ? MOVED_TEMP.responseCode 
                    : MOVED_PERM.responseCode;
        }
        
        
    }
    
    public SendRedirectActionlet() {
        super("api.system.ruleengine.actionlet.send_redirect",
                new ParameterDefinition<>(0, INPUT_REDIRECT_METHOD,
                                        new DropdownInput()
                                        .minSelections(1)
                                        .maxSelections(1)
                                        .option(REDIRECT_METHOD.FORWARD.name())
                                        .option(REDIRECT_METHOD.MOVED_TEMP.name())
                                        .option(REDIRECT_METHOD.MOVED_PERM.name())),
                new ParameterDefinition<>(1, INPUT_URL_KEY, new TextInput<>(new TextType().required()))

                        );
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        final String myURL = CMSUrlUtil.getInstance().getURIFromRequest(request);
        final Optional<Pattern> patternOpt = Optional.ofNullable(
                        (Pattern) request.getAttribute(VisitorsCurrentUrlConditionlet.CURRENT_URL_CONDITIONLET_MATCHER));



        final String rewriteUrl = instance.rewriteUrl(myURL, patternOpt);
        // nothing to do
        if (myURL.equals(rewriteUrl)) {
            return true;
        }
        response.setHeader("X-DOT-SendRedirectRuleAction", "true" );
        
        VanityUrlResult result = new VanityUrlResult(rewriteUrl, request.getQueryString(), instance.responseCode);
        VanityUrlRequestWrapper wrapper = new VanityUrlRequestWrapper(request, result);
        if (APILocator.getVanityUrlAPI().handleVanityURLRedirects(wrapper, response, result)) {
            Try.run(() -> response.flushBuffer());
        }
        return true;
    }

    public class Instance implements RuleComponentInstance {
        private final int responseCode;
        private final String redirectToUrl;

        public Instance(Map<String, ParameterModel> parameters) {
            assert parameters != null;
            this.redirectToUrl = parameters.getOrDefault(INPUT_URL_KEY, new ParameterModel()).getValue();
            this.responseCode = Try.of(() -> REDIRECT_METHOD.getResponse(parameters.get(INPUT_REDIRECT_METHOD).getValue()))
                            .getOrElse(301);


        }


        String rewriteUrl(String urlIn, Optional<Pattern> patternOpt) {

            if (!patternOpt.isPresent()) {
                return redirectToUrl;
            }

            final Pattern pattern = patternOpt.get();
            final Matcher matcher = pattern.matcher(urlIn);
            if (!matcher.matches()) {
                return urlIn;
            }
            String newForward = this.redirectToUrl;
            for (int i = 1; i <= matcher.groupCount(); i++) {
                newForward = newForward.replace("$" + i, matcher.group(i));
            }


            return newForward;

        }
    }
    
}
