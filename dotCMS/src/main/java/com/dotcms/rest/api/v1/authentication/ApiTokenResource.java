package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
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
import org.glassfish.jersey.internal.util.Base64;
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

import static java.util.Collections.EMPTY_MAP;

/**
 * Endpoint to handle Api Tokens
 */
@Path("/v1/apitoken")
@Tag(name = "Api Token",
        description = "Endpoints that handle operations related to Api Tokens",
        externalDocs = @ExternalDocumentation(description = "Additional Api Token information",
                url = "https://www.dotcms.com/docs/latest/rest-api-authentication#APIToken"))

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
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "getApiTokens",
            summary = "Gets api tokens from a user's ID",
            description = "Takes a user ID and returns a list of Api Tokens associated with the ID.\n\n" +
                    "The list of tokens also includes any tokens that have been revoked.\n\n",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User Api tokens successfully retrieved",
                            content = @Content(mediaType = "application/json",
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
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")

            }
    )
    public final Response getApiTokens(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("userId") @Parameter(
                    required = true,
                    description = "ID for user getting api tokens.",
                    schema = @Schema(type = "string")
            ) final String userId,
            @QueryParam("showRevoked") @Parameter(
                    description = "Determines whether revokes tokens are shown.",
                    schema = @Schema(type = "boolean")
            )final boolean showRevoked) {


        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final List<ApiToken> tokens = tokenApi.findApiTokensByUserId(userId, showRevoked, initDataObject.getUser());

        return Response.ok(new ResponseEntityView(Map.of("tokens", tokens), EMPTY_MAP)).build(); // 200
    }

    @PUT
    @Path("/{tokenId}/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(operationId = "putTokenIdRevoke",
            summary = "Revokes an api token from user(s)",
            description = "Takes a token ID and revokes it from any specified users.\n\n" +
                    "If the token attempmting to be revoked is expired the request will fail.\n\n",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens revoked successfully",
                            content = @Content(mediaType = "application/json",
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
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response revokeApiToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("tokenId") @Parameter(
                    required = true,
                    description = "ID of Api token being revoked",
                    schema = @Schema(type = "string"))
            @RequestBody(description = "This method takes an api-token ID and revokes it from the IP address where the request was made.\n\n" +
                    "If the token is expired then the system will return a message stating the token is expire.\n\n",
                    required = true,
                    content = @Content(
                            schema = @Schema(type = "String")
                    ))
            final String tokenId) {

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
            return Response.ok(new ResponseEntityView(Map.of("revoked", token), EMPTY_MAP)).build(); // 200
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }

    @DELETE
    @Path("/{tokenId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "deleteApiToken",
            summary = "Deletes an api-token",
            description = "Takes an api-token ID and deletes from the device connected to the IP address from the request.",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token successfully deleted",
                            content = @Content(mediaType = "application/json",
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
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response deleteApiToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("tokenId") @Parameter(
                    required = true,
                    description = "ID of Api token being revoked",
                    schema = @Schema(type = "string"))
            final String tokenId) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, "users");
        final User user                     = initDataObject.getUser();
        final Optional<ApiToken> optToken   = tokenApi.findApiToken(tokenId);

        if (optToken.isPresent()) {

            final ApiToken token = optToken.get();
            SecurityLogger.logInfo(this.getClass(), "Deleting token " + token + " from " + request.getRemoteAddr() + " ");

            if(tokenApi.deleteToken(token, user)) {

                return Response.ok(new ResponseEntityView(Map.of("deleted", token), EMPTY_MAP)).build(); // 200
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
    @Path("/")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "postIssueApiToken",
            summary = "Issues api token to authorized user",
            description = "Issues an api token to an admin user who has requested the token, no other user can request a token.",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token successfully issued to user",
                            content = @Content(mediaType = "application/json",
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
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response issueApiToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "This method requires a POST body of a JSON object containing the following properties.\n\n" +
                    "| **Property**    | **Value** | **Description**                               |\n" +
                    "|-----------------|-----------|-----------------------------------------------|\n" +
                    "| `userId`        | String    | **Required.** ID of user attempting receiving |\n" +
                    "| `tokenId`       | String    | ID of api token being issued                  |\n" +
                    "| `network`       | String    | User IP address                               |\n" +
                    "| `shouldBeAdmin` | Boolean   | Must be `true` to receive token               |",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ApiTokenForm.class))
            )
            final ApiTokenForm formData) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        final User requestingUser = initDataObject.getUser();
        final User forUser = formData.userId != null ? getUserById(formData, requestingUser) : requestingUser;

        if(forUser == null) {
            return ExceptionMapperUtil.createResponse(new DotStateException("No user found"), Response.Status.NOT_FOUND);
        }

        if (requestingUser != forUser && !requestingUser.isAdmin()) {
            throw new DotDataException("Just Admin user can request a Token for another user");
        }

        if (!forUser.isAdmin() && formData.shouldBeAdmin) {
            throw new DotSecurityException("User should be Admin");
        }

        final String netmaskStr = formData.network!=null && !"0.0.0.0/0".equals(formData.network)? formData.network:null;
        final String netmask    = Try.of(()->new SubnetUtils(netmaskStr).getInfo().getCidrSignature()).getOrNull();
        final Map<String, Object> claims = formData.claims;
        final int expirationSeconds      = formData.expirationSeconds;

        if(expirationSeconds < 0) {

            return ExceptionMapperUtil.createResponse(new DotStateException("invalid expirationDays"), Response.Status.BAD_REQUEST);
        }

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
        return Response.ok(new ResponseEntityView(Map.of("token", token,"jwt", jwt), EMPTY_MAP)).build(); // 200
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
     *      netmask:'192.168.1.0/24',
     *      expirationDays:1000,
     *      userId:'dotcms.org.1'
     *      claims: {
     *          label: 'Push Publish'
     *      }
     *    },
     * 	  remote: {
     * 	    host: localhost,
     * 		port: 8090
     * 	  },
     * 	  auth: {
     * 	    login: admin@dotcms.com,
     * 		pasword: [password in Base64]
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
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "putGetRemoteToken",
            summary = "Gets a remote api token",
            description = "Returns a remote token to the user who requested the token.\n\n" +
                    "Users must be an admin to request a token.",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Remote token retrieved successfully",
                            content = @Content(mediaType = "application/json",
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
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response getRemoteToken(
            @Context final HttpServletRequest httpRequest,
            @RequestBody(description = "PUT body consists of a JSON object containing the following properties, values are separated by their parent properites:\n\n" +
                    "| **Token Properties**  | **Value** | **Description**                                       |\n" +
                    "|-----------------------|-----------|-------------------------------------------------------|\n" +
                    "| `network`             | String    | IP address where token can be used                    |\n" +
                    "| `expirationSeconds`   | String    | Seconds until token expires                           |\n" +
                    "| `userId`              | String    | ID of user receiving remote token                     |\n" +
                    "| `tokenId`             | String    | ID of api token user is getting                       |\n" +
                    "| `label`               | String    | **Subsection of `claims`** Name of remote token       |\n\n" +
                    "| **Remote Properties** | **Value** | **Description**                                       |\n" +
                    "|-----------------------|-----------|-------------------------------------------------------|\n" +
                    "| `host`                | String    | Current host user is logged into; such as `localhost` |\n" +
                    "| `port`                | String    | Port number for host                                  |\n" +
                    "| `protocol`            | String    | Type of web protocol where the token resides          |\n\n" +
                    "| **Auth Properties**   | **Value** | **Description**                                       |\n" +
                    "|-----------------------|-----------|-------------------------------------------------------|\n" +
                    "| `login`               | String    | Username for login                                    |\n" +
                    "| `password`            | String    | User password                                         |",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RemoteAPITokenForm.class),
                            examples = {
                                    @ExampleObject(
                                            value = "{\n" +
                                                    "  \"token\": {\n" +
                                                    "    \"network\": \"0.0.0.0/0\",\n" +
                                                    "    \"expirationSeconds\": \"string\",\n" +
                                                    "    \"userId\": \"string\",\n" +
                                                    "    \"claims\": {\n" +
                                                    "      \"label\": \"string\"\n" +
                                                    "    }\n" +
                                                    "  },\n" +
                                                    "  \"remote\": {\n" +
                                                    "    \"host\": \"string\",\n" +
                                                    "    \"port\": \"string\",\n" +
                                                    "    \"protocol\": \"string\"\n" +
                                                    "  },\n" +
                                                    "  \"auth\": {\n" +
                                                    "    \"login\": \"string\",\n" +
                                                    "    \"password\": \"string\"\n" +
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
                password = Base64.decodeAsString(formData.password());
            }

            final Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Basic " + Base64.encodeAsString(formData.login() + ":" + password))
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getGetJwtFromApiToken",
            summary = "Retrieves JSON web token from api token",
            description = "Returns a JSON web token associated with an api token ID requested by a user, this token will then be included in a message to the user.",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "JSON web token successfully retrieved",
                            content = @Content(mediaType = "application/json",
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
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public final Response getJwtFromApiToken(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("tokenId") @Parameter(
                    required = true,
                    description = "Identifier for the api token getting its json web token retrieved.",
                    schema = @Schema(type = "string")
            )final String tokenId) {

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
        return Response.ok(new ResponseEntityView(Map.of("jwt", jwt), EMPTY_MAP)).build(); // 200
    }



    @PUT
    @Path("/users/{userid}/revoke")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "putRevokeUserToken",
            summary = "Revokes specified token from user",
            description = "This operation takes in a user id and retrieves any tokens associated to the user.\n\n" +
                    "If the user token is not null, then this token will be invalidated and revoked.",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token revoked successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RemoteAPITokenForm.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error")
            })
    public final Response revokeUserToken(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("userid") @Parameter(
                    required = true,
                    description = "Identification of the user whose user token is being revoked.",
                    schema = @Schema(type = "string")
            )
            @RequestBody(description = "PUT body conists of a JSON object containing the `userID` of the user whose tokens are being revoked.",
                    required = true,
                    content = @Content(
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
                    ))
            final String userid) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource).rejectWhenNoUser(true)
                .requestAndResponse(request, response).requiredPortlet("users")
                .requiredFrontendUser(false)
                .requiredBackendUser(true).init();

        if (APILocator.getRoleAPI().doesUserHaveRole(initDataObject.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())) {

            final User user      = initDataObject.getUser();
            final User userToken = APILocator.getUserAPI().loadUserById(userid);

            if (null != userToken) {

                SecurityLogger.logInfo(this.getClass(), "Revoking token " + userid + " from " + request.getRemoteAddr() + " ");
                userToken.setSkinId(UUIDGenerator.generateUuid()); // setting a new id will invalidate the token
                APILocator.getUserAPI().save(userToken, user, PageMode.get(request).respectAnonPerms); // this will invalidate
                return Response.ok(new ResponseEntityView(Map.of("revoked", userid), EMPTY_MAP)).build(); // 200
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "putRevokeUsersToken",
            summary = "Revokes users' tokens from list of tokens",
            description = "This operation checks if user has an admin role and retrieves all user tokens.\n\n" +
                    "Once all user tokens are received, each are invalidated and returned as a list of revoked tokens.",
            tags = {"Api Token"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User tokens successfully revoked",
                            content = @Content(mediaType = "application/json",
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

                return Response.ok(new ResponseEntityView(Map.of("revoked", userTokenIds), EMPTY_MAP)).build(); // 200
            }
        } else {

            return ExceptionMapperUtil.createResponse(new DotStateException("unauthorized to remove the token"), Response.Status.UNAUTHORIZED);
        }

        return ExceptionMapperUtil.createResponse(new DotStateException("No token"), Response.Status.NOT_FOUND);
    }
}