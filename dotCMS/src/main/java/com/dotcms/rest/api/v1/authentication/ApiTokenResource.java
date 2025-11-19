package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.vavr.control.Try;
import java.util.Base64;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_MAP;

/**
 * Endpoint to handle Api Tokens
 */
@Path("/v1/apitoken")
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "API Token")

public class ApiTokenResource implements Serializable {

    private final ApiTokenAPI tokenApi;
    private final WebResource webResource;

    /**
     * Default constructor.
     */
    public ApiTokenResource() {
        this(APILocator.getApiTokenAPI(), new WebResource());
    }

    @VisibleForTesting
    protected ApiTokenResource(final ApiTokenAPI tokenApi, final WebResource webResource) {

        this.tokenApi    = tokenApi;
        this.webResource = webResource;
    }

    @GET
    @Path("/{userId}/tokens")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getApiTokensByUserIdV1",
            summary = "Retrieves API tokens based on a user ID",
            description = "Accepts a user identifier and returns a list of API tokens associated with that user.\n\n" +
                    "The returned list may optionally include or exclude tokens that have been revoked.\n\n",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User's API tokens successfully retrieved",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityMapView.class),
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "  \"entity\": {\n" +
                                                            "    \"tokens\": [\n" +
                                                            "      {\n" +
                                                            "        \"allowNetwork\": null,\n" +
                                                            "        \"claims\": {\n" +
                                                            "          \"label\": \"string\"\n" +
                                                            "        },\n" +
                                                            "        \"expired\": false,\n" +
                                                            "        \"expiresDate\": 1822623941000,\n" +
                                                            "        \"id\": \"string\",\n" +
                                                            "        \"issueDate\": 1728061510000,\n" +
                                                            "        \"issuer\": \"string\",\n" +
                                                            "        \"modificationDate\": 1728061510000,\n" +
                                                            "        \"notBeforeDate\": false,\n" +
                                                            "        \"requestingIp\": \"string\",\n" +
                                                            "        \"requestingUserId\": \"string\",\n" +
                                                            "        \"revoked\": false,\n" +
                                                            "        \"revokedDate\": null,\n" +
                                                            "        \"subject\": \"string\",\n" +
                                                            "        \"tokenType\": \"string\",\n" +
                                                            "        \"userId\": \"string\",\n" +
                                                            "        \"valid\": true\n" +
                                                            "      }\n" +
                                                            "    ]\n" +
                                                            "  },\n" +
                                                            "  \"errors\": [],\n" +
                                                            "  \"i18nMessagesMap\": {},\n" +
                                                            "  \"messages\": [],\n" +
                                                            "  \"pagination\": null,\n" +
                                                            "  \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Invalid user"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Invalid user"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")

            }
    )
    public final Response getApiTokens(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("userId") @Parameter(
                    required = true,
                    description = "Identifier of user to check for tokens.",
                    schema = @Schema(type = "string")
            ) final String userId,
            @QueryParam("showRevoked") @Parameter(
                    description = "Determines whether revoked tokens are shown. Defaults to `false` if omitted.",
                    schema = @Schema(type = "boolean")
            ) final boolean showRevoked) {


        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final List<ApiToken> tokens = tokenApi.findApiTokensByUserId(userId, showRevoked, initDataObject.getUser());

        return Response.ok(new ResponseEntityMapView(Map.of("tokens", tokens), EMPTY_MAP))
                .build(); // 200
    }

    @PUT
    @Path("/{tokenId}/revoke")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "putRevokeTokenByIdV1",
            summary = "Revokes an API token",
            description = "Revokes a token by its identifier.\n\n Returned entity contains the " +
                    "property `revoked`, whose value is an object representing the revoked token.",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token revoked successfully",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityMapView.class),
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "  \"entity\": {\n" +
                                                            "    \"revoked\": {\n" +
                                                            "      \"allowNetwork\": null,\n" +
                                                            "      \"claims\": {\n" +
                                                            "        \"label\": \"string\"\n" +
                                                            "      },\n" +
                                                            "      \"expired\": false,\n" +
                                                            "      \"expiresDate\": 1822623941000,\n" +
                                                            "      \"id\": \"string\",\n" +
                                                            "      \"issueDate\": 1728061510000,\n" +
                                                            "      \"issuer\": \"string\",\n" +
                                                            "      \"modificationDate\": 1728069870000,\n" +
                                                            "      \"notBeforeDate\": false,\n" +
                                                            "      \"requestingIp\": \"string\",\n" +
                                                            "      \"requestingUserId\": \"string\",\n" +
                                                            "      \"revoked\": true,\n" +
                                                            "      \"revokedDate\": 1728069870000,\n" +
                                                            "      \"subject\": \"string\",\n" +
                                                            "      \"tokenType\": \"string\",\n" +
                                                            "      \"userId\": \"string\",\n" +
                                                            "      \"valid\": false\n" +
                                                            "    }\n" +
                                                            "  },\n" +
                                                            "  \"errors\": [],\n" +
                                                            "  \"i18nMessagesMap\": {},\n" +
                                                            "  \"messages\": [],\n" +
                                                            "  \"pagination\": null,\n" +
                                                            "  \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Invalid user"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Token not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response revokeApiToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("tokenId") @Parameter(
                    required = true,
                    description = "Identifier of API token to be revoked",
                    schema = @Schema(type = "string")) final String tokenId) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user                     = initDataObject.getUser();
        final Optional<ApiToken> optToken   = this.tokenApi.findApiToken(tokenId);

        if (optToken.isPresent()) {

            ApiToken token = optToken.get();

            if(token.isExpired()) {

                return ExceptionMapperUtil.createResponse(new DotStateException("Token Expired"), Response.Status.NOT_ACCEPTABLE);
            }

            SecurityLogger.logInfo(this.getClass(), "Revoking token " + token + " from " + request.getRemoteAddr() + " ");
            this.tokenApi.revokeToken(token, user);
            token = this.tokenApi.findApiToken(tokenId).get();
            return Response.ok(new ResponseEntityMapView(Map.of("revoked", token))).build(); // 200
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    @DELETE
    @Path("/{tokenId}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "deleteApiTokenByIdV1",
            summary = "Deletes an API token",
            description = "Deletes an API token by identifier. May be performed on either active, expired, or revoked.\n\n" +
                    "Returned entity contains the property `deleted`, the value of which is the deleted token object.",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token successfully deleted",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityMapView.class),
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "  \"entity\": {\n" +
                                                            "    \"deleted\": {\n" +
                                                            "      \"allowNetwork\": null,\n" +
                                                            "      \"claims\": {\n" +
                                                            "        \"label\": \"string\"\n" +
                                                            "      },\n" +
                                                            "      \"expired\": false,\n" +
                                                            "      \"expiresDate\": 1822623941000,\n" +
                                                            "      \"id\": \"string\",\n" +
                                                            "      \"issueDate\": 1728061510000,\n" +
                                                            "      \"issuer\": \"string\",\n" +
                                                            "      \"modificationDate\": 1728069870000,\n" +
                                                            "      \"notBeforeDate\": false,\n" +
                                                            "      \"requestingIp\": \"string\",\n" +
                                                            "      \"requestingUserId\": \"string\",\n" +
                                                            "      \"revoked\": true,\n" +
                                                            "      \"revokedDate\": 1728069870000,\n" +
                                                            "      \"subject\": \"string\",\n" +
                                                            "      \"tokenType\": \"string\",\n" +
                                                            "      \"userId\": \"string\",\n" +
                                                            "      \"valid\": false\n" +
                                                            "    }\n" +
                                                            "  },\n" +
                                                            "  \"errors\": [],\n" +
                                                            "  \"i18nMessagesMap\": {},\n" +
                                                            "  \"messages\": [],\n" +
                                                            "  \"pagination\": null,\n" +
                                                            "  \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Invalid user"),
                    @ApiResponse(responseCode = "404", description = "Token not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response deleteApiToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("tokenId") @Parameter(
                    required = true,
                    description = "Identifier of API token to be deleted.",
                    schema = @Schema(type = "string"))
            final String tokenId) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user                     = initDataObject.getUser();
        final Optional<ApiToken> optToken   = tokenApi.findApiToken(tokenId);

        if (optToken.isPresent()) {

            final ApiToken token = optToken.get();
            SecurityLogger.logInfo(this.getClass(), "Deleting token " + token + " from " + request.getRemoteAddr() + " ");

            if(tokenApi.deleteToken(token, user)) {

                return Response.ok(new ResponseEntityMapView(Map.of("deleted", token)))
                        .build(); // 200
            }

            return ExceptionMapperUtil.createResponse(new DotStateException("No permissions to token"), Response.Status.FORBIDDEN);
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    /**
     * Issue a new APIToken.
     * If the shouldBeAdmin is true then response with a 403 if the user for whom the token is not admin
     * If the userId parameter is null then the token is generated to the requesting user
     *
     * @param request
     * @param response
     * @param formData - json data, expecting {netmask:'192.168.1.0/24', expirationDays:1000, userId:'dotcms.org.1', shouldBeAdmin: 'true'}
     * @return Response
     */
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "postIssueApiTokenV1",
            summary = "Issues an API token",
            description = "Issues an API token to an authorized user account.\n\n" +
                    "Returns an object representing the issued token.",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token successfully issued to user",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityApiTokenWithJwtView.class),
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "  \"entity\": {\n" +
                                                            "    \"jwt\": \"string\",\n" +
                                                            "    \"token\": {\n" +
                                                            "      \"allowNetwork\": \"0.0.0.0/0\",\n" +
                                                            "      \"claims\": {\n" +
                                                            "        \"label\": \"string\"\n" +
                                                            "      },\n" +
                                                            "      \"expired\": false,\n" +
                                                            "      \"expiresDate\": 0 \n," +
                                                            "      \"id\": \"string\",\n" +
                                                            "      \"issueDate\": 0,\n" +
                                                            "      \"issuer\": \"string\",\n" +
                                                            "      \"modificationDate\": 0,\n" +
                                                            "      \"notBeforeDate\": false,\n" +
                                                            "      \"requestingIp\": \"string\",\n" +
                                                            "      \"requestingUserId\": \"string\",\n" +
                                                            "      \"revoked\": false,\n" +
                                                            "      \"revokedDate\": 0,\n" +
                                                            "      \"subject\": \"string\",\n" +
                                                            "      \"tokenType\": \"string\",\n" +
                                                            "      \"userId\": \"string\",\n" +
                                                            "      \"valid\": true\n" +
                                                            "    }\n" +
                                                            "  },\n" +
                                                            "  \"errors\": [],\n" +
                                                            "  \"i18nMessagesMap\": {},\n" +
                                                            "  \"messages\": [],\n" +
                                                            "  \"pagination\": null," +
                                                            "  \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Invalid user"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response issueApiToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "This method requires a POST body of a JSON object containing the following properties.\n\n" +
                    "| Property        | Value     | Description                                   |\n" +
                    "|-----------------|-----------|-----------------------------------------------|\n" +
                    "| `userId`             | String    | **Required.** ID of user attempting receiving |\n" +
                    "| `expirationSeconds`  | Integer    | **Required.** TTL of token in seconds. |\n" +
                    "| `network`            | String    | Network mask in which token is valid. Defaults to `0.0.0.0/0`, " +
                                                            "or any local network.  |\n" +
                    "| `claims`             | Object    | Contains `label` property. |\n" +
                    "| `claims.label`       | String    | Sets a user-defined name for token. |\n" +
                    "| `shouldBeAdmin`      | Boolean   | If `true`, the call only succeeds if the token is being issued " +
                                                            "to an admin account. Defaults to `false` if omitted. |\n",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ApiTokenForm.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                    "  \"userId\": \"string\",\n" +
                                    "  \"expirationSeconds\": 0,\n" +
                                    "  \"network\": \"string\",\n" +
                                    "  \"claims\": {\n" +
                                    "    \"label\": \"string\"\n" +
                                    "  },\n" +
                                    "  \"shouldBeAdmin\": false\n" +
                                    "}")
                    )
            )
            final ApiTokenForm formData) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        final User requestingUser = initDataObject.getUser();
        final User forUser = formData.userId != null ? getUserById(formData, requestingUser) : requestingUser;

        if(forUser == null) {
            return ExceptionMapperUtil.createResponse(new DotStateException("No user found"), Response.Status.NOT_FOUND);
        }

        if (requestingUser != forUser && !requestingUser.isAdmin()) {
            throw new DotDataException("Only Admin user can request a token for another user");
        }

        if (!forUser.isAdmin() && formData.shouldBeAdmin) {
            throw new DotSecurityException("User should be Admin");
        }

        final String netmaskStr = formData.network!=null && !"0.0.0.0/0".equals(formData.network)? formData.network:null;
        final String netmask    = Try.of(()->new SubnetUtils(netmaskStr).getInfo().getCidrSignature()).getOrNull();
        final Map<String, Object> claims = formData.claims;
        final int expirationSeconds      = formData.expirationSeconds;



        if(expirationSeconds < 0) {

            return ExceptionMapperUtil.createResponse(new DotStateException("Invalid expirationSeconds"), Response.Status.BAD_REQUEST);
        }

        Date expires = Date.from(Instant.now().plus(expirationSeconds, ChronoUnit.SECONDS));
        ApiToken token = ApiToken.builder()
                .withAllowNetwork(netmask)
                .withIssueDate(new Date())
                .withClaims(claims)
                .withUser(forUser)
                .withRequestingIp(request.getRemoteAddr())
                .withRequestingUserId(requestingUser.getUserId())
                .withExpires( Date.from(Instant.now().plus(expirationSeconds, ChronoUnit.SECONDS)))
                .build();

        token = this.tokenApi.persistApiToken(token, requestingUser);
        final String jwt = this.tokenApi.getJWT(token, requestingUser);
        return Response.ok(new ResponseEntityMapView(Map.of("token", token, "jwt", jwt)))
                .build(); // 200
    }

    private User getUserById(ApiTokenForm formData, User requestingUser) {
        final User forUser = Try.of(()->
                        APILocator.getUserAPI().loadUserById(formData.userId, requestingUser, false ))
                .getOrNull();
        return forUser;
    }

    /**
     * Request a Token to a remote server
     * @param request
     * @param response
     * @param formData - json data, expecting
     * <pre>
     * {
     *    token: {
     *      network: '192.168.1.0/24',
     *      expirationSeconds: 1000,
     *      claims: {
     *          label: 'Push Publish'
     *      }
     *    },
     * 	  remote: {
     * 	    host: 'localhost',
     * 		port: 8090,
     * 	    protocol: 'http'
     * 	  },
     * 	  auth: {
     * 	    login: admin@dotcms.com,
     * 		password: [password in Base64]
     * 	  }
     * }
     * </pre>
     * @return Response
     */
    @PUT
    @Path("/remote")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "putGetRemoteTokenV1",
            summary = "Generates a remote API token",
            description = "This endpoint takes as part of its payload authentication credentials for a user account " +
                    "on a remote dotCMS instance. It returns a token object that can be used to permit remote operation " +
                    "according to the role and permissions of the authenticated account.\n\nThis is used, for example, " +
                    "in configuring a [push publishing](https://www.dotcms.com/docs/latest/push-publishing) endpoint.\n\n" +
                    "Usable only by administrators.",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Remote token generated successfully",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityApiTokenWithJwtView.class),
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "  \"entity\": {\n" +
                                                            "    \"jwt\": \"string\",\n" +
                                                            "    \"token\": {\n" +
                                                            "      \"allowNetwork\": \"0.0.0.0/0\",\n" +
                                                            "      \"claims\": {\n" +
                                                            "        \"label\": \"string\"\n" +
                                                            "      },\n" +
                                                            "      \"expired\": false,\n" +
                                                            "      \"expiresDate\": 0 \n," +
                                                            "      \"id\": \"string\",\n" +
                                                            "      \"issueDate\": 0,\n" +
                                                            "      \"issuer\": \"string\",\n" +
                                                            "      \"modificationDate\": 0,\n" +
                                                            "      \"notBeforeDate\": false,\n" +
                                                            "      \"requestingIp\": \"string\",\n" +
                                                            "      \"requestingUserId\": \"string\",\n" +
                                                            "      \"revoked\": false,\n" +
                                                            "      \"revokedDate\": 0,\n" +
                                                            "      \"subject\": \"string\",\n" +
                                                            "      \"tokenType\": \"string\",\n" +
                                                            "      \"userId\": \"string\",\n" +
                                                            "      \"valid\": true\n" +
                                                            "    }\n" +
                                                            "  },\n" +
                                                            "  \"errors\": [],\n" +
                                                            "  \"i18nMessagesMap\": {},\n" +
                                                            "  \"messages\": [],\n" +
                                                            "  \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Invalid user"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response getRemoteToken(
            @Context final HttpServletRequest httpRequest,
            @RequestBody(description = "PUT body consists of a JSON object containing three properties: `token`, concerning the token's " +
                    "direct properties; `remote`, defining the remote host, and `auth`, specifying remote user authentication.\n\n" +
                    "Each of these three top-level properties is itself an object containing further properties, listed fully below:\n\n" +
                    "| Properties                 | Value   | Description                                                            |\n" +
                    "|----------------------------|---------|------------------------------------------------------------------------|\n" +
                    "| `token.network`            | String  | Network mask in which the token is active.                             |\n" +
                    "| `token.expirationSeconds`  | String  | Seconds until the token expires.                                       |\n" +
                    "| `token.claims`             | Object  | Object containing the property `label`, defined below.                 |\n" +
                    "| `token.claims.label`       | String  | The name of the token generated.                                       |\n" +
                    "|             |   |  |\n" +
                    "| `remote.host`              | String  | Remote host for which to generate a token.                             |\n" +
                    "| `remote.port`              | String  | Port number for the remote host.                                       |\n" +
                    "| `remote.protocol`          | String  | Web protocol used to connect to the remote host.                       |\n" +
                    "|             |   |  |\n" +
                    "| `auth.login`               | String  | Email of account from which the remote token will derive permissions.  |\n" +
                    "| `auth.password`            | String  | A string representing a base64-encoded password.                       |\n",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RemoteAPITokenForm.class), // can't get this to introspect properly >:(
                            examples = {
                                    @ExampleObject(
                                            value = "{\n" +
                                                    "  \"token\": {\n" +
                                                    "    \"network\": \"0.0.0.0/0\",\n" +
                                                    "    \"expirationSeconds\": \"1000\",\n" +
                                                    "    \"claims\": {\n" +
                                                    "      \"label\": \"Example\"\n" +
                                                    "    }\n" +
                                                    "  },\n" +
                                                    "  \"remote\": {\n" +
                                                    "    \"host\": \"dotcms-receiver.local\",\n" +
                                                    "    \"port\": \"8082\",\n" +
                                                    "    \"protocol\": \"http\"\n" +
                                                    "  },\n" +
                                                    "  \"auth\": {\n" +
                                                    "    \"login\": \"admin@dotcms.com\",\n" +
                                                    "    \"password\": \"YWRtaW4=\"\n" +
                                                    "  }\n" +
                                                    "}"
                                    )
                            }
                    ))
            final RemoteAPITokenForm formData) {

        if (!Config.getBooleanProperty("ENABLE_PROXY_TOKEN_REQUESTS", true)) {
            final String message = "ENABLE_PROXY_TOKEN_REQUESTS is disabled, remote token is not allowed";
            SecurityLogger.logInfo(ApiTokenResource.class, message);
            throw new ForbiddenException("ENABLE_PROXY_TOKEN_REQUESTS should be true");
        }

        final InitDataObject initDataObject = this.webResource.init(null, true, httpRequest, true, null);

        if (!initDataObject.getUser().isAdmin()) {
            SecurityLogger.logInfo(ApiTokenResource.class, "Should be Admin user to request a remote token");
            throw new ForbiddenException("Should be Admin user to request a remote token");
        }

        final String protocol = formData.protocol();
        final String remoteURL = String.format("%s://%s:%d/api/v1/apitoken", protocol, formData.host(), formData.port());
        final Client client = getRestClient();

        try {

            final WebTarget webTarget = client.target(remoteURL);

            String password = "";

            if (UtilMethods.isSet(formData.password())) {
                password = new String(Base64.getDecoder().decode(formData.password()), java.nio.charset.StandardCharsets.UTF_8);
            }

            final Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((formData.login() + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .post(Entity.entity(formData.getTokenInfo(), MediaType.APPLICATION_JSON));

            if (response.getStatus() != HttpStatus.SC_OK) {
                final String message = String.format("Status code : %s", response.getStatus());

                if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED || response.getStatus() == HttpStatus.SC_FORBIDDEN ) {
                    SecurityLogger.logInfo(ApiTokenResource.class, message);
                } else {
                    Logger.error(ApiTokenResource.class, message);
                }
            }

            return Response
                    .status(response.getStatus())
                    .entity(response.readEntity(String.class))
                    .build();
        } catch (ProcessingException e){
            if (e.getCause().getClass() == UnknownHostException.class ||
                    e.getCause().getClass() == NoRouteToHostException.class ||
                    e.getCause().getClass() == ConnectException.class ||
                    e.getCause().getClass() == IllegalStateException.class) {
                Logger.error(ApiTokenResource.class, String.format("Invalid server URL: %s", remoteURL));
                return Response.status(Response.Status.NOT_FOUND).build();
            } else {
                throw e;
            }
        } finally {
            client.close();
        }
    }

    private Client getRestClient() {
        return RestClientBuilder.newClient();
    }

    /**
     * Get a JWT issued from a token
     * @param request
     * @param response
     * @param tokenId
     * @return
     */
    @GET
    @Path("/{tokenId}/jwt")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getJwtFromApiTokenV1",
            summary = "Generates a new JWT for an existing token",
            description = "Returns a JSON web token. This overwrites the JWT value associated with the " +
                    "specified token object.",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "JSON web token successfully created",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityJwtView.class),
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "    \"entity\": {\n" +
                                                            "        \"jwt\": \"string\"\n" +
                                                            "    },\n" +
                                                            "    \"errors\": [],\n" +
                                                            "    \"i18nMessagesMap\": {},\n" +
                                                            "    \"messages\": [],\n" +
                                                            "    \"pagination\": null,\n" +
                                                            "    \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Invalid user"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Token not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response getJwtFromApiToken(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("tokenId") @Parameter(
                    required = true,
                    description = "Identifier of API token to receive a new JWT.",
                    schema = @Schema(type = "string")
            ) final String tokenId) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user = initDataObject.getUser();
        final Optional<ApiToken> optToken = tokenApi.findApiToken(tokenId);

        if (optToken.isEmpty()) {

            return ExceptionMapperUtil.createResponse(new DotStateException("token id not found"), Response.Status.NOT_FOUND);
        }

        final ApiToken token = optToken.get();
        if(token.isExpired() || token.isRevoked()) {

            return ExceptionMapperUtil.createResponse(new DotStateException("token not valid"), Response.Status.BAD_REQUEST);
        }

        SecurityLogger.logInfo(this.getClass(), "Revealing token to user: " + user.getUserId() + " from: " + request.getRemoteAddr() + " token:"  + token );
        final String jwt = tokenApi.getJWT(token, user);
        return Response.ok(new ResponseEntityMapView(Map.of("jwt", jwt))).build(); // 200
    }



    @PUT
    @Path("/users/{userId}/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Hidden // This one doesn't seem to work; on 200 response, no token is revoked.
    @Operation(operationId = "putRevokeUserTokenV1",
            summary = "Revokes specified token from user",
            description = "This operation revokes all API tokens associated with a user. Usable only by administrators.",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens revoked successfully",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityMapView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized to remove tokens"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            })
    public final Response revokeUserToken(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("userId") @Parameter(
                    required = true,
                    description = "Identifier of user to have all tokens revoked.",
                    schema = @Schema(type = "string")
            ) final String userId) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource).rejectWhenNoUser(true)
                .requestAndResponse(request, response).requiredPortlet("users")
                .requiredFrontendUser(false)
                .requiredBackendUser(true).init();

        if (APILocator.getRoleAPI().doesUserHaveRole(initDataObject.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())) {

            final User user      = initDataObject.getUser();
            final User userToken = APILocator.getUserAPI().loadUserById(userId);

            if (null != userToken) {

                SecurityLogger.logInfo(this.getClass(), "Revoking token " + userId + " from " + request.getRemoteAddr() + " ");
                userToken.setSkinId(UUIDGenerator.generateUuid()); // setting a new id will invalidate the token
                APILocator.getUserAPI().save(userToken, user, PageMode.get(request).respectAnonPerms); // this will invalidate
                return Response.ok(new ResponseEntityMapView(Map.of("revoked", userId)))
                        .build(); // 200
            }
        } else {

            return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, initDataObject.getUser().getLocale(),
                    initDataObject.getUser().getUserId(),
                    "unauthorized to remove the token");
        }

        return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.NOT_FOUND, initDataObject.getUser().getLocale(),
                initDataObject.getUser().getUserId(),
                "No token");
    }

    @PUT
    @Path("/users/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Hidden // This one doesn't seem to work; revokes no tokens for any user.
    @Operation(operationId = "putRevokeAllUsersTokensV1",
            summary = "Revokes all users' tokens",
            description = "This operation revokes all tokens for all users. Usable only by administrators.",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User tokens successfully revoked",
                            content = @Content(mediaType = "application/json",
                                              schema = @Schema(implementation = ResponseEntityMapView.class),
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "  \"entity\": {\n" +
                                                            "    \"revoked\": \"string\"\n" +
                                                            "  },\n" +
                                                            "  \"errors\": [],\n" +
                                                            "  \"i18nMessagesMap\": {},\n" +
                                                            "  \"messages\": [],\n" +
                                                            "  \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized to remove tokens"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            })
    public final Response revokeUsersToken(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource).rejectWhenNoUser(true)
                .requestAndResponse(request, response).requiredPortlet("users")
                .requiredFrontendUser(false)
                .requiredBackendUser(true).init();

        if (APILocator.getRoleAPI().doesUserHaveRole(initDataObject.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())) {

            final User user                 = initDataObject.getUser();
            final List<User>   usersToken   = APILocator.getUserAPI().findAllUsers();
            final List<String> userTokenIds = new ArrayList<>();
            if (null != usersToken && !usersToken.isEmpty()) {

                for (final User userToken: usersToken) {

                    SecurityLogger.logInfo(this.getClass(), "Revoking token " + userToken.getUserId() + " from " + request.getRemoteAddr() + " ");
                    userToken.setSkinId(UUIDGenerator.generateUuid()); // setting a new id will invalidate the token
                    APILocator.getUserAPI().save(userToken, user, PageMode.get(request).respectAnonPerms); // this will invalidate
                    userTokenIds.add( userToken.getUserId());
                }

                return Response.ok(
                                new ResponseEntityMapView(
                                        Map.of("revoked", userTokenIds)))
                        .build(); // 200
            }
        } else {

            return ExceptionMapperUtil.createResponse(new DotStateException("unauthorized to remove the token"), Response.Status.UNAUTHORIZED);
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    @GET
    @Path("/expiring")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getExpiringApiTokensV1",
            summary = "Retrieves API tokens that are about to expire",
            description = "Returns a list of API tokens that will expire within the configured number of days.\n\n" +
                    "For admin users, returns all expiring tokens from all users.\n" +
                    "For limited users, returns only their own expiring tokens.\n\n" +
                    "The number of days to look ahead can be configured via the EXPIRING_TOKEN_LOOKAHEAD_DAYS property (default: 7).",
            tags = {"API Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Expiring API tokens successfully retrieved",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "  \"entity\": {\n" +
                                                            "    \"tokens\": [\n" +
                                                            "      {\n" +
                                                            "        \"expiresDate\": 1844834400000,\n" +
                                                            "        \"id\": \"apie3362144-8906-460d-b16e-e46a5bf69aef\",\n" +
                                                            "        \"issueDate\": 1750183464000,\n" +
                                                            "        \"userId\": \"dotcms.org.1\"\n" +
                                                            "      },\n" +
                                                            "      {\n" +
                                                            "        \"expiresDate\": 1844835400000,\n" +
                                                            "        \"id\": \"apie46a5bf69aef-8906-460d-asde-e46a5bf69aef\",\n" +
                                                            "        \"issueDate\": 1750183464000,\n" +
                                                            "        \"userId\": \"dotcms.org.1\"\n" +
                                                            "      }\n" +
                                                            "    ]\n" +
                                                            "  },\n" +
                                                            "  \"errors\": [],\n" +
                                                            "  \"i18nMessagesMap\": {},\n" +
                                                            "  \"messages\": [],\n" +
                                                            "  \"pagination\": null,\n" +
                                                            "  \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid user"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response getExpiringApiTokens(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user = initDataObject.getUser();

        final int daysLookahead = Config.getIntProperty("EXPIRING_TOKEN_LOOKAHEAD_DAYS", 7);

        if (daysLookahead < 0) {
            return ExceptionMapperUtil.createResponse(
                new DotStateException("Invalid EXPIRING_TOKEN_LOOKAHEAD_DAYS configuration: " + daysLookahead), 
                Response.Status.INTERNAL_SERVER_ERROR);
        }

        //Get the expiring tokens
        final List<ApiToken> expiringTokens = tokenApi.findExpiringTokens(daysLookahead, user);
        
        // Create token view
        final List<Map<String, Object>> tokenViews = ApiToken.toResponseViewList(expiringTokens);
        
        return Response.ok(new ResponseEntityView(Map.of("tokens", tokenViews), EMPTY_MAP)).build();
    }
}
