package com.dotcms.auth.providers.jwt.factories;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.vavr.control.Try;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Api to handle the generation of the Api tokens
 */
public class ApiTokenAPI {

   public final static String TOKEN_404_STR = "TOKEN_404";
   public final static String TOKEN_PREFIX  = "api";
   
    private final ApiTokenCache cache;
    private final ApiTokenSQL   sql;

    public ApiTokenAPI() {
        this(CacheLocator.getApiTokenCache(), ApiTokenSQL.getInstance());
    }

    @VisibleForTesting
    public ApiTokenAPI(final ApiTokenCache cache, final ApiTokenSQL sql) {

        this.cache = cache;
        this.sql   = sql;

    }

    private final static ApiToken TOKEN_404 = ApiToken.builder().withId(TOKEN_404_STR).build();

    /**
     * Returns the API token 
     * that matches the token id.  tokenId must start with "api"
     * @param tokenId {@link String}
     * @return Optional ApiToken
     */
    @CloseDBIfOpened
    public Optional<ApiToken> findApiToken(final String tokenId) {

        if(tokenId==null || !tokenId.startsWith(TOKEN_PREFIX)) {

            return Optional.empty();
        }

        final Optional<ApiToken> optToken = cache.getApiToken(tokenId);
        if (optToken.isEmpty()) {

            final ApiToken token = this.findApiTokenDB(tokenId).orElse(TOKEN_404);
            cache.putApiToken(tokenId, token);
            return Optional.ofNullable((TOKEN_404.equals(token) ? null : token));
        }

        return (TOKEN_404.equals(optToken.get()) ? Optional.empty() : optToken);
    }
    
    /**
     * Issues a unique compact JWT 
     * for the token.  Multiple JWTs can be issued, though
     * when the APIToken is revoked, they will all
     * become invalid
     * @param apiToken {@link ApiToken}
     * @param user     {@link User}
     * @return String
     */
    @CloseDBIfOpened
    public String getJWT(final ApiToken apiToken, final User user) {

        if(findApiToken(apiToken.id).isEmpty()) {

            throw new DotStateException("You can only get a JWT from a APIToken that has been persisted to db. Call persistApiToken first");
        }
        
        if(!checkPerms(apiToken, user)) {

            throw new DotStateException("User :" + user.getUserId() + " does not have permission to token " + apiToken);
        }

        if(apiToken.isExpired() || apiToken.isRevoked()) {

            throw new DotStateException("API token is either revoked or expired " + apiToken);
        }

        return JsonWebTokenFactory.getInstance().getJsonWebTokenService().generateApiToken(apiToken);
    }
    
    
    /**
     * this will either return user associated to the token or
     * empty if token is expired/revoked/etc
     * @param jwt  {@link String} encrypt jwt
     * @param ipAddress {@link String} request ip address
     * @return Optional User
     */
    @CloseDBIfOpened
    public Optional<User> userFromJwt(final String jwt, final String ipAddress) {
        

        final JWToken bean = Try.of(()->JsonWebTokenFactory.getInstance().getJsonWebTokenService().parseToken(jwt, ipAddress))
                .onFailure(e-> {
                    SecurityLogger.logInfo(this.getClass(), "JWT Failed from ipaddress:" + ipAddress + " " + e.getMessage());
                    Logger.warn(this.getClass(), e.getMessage());
                }).getOrNull();

        return bean != null?
                bean.getActiveUser():Optional.empty();
    }
    
    /**
     * This will either return your ApiToken or
     * empty if token is expired/revoked/etc
     * @param jwt  {@link String} encrypt jwt
     * @param ipAddress {@link String} request ip address
     * @return Optional JWTToken
     */
    @CloseDBIfOpened
    public Optional<JWToken> fromJwt(final String jwt, final String ipAddress) {
        

        JWToken bean  = Try.of(()->JsonWebTokenFactory.getInstance().getJsonWebTokenService().parseToken(jwt, ipAddress))
                .onFailure(e-> {
                    SecurityLogger.logInfo(this.getClass(), "JWT Failed from ipaddress:" + ipAddress + " " + e.getMessage());
                    Logger.warn(this.getClass(), e.getMessage());
                }).getOrNull();

        return Optional.ofNullable(bean);
    }

    /**
     * this will either return your ApiToken or 
     * empty if token is expired/revoked/etc
     * @param jwt {@link String} encrypt jwt
     * @return
     */
    public Optional<JWToken> fromJwt(final String jwt) {

        return fromJwt(jwt, null);
    }

    /**
     * Finds ApiToken in the db
     * @param tokenId {@link String} db token id
     * @return Optional ApiToken
     */
    @CloseDBIfOpened
    protected Optional<ApiToken> findApiTokenDB(final String tokenId) {

        try {

            return Optional
                    .of(new ApiTokenDBTransformer(new DotConnect().setSQL(sql.SELECT_BY_TOKEN_ID_SQL).addParam(tokenId).loadObjectResults())
                            .asList().get(0));
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        } catch (IndexOutOfBoundsException aar) {

            return Optional.empty();
        }
    }

    /**
     * Deletes ApiToken in the db
     * @param tokenId  {@link String} db token id
     * @return true if successfully removed
     */
    @WrapInTransaction
    private boolean deleteToken(final String tokenId) {

        SecurityLogger.logInfo(this.getClass(), "deleting token " + tokenId);

        try {

            new DotConnect().setSQL(sql.DELETE_TOKEN_SQL).addParam(tokenId).loadResult();
            return true;
        } catch (DotDataException e) {
            throw new DotStateException(e);
        } finally {
            cache.removeApiToken(tokenId);
        }
    }
    
    /**
     * deletes ApiToken in the db, if the permissions allowed the user to remove it, otherwise DotStateException will be throwed
     * @param token {@link ApiToken}
     * @param user  {@link User}
     * @return Boolean
     */
    @WrapInTransaction
    public boolean deleteToken(final ApiToken token, final User user) {
        
        if(this.checkPerms(token, user)) {

            return this.deleteToken(token.id);
        }

        throw new DotStateException("User :" + user.getUserId() + " does not have permission to token " + token);
    }

    
    /**
     * sets the Token revoke date to now
     * invalidating all JWT issued for this ApiToken
     * @param token {@link ApiToken}
     * @param user  {@link User} the user is used to validated the permissions to allow the operation
     * @return boolean
     */
    @WrapInTransaction
    public boolean revokeToken(final ApiToken token, final User user) {

        if(token.isExpired() || token.isRevoked()) {

            throw new DotStateException("Token is already revoked or expired");
        }

        if(checkPerms(token, user)){
            SecurityLogger.logInfo(this.getClass(), "Revoking token " + token  );
            return this.revokeTokenDb(token.getSubject());
        }
        
        throw new DotStateException("User :" + user.getUserId() + " does not have permission to token " + token);
    }
    
    /**
     * sets the Token revoke date to now
     * invalidating all JWT issued for this ApiToken
     * @param tokenId {@link String}
     * @return boolean
     */
    private boolean revokeTokenDb(final String tokenId) {

        SecurityLogger.logInfo(this.getClass(), "revoking token " + tokenId);

        try {

            new DotConnect().setSQL(sql.UPDATE_REVOKE_TOKEN_SQL).addParam(new Date()).addParam(new Date()).addParam(tokenId).loadResult();
            return true;
        } catch (DotDataException e) {
            throw new DotStateException(e);
        } finally {
            cache.removeApiToken(tokenId);
        }
    }


    /**
     * creates and inserts a new ApiToken
     * @param userId {@link String}
     * @param expireDate {@link Date}
     * @param requestingUserId {@link String}
     * @param requestingIpAddress {@link String}
     * @return ApiToken
     */
    @CloseDBIfOpened
    public ApiToken persistApiToken(final String userId, final Date expireDate, final String requestingUserId,
            final String requestingIpAddress) {

        return persistApiToken(userId, expireDate, requestingUserId, requestingIpAddress, null);
    }
    
    /**
     * creates and inserts a new ApiToken
     * @param userId {@link String}
     * @param expireDate {@link Date}
     * @param requestingUserId {@link String}
     * @param requestingIpAddress {@link String}
     * @param label {@link String}
     * @return ApiToken
     */
    @WrapInTransaction
    public ApiToken persistApiToken(final String userId, final Date expireDate, final String requestingUserId,
            final String requestingIpAddress, final String label) {

        final User user = Try.of(() -> APILocator.getUserAPI().loadUserById(requestingUserId))
                .getOrElseThrow(() -> new DotRuntimeException("Unable to load user" + requestingUserId));

        final Map<String,Object> claims = label != null?
                ImmutableMap.of("label", label) : ImmutableMap.of();

        final ApiToken tokenIssued = ApiToken.builder()
                .withUserId(userId)
                .withExpires(expireDate)
                .withClaims(claims)
                .withRequestingUserId(requestingUserId)
                .withRequestingIp(requestingIpAddress)
                .build();

        return persistApiToken(tokenIssued, user);
    }
    
    
    
    /**
     * creates and inserts a new ApiToken and sets the requesting user to the user passed in
     * @param apiToken {@link ApiToken}
     * @param user     {@link User}
     * @return ApiToken
     */
    @WrapInTransaction
    public ApiToken persistApiToken(final ApiToken apiToken, final User user) {

        final ApiToken tokenRequested = ApiToken.from(apiToken).withRequestingUserId(user.getUserId()).build();

        if(checkPerms(tokenRequested, user)) {

            return insertApiTokenDB(tokenRequested);
        }
        
        throw new DotStateException("User :" + user.getUserId() + " does not have permission to token " + tokenRequested);
    }

    /**
     * Inserts ApiToken into the db
     * @param token ApiToken
     * @return ApiToken
     */
    private ApiToken insertApiTokenDB(final ApiToken token) {

        if (token.id != null) {

            throw new DotStateException("ApiToken token IDs are generated when the ApiToken is created");
        }

        if (token.requestingUserId == null || token.requestingIp == null) {

            throw new DotStateException("ApiToken require requesting user and requesting ip addresses to be set");
        }

        if (token.allowNetwork != null && !token.allowNetwork.equals("0.0.0.0/0")) {

            Try.of(() -> new SubnetUtils(token.allowNetwork).getInfo())
                    .getOrElseThrow(() -> new DotStateException("allowFromNetwork:" + token.allowNetwork + " is invalid"));
        }

        
        if (token.claims != null) {

            Try.of(() -> new JSONObject(token.claims))
                    .getOrElseThrow(() -> new DotStateException("token.claims:" + token.claims + " must be valid json"));
        }

        final ApiToken newToken = ApiToken.from(token).withId(TOKEN_PREFIX + UUID.randomUUID().toString())
                .withModDate(new Date())
                .withIssuer(ClusterFactory.getClusterId())
                .withIssueDate(new Date())
                .build();

        try {

            new DotConnect()
            .setSQL(sql.INSERT_TOKEN_ISSUE_SQL)
            .addParam(newToken.id)
            .addParam(newToken.userId)
            .addParam(newToken.issueDate)
            .addParam(newToken.expiresDate)
            .addParam(newToken.requestingUserId)
            .addParam(newToken.requestingIp)
            .addParam(newToken.revoked)
            .addParam("0.0.0.0/0".equals(newToken.allowNetwork) ? null : newToken.allowNetwork)
            .addParam(newToken.issuer)
            .addParam(Try.of(()->new ObjectMapper().writeValueAsString(newToken.claims)).getOrNull())
            .addParam(newToken.modificationDate).loadResult();

            return newToken;
        } catch (DotDataException e) {

            throw new DotStateException(e);
        }
    }

    /**
     * Finds all api tokens associated with a userId
     * @param userId       {@link String}
     * @param showRevokedExpired {@link Boolean}
     * @return List of ApiToken
     */
    private List<ApiToken> findApiTokensByUserIdDB(final String userId, final boolean showRevokedExpired) {

        final List<ApiToken> tokenList= new ArrayList<>();

        try {

            tokenList.addAll(new ApiTokenDBTransformer(new DotConnect()
                    .setSQL(sql.SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE)
                    .addParam(userId)
                    .addParam(new Date())
                    .loadObjectResults()).asList());

            if(showRevokedExpired) {

                tokenList.addAll(new ApiTokenDBTransformer(new DotConnect()
                        .setSQL(sql.SELECT_BY_TOKEN_USER_ID_SQL_INACTIVE)
                        .addParam(userId)
                        .addParam(new Date())
                        .loadObjectResults()).asList());
            }

            return tokenList;
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        }
    }

    /**
     * Finds all api tokens associated with a userId
     * Takes the requestingUser and checks permissions on them
     * @param userId {@link String}
     * @param showRevoked {@link Boolean}
     * @param requestingUser {@link User}
     * @return
     */
    @CloseDBIfOpened
    public List<ApiToken> findApiTokensByUserId(final String userId, final boolean showRevoked, final User requestingUser) {

        final User userWithTokens = Try.of(()->APILocator.getUserAPI()
                .loadUserById(userId, requestingUser, false)).getOrNull();

        if(userWithTokens == null || userWithTokens.getUserId() == null) {

            return ImmutableList.of();
        }

       return findApiTokensByUserIdDB(userId, showRevoked).stream()
               .filter(token -> checkPerms(token, requestingUser))
               .collect(Collectors.toList());  
    }

    /***
     * checks if a requesting user has permissions over a token
     * true if any of these - user == token user || user == user who requested the token || user == cmsAdmin || user has userManager rights
     * @param token {@link ApiToken}
     * @param user  {@link User}
     * @return boolean
     */
    private boolean checkPerms(final ApiToken token, final User user) {

        if(token == null || user == null) {

            return false;
        }

        if(null == Try.of(() -> APILocator.getUserAPI().loadUserById(token.userId)).getOrNull()) {

            Logger.warn(this.getClass(), "trying to generate an API token for a non existing user.  Trying:" + token);
            return false;
        }

        return Try.of(() -> (APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())
                || user.getUserId().equals(token.getUserId()) 
                || user.getUserId().equals(token.requestingUserId))
                || APILocator.getPortletAPI().hasUserAdminRights(user))
                .getOrElse(false);
    }

    /**
     * Return true if the token was correctly constructed
     * @param token
     * @return
     */
    public  boolean isWellFormedToken(final String token){
        try {
            return JsonWebTokenUtils.getInstance().isWellFormed(token);
        } catch (MalformedJwtException e){
            return false;
        } catch(SignatureException | ExpiredJwtException | IllegalArgumentException e){
            return true;
        }

    }

    /**
     * Retrieves API tokens that are about to expire within the specified number of days.
     * For admin users, returns all expiring tokens from all users.
     * For limited users, returns only their own expiring tokens.
     * 
     * @param daysLookahead Number of days to look ahead for expiring tokens
     * @param requestingUser User making the request
     * @return List of ApiToken objects that will expire within the specified days
     */
    @CloseDBIfOpened
    public List<ApiToken> findExpiringTokens(final int daysLookahead, final User requestingUser) {
        List<ApiToken> allTokens = new ArrayList<>();
        
        try {
            if (requestingUser.isAdmin()) {
                // Admin users get all tokens from all users
                final List<User> allUsers = APILocator.getUserAPI().findAllUsers();
                for (User user : allUsers) {
                    List<ApiToken> userTokens = this.findApiTokensByUserId(user.getUserId(), false, requestingUser);
                    allTokens.addAll(userTokens);
                }
            } else {
                // Limited users get only their own tokens
                allTokens = this.findApiTokensByUserId(requestingUser.getUserId(), false, requestingUser);
            }
            
            // Filter tokens that will expire within the specified days
            return allTokens.stream()
                    .filter(Objects::nonNull)
                    .filter(token -> !token.isExpired() && !token.isRevoked())
                    .filter(token -> {
                        final long daysLeftToExpire = DateUtil.diffDates(new Date(), token.getExpiresDate()).get("diffDays");
                        return daysLeftToExpire >= 0 && daysLeftToExpire <= daysLookahead;
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            Logger.error(this, "Error retrieving expiring tokens: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

}
