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
                    SecurityLogger.logInfo(this.getClass(), "from ipaddress:" + ipAddress + " " + e.getMessage());
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
    
    public boolean deleteToken(ApiToken token) {
        return this.deleteToken(token.id);
    }
    
    public boolean revokeToken(JWToken token) {
        if(token.getTokenType()==TokenType.API_TOKEN ) {
            return revokeToken(token.getSubject());
        }
        return false;
    }
    
    
    public boolean revokeToken(ApiToken token) {
        return this.revokeToken(token.getSubject());
    }

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


    public ApiToken persistApiToken(final String userId, final Date expireDate, final String requestingUserId,
            final String requestingIpAddress) {

        User user = Try.of(() -> APILocator.getUserAPI().loadUserById(requestingUserId))
                .getOrElseThrow(() -> new DotRuntimeException("Unable to load user" + requestingUserId));

        final ApiToken tokenIssued = ApiToken.builder().withUserId(userId).withExpires(expireDate)
                .withRequestingUserId(requestingUserId).withRequestingIp(requestingIpAddress).build();


        return persistApiToken(tokenIssued, user);


    }

    public ApiToken persistApiToken(final ApiToken tokenIssued, final User user) {

        ApiToken tokenRequested = ApiToken.from(tokenIssued).withRequestingUserId(user.getUserId()).build();

        return insertApiTokenDB(tokenRequested);


    }


    @CloseDBIfOpened
    private ApiToken insertApiTokenDB(final ApiToken token) {


        if (token.id != null) {
            throw new DotStateException("ApiToken token IDs are generated when the ApiToken is created");
        }
        if (token.requestingUserId == null || token.requestingIp == null) {
            throw new DotStateException("ApiToken require requesting user and requesting ip addresses to be set");
        }
        if (token.allowFromNetwork != null) {
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
            .addParam(newToken.allowFromNetwork)
            .addParam(newToken.clusterId)
            .addParam(newToken.claims)
            .addParam(newToken.modDate).loadResult();

            return newToken;
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }

    }


    @CloseDBIfOpened
    private List<ApiToken> findApiTokensByUserIdDB(final String userId, final boolean showRevoked) {

        final String SQL = (showRevoked) ? sql.SELECT_BY_TOKEN_USER_ID_SQL_ALL : sql.SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE;


        try {
            return new ApiTokenDBTransformer(new DotConnect().setSQL(SQL).addParam(userId).loadObjectResults()).asList();
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        }

    }

    @CloseDBIfOpened
    public List<ApiToken> findApiTokensByUserId(final String userId, final boolean showRevoked, final User requestingUser) {

        User userWithTokens = Try.of(()->APILocator.getUserAPI().loadUserById(userId, requestingUser, false)).getOrNull();
        if(userWithTokens==null || userWithTokens.getUserId()==null) {
            return ImmutableList.of();
        }

        return findApiTokensByUserIdDB(userId, showRevoked);

    }

}
