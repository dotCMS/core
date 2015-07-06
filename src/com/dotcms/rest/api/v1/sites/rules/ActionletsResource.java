package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.config.AuthenticationProvider;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

@Path("/v1")
public class ActionletsResource {

    private final RulesAPI rulesAPI;
    private final AuthenticationProvider authProxy;

    public ActionletsResource() {
        this(new ApiProvider());
    }

    private ActionletsResource(ApiProvider apiProvider) {
        this(apiProvider, new AuthenticationProvider(apiProvider));
    }

    @VisibleForTesting
    protected ActionletsResource(ApiProvider apiProvider, AuthenticationProvider authProxy) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.authProxy = authProxy;
    }

    /**
     * <p>Returns a JSON with all the RuleActionlet Objects defined.
     * <p/>
     * Usage: /ruleactionlets/
     */
    @GET
    @Path("/ruleactionlets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context HttpServletRequest request) throws JSONException {
        User user = getUser(request);

        JSONObject jsonActionlets = new JSONObject();

        try {

            List<RuleActionlet> actionlets = rulesAPI.findActionlets();

            for (RuleActionlet actionlet : actionlets) {
                JSONObject actionletObject = new JSONObject();
                actionletObject.put("name", actionlet.getLocalizedName());
                jsonActionlets.put(actionlet.getClass().getSimpleName(), actionletObject);
            }

            return Response.ok(jsonActionlets.toString(), MediaType.APPLICATION_JSON).build();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Rule Actionlets", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @VisibleForTesting
    User getUser(@Context HttpServletRequest request) {
        return authProxy.authenticate(request);
    }
}
