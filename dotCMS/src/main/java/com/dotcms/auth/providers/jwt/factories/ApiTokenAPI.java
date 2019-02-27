package com.dotcms.auth.providers.jwt.factories;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.TokenType;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import io.vavr.control.Try;


public class ApiTokenAPI {


   public final static String TOKEN_404_STR = "TOKEN_404";
   public final static String TOKEN_PREFIX = "api";
   
    private final ApiTokenCache cache;
    private final ApiTokenSQL sql;
    public ApiTokenAPI() {
        this(CacheLocator.getApiTokenCache(), ApiTokenSQL.getInstance());
    }

    public ApiTokenAPI(final ApiTokenCache cache,final ApiTokenSQL sql ) {
        this.cache = cache;
        this.sql=sql;

    }


    private final static ApiToken TOKEN_404 = ApiToken.builder().withId(TOKEN_404_STR).build();

    /**
     * Returns the API token 
     * that matches the token id.  tokenId must start with "api"
     * @param tokenId
     * @return
     */
    public Optional<ApiToken> findApiToken(final String tokenId) {
        if(!tokenId.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }
        final Optional<ApiToken> optToken = cache.getApiToken(tokenId);
        if (!optToken.isPresent()) {
            ApiToken token = this.findApiTokenDB(tokenId).orElse(TOKEN_404);
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
     * @param tokenId
     * @return
     */
    public String getJWT(final ApiToken apiToken) {
        if(!findApiToken(apiToken.id).isPresent()) {
            throw new DotStateException("You can only get a JWT from a APIToken that has been persisted to db. Call persistApiToken first");
        }
        return JsonWebTokenFactory.getInstance().getJsonWebTokenService().generateApiToken(apiToken);
        

    }
    /**
     * this will either return your ApiToken or 
     * empty if token is expired/revoked/etc
     * @param jwt
     * @return
     */
    public Optional<JWToken> fromJwt(final String jwt, final String ipAddress) {
        

        JWToken bean  = Try.of(()->JsonWebTokenFactory.getInstance().getJsonWebTokenService().parseToken(jwt, ipAddress))
                .onFailure(e-> {
                    SecurityLogger.logInfo(this.getClass(), "JWT Failed from ipaddress:" + ipAddress + " " + e.getMessage());
                    Logger.warn(this.getClass(), e.getMessage());
                }).getOrNull();
                
        
        
        return Optional.ofNullable(bean) ;
        
    }
    /**
     * this will either return your ApiToken or 
     * empty if token is expired/revoked/etc
     * @param jwt
     * @return
     */
    public Optional<JWToken> fromJwt(final String jwt) {
        return fromJwt(jwt, null);
    }
    /**
     * Finds ApiToken in the db
     * @param tokenId
     * @return
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
     * deletes ApiToken in the db
     * @param tokenId
     * @return
     */
    @CloseDBIfOpened
    public boolean deleteToken(final String tokenId) {
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
     * deletes ApiToken in the db
     * @param token
     * @return
     */
    public boolean deleteToken(ApiToken token) {
        return this.deleteToken(token.id);
    }
    
    /**
     * sets the Token revoke date to now
     * invalidating all JWT issued for this ApiToken
     * only works if ApiToken is passed in
     * @param token
     * @return
     */
    public boolean revokeToken(JWToken token) {
        if(token.getTokenType()==TokenType.API_TOKEN ) {
            return revokeToken(token.getSubject());
        }
        return false;
    }
    
    /**
     * sets the Token revoke date to now
     * invalidating all JWT issued for this ApiToken
     * @param token
     * @return
     */
    public boolean revokeToken(ApiToken token) {
        return this.revokeToken(token.getSubject());
    }
    
    /**
     * sets the Token revoke date to now
     * invalidating all JWT issued for this ApiToken
     * @param tokenId
     * @return
     */
    @CloseDBIfOpened
    public boolean revokeToken(final String tokenId) {

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
     * @param userId
     * @param expireDate
     * @param requestingUserId
     * @param requestingIpAddress
     * @return
     */
    public ApiToken persistApiToken(final String userId, final Date expireDate, final String requestingUserId,
            final String requestingIpAddress) {

        User user = Try.of(() -> APILocator.getUserAPI().loadUserById(requestingUserId))
                .getOrElseThrow(() -> new DotRuntimeException("Unable to load user" + requestingUserId));

        final ApiToken tokenIssued = ApiToken.builder().withUserId(userId).withExpires(expireDate)
                .withRequestingUserId(requestingUserId).withRequestingIp(requestingIpAddress).build();


        return persistApiToken(tokenIssued, user);


    }
    /**
     * creates and inserts a new ApiToken and sets the requesting user to the user passed in
     * @param tokenIssued
     * @param user
     * @return
     */
    public ApiToken persistApiToken(final ApiToken apiToken, final User user) {

        ApiToken tokenRequested = ApiToken.from(apiToken).withRequestingUserId(user.getUserId()).build();

        return insertApiTokenDB(tokenRequested);


    }

    /**
     * Inserts ApiToken into the db
     * @param token
     * @return
     */
    @CloseDBIfOpened
    private ApiToken insertApiTokenDB(final ApiToken token) {


        if (token.id != null) {
            throw new DotStateException("ApiToken token IDs are generated when the ApiToken is created");
        }
        if (token.requestingUserId == null || token.requestingIp == null) {
            throw new DotStateException("ApiToken require requesting user and requesting ip addresses to be set");
        }

        
        
        if (token.allowFromNetwork != null && !token.allowFromNetwork.equals("0.0.0.0/0")) {
            Try.of(() -> new SubnetUtils(token.allowFromNetwork).getInfo())
                    .getOrElseThrow(() -> new DotStateException("allowFromNetwork:" + token.allowFromNetwork + " is invalid"));
        }

        
        if (token.claims != null) {
            Try.of(() -> new JSONObject(token.claims))
                    .getOrElseThrow(() -> new DotStateException("token.claims:" + token.claims + " must be valid json"));
        }
        
        
        


        final ApiToken newToken = ApiToken.from(token).withId(TOKEN_PREFIX + UUID.randomUUID().toString()).withModDate(new Date())
                .withIssueDate(new Date()).build();


        try {
            new DotConnect()
            .setSQL(sql.INSERT_TOKEN_ISSUE_SQL)
            .addParam(newToken.id)
            .addParam(newToken.userId)
            .addParam(newToken.issueDate)
            .addParam(newToken.expires)
            .addParam(newToken.requestingUserId)
            .addParam(newToken.requestingIp)
            .addParam(newToken.revoked)
            .addParam("0.0.0.0/0".equals(newToken.allowFromNetwork) ? null : newToken.allowFromNetwork)
            .addParam(newToken.clusterId)
            .addParam(newToken.claims)
            .addParam(newToken.modDate).loadResult();

            return newToken;
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }

    }

    /**
     * Finds all api tokens associated with a userId
     * @param userId
     * @param showRevoked
     * @return
     */
    @CloseDBIfOpened
    private List<ApiToken> findApiTokensByUserIdDB(final String userId, final boolean showRevoked) {

        final String SQL = (showRevoked) ? sql.SELECT_BY_TOKEN_USER_ID_SQL_ALL : sql.SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE;


        try {
            return new ApiTokenDBTransformer(new DotConnect().setSQL(SQL).addParam(userId).loadObjectResults()).asList();
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        }

    }
    /**
     * Finds all api tokens associated with a userId
     * Takes the requestingUser and checks permissions on them
     * @param userId
     * @param showRevoked
     * @return
     */
    @CloseDBIfOpened
    public List<ApiToken> findApiTokensByUserId(final String userId, final boolean showRevoked, final User requestingUser) {

        User userWithTokens = Try.of(()->APILocator.getUserAPI().loadUserById(userId, requestingUser, false)).getOrNull();
        if(userWithTokens==null || userWithTokens.getUserId()==null) {
            return ImmutableList.of();
        }

        return findApiTokensByUserIdDB(userId, showRevoked);

    }

}
