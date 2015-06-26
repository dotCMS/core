package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.config.AuthenticationProvider;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

@Path("/v1")
public class RulesResource {

    private final RulesAPI rulesAPI;
    private final AuthenticationProvider authProxy;
    private final RuleTransform ruleTransform = new RuleTransform();
    private HostAPI hostAPI;

    @SuppressWarnings("unused")
    public RulesResource() {
        this(new ApiProvider());
    }

    private RulesResource(ApiProvider apiProvider) {
        this(apiProvider, new AuthenticationProvider(apiProvider));
    }

    @VisibleForTesting
    protected RulesResource(ApiProvider apiProvider, AuthenticationProvider authProxy) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.authProxy = authProxy;
    }

    /**
     * <p>Returns a JSON representation of the rules defined in the given Host or Folder
     * <p/>
     * Usage: /rules/{hostOrFolderIdentifier}
     */
    @GET
    @JSONP
    @Path("/sites/{id}/rules")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response list(@Context HttpServletRequest request, @PathParam("id") String siteId) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user);
        List<RestRule> restRules = getRulesInternal(user, host);
        Map<String, RestRule> hash = Maps.newHashMapWithExpectedSize(restRules.size());
        for (RestRule restRule : restRules) {
            hash.put(restRule.key, restRule);
        }

        return Response.ok(hash).build();
    }

    /**
     * <p>Returns a JSON representation of the Rule with the given ruleId
     * <p/>
     * Usage: GET api/rules-engine/sites/{siteId}/rules/{ruleId}
     */
    @GET
    @JSONP
    @Path("/sites/{siteId}/rules/{ruleId}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response self(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId) {
        checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request);
        return Response.ok(getRuleInternal(ruleId, user)).build();
    }

    /**
     * <p>Saves a new Rule
     * <br>
     * <p/>
     * Usage: /rules/
     */
    @POST
    @JSONP
    @Path("/sites/{id}/rules")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request, @PathParam("id") String siteId, RestRule restRule) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user);
        String ruleId = createRuleInternal(host.getIdentifier(), restRule, user);

        try {
            @SuppressWarnings("unused")
            URI path = new URI(ruleId);
            return Response.ok("{ \"id\": \"" + ruleId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Rule id '%s'", ruleId);
        }
    }

    /**
     * <p>Updates a new Rule
     * <br>
     * <p/>
     * Usage: /rules/
     */
    @PUT
    @JSONP
    @Path("/sites/{siteId}/rules/{ruleId}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    public RestRule update(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId, RestRule restRule) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request);
        getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?

        updateRuleInternal(user, new RestRule.Builder().from(restRule).key(ruleId).build());

        return restRule;
    }

    /**
     * <p>Deletes a Rule
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/rules/{ruleId}
     */
    @DELETE
    @Path("/sites/{siteId}/rules/{ruleId}")
    public Response remove(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId) {
        User user = getUser(request);

        try {
            getHost(siteId, user);
            Rule rule = getRule(ruleId, user);
            rulesAPI.deleteRule(rule, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    private Host getHost(String siteId, User user) {
        try {
            Host host = hostAPI.find(siteId, user, false);
            if(host == null) {
                throw new NotFoundException("Site not found: '%s'", siteId);
            }
            return host;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private Rule getRule(String ruleId, User user) {
        try {
            Rule rule = rulesAPI.getRuleById(ruleId, user, false);
            if(rule == null) {
                throw new NotFoundException("Rule not found: '%s'", ruleId);
            }
            return rule;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private User getUser(@Context HttpServletRequest request) {
        return authProxy.authenticate(request);
    }

    private List<RestRule> getRulesInternal(User user, Host host) {
        try {
            List<Rule> rules = rulesAPI.getRulesByHost(host.getIdentifier(), user, false);
//            return Lists.newArrayList(Lists.transform(rules, ruleTransform.appToRestFn()));
            return rules.stream().map(ruleTransform.appToRestFn()).collect(Collectors.toList());
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private RestRule getRuleInternal(String ruleId, User user) {
        Rule input = getRule(ruleId, user);
        return ruleTransform.appToRest(input);
    }

    private String createRuleInternal(String siteId, RestRule restRule, User user) {
        try {
            Rule rule = ruleTransform.restToApp(restRule);
            rule.setHost(siteId);
            rulesAPI.saveRule(rule, user, false);
            return rule.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String updateRuleInternal(User user, RestRule restRule) {
        try {
            Rule rule = rulesAPI.getRuleById(restRule.key, user, false);
            if(rule == null) {
                throw new NotFoundException("Rule with key '%s' not found: ", restRule.key);
            }
            ruleTransform.applyRestToApp(restRule, rule);
            rulesAPI.saveRule(rule, user, false);
            return rule.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
