package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotcms.enterprise.rules.RulesAPI;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

@Path("/v1/system/ruleengine")
public class ActionletsResource {

    private final RulesAPI rulesAPI;
    private final WebResource webResource;
    private final ActionletTransform transform = new ActionletTransform();

    public ActionletsResource() {
        this(new ApiProvider());
    }

    private ActionletsResource(ApiProvider apiProvider) {
        this(apiProvider, new WebResource(apiProvider));
    }

    @VisibleForTesting
    protected ActionletsResource(ApiProvider apiProvider, WebResource webResource) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.webResource = webResource;
    }

    /**
     * <p>Returns a JSON with all the RuleActionlet Objects defined.
     * <p>
     * Usage: /ruleactionlets/
     */
    @GET
    @Path("/actionlets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context HttpServletRequest request) {
        User user = getUser(request);
        return Response.ok(getActionletsInternal()).build();
    }

    public Map<String, RestActionlet> getActionletsInternal() {
        try {
            List<RuleActionlet> actionlets = rulesAPI.findActionlets();
            return actionlets.stream().map(transform.appToRestFn()).collect(Collectors.toMap(restAction -> restAction.id, Function.identity()));
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException | InvalidLicenseException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    @VisibleForTesting
    User getUser(@Context HttpServletRequest request) {
        return webResource.init(true, request, true).getUser();
    }
}
