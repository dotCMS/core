package com.dotcms.auth.providers.jwt.factories;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.SecurityLogger;
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


    public Optional<ApiToken> findJWTokenIssued(final String tokenId) {
        if(!tokenId.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }
        final Optional<ApiToken> optToken = cache.getToken(tokenId);
        if (!optToken.isPresent()) {
            ApiToken token = this.findJWTokenIssuedDB(tokenId).orElse(TOKEN_404);
            cache.putJWTokenIssued(tokenId, token);
            return Optional.ofNullable((TOKEN_404.equals(token) ? null : token));
        }

        return optToken;

    }


    @CloseDBIfOpened
    protected Optional<ApiToken> findJWTokenIssuedDB(final String tokenId) {

        try {

            return Optional
                    .of(new ApiTokenDBTransformer(new DotConnect().setSQL(sql.SELECT_BY_TOKEN_ID_SQL).addParam(tokenId).loadObjectResults())
                            .asList().get(0));
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        } catch (ArrayIndexOutOfBoundsException aar) {

            return Optional.empty();
        }


    }

    public boolean revokeToken(ApiToken token) {
        return this.revokeToken(token.id);
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
            cache.removeToken(tokenId);
        }

    }


    public ApiToken persistJWTokenIssued(final String userId, final Date expireDate, final String requestingUserId,
            final String requestingIpAddress) {

        User user = Try.of(() -> APILocator.getUserAPI().loadUserById(requestingUserId))
                .getOrElseThrow(() -> new DotRuntimeException("Unable to load user" + requestingUserId));

        final ApiToken tokenIssued = ApiToken.builder().withUserId(userId).withExpires(expireDate)
                .withRequestingUserId(requestingUserId).withRequestingIp(requestingIpAddress).build();


        return persistJWTokenIssued(tokenIssued, user);


    }

    public ApiToken persistJWTokenIssued(final ApiToken tokenIssued, final User user) {

        ApiToken tokenRequested = ApiToken.from(tokenIssued).withRequestingUserId(user.getUserId()).build();

        return insertJWTokenIssuedDB(tokenRequested);


    }


    @CloseDBIfOpened
    private ApiToken insertJWTokenIssuedDB(final ApiToken token) {


        if (token.id != null) {
            throw new DotStateException("JWTokenIssued token IDs are generated when the JWTokenIssued is created");
        }
        if (token.requestingUserId == null || token.requestingIp == null) {
            throw new DotStateException("JWTokenIssued require requesting user and requesting ip addresses to be set");
        }
        if (token.allowFromNetwork != null) {
            Try.of(() -> new SubnetUtils(token.allowFromNetwork).getInfo())
                    .getOrElseThrow(() -> new DotStateException("allowFromNetwork:" + token.allowFromNetwork + " is invalid"));
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
            .addParam(newToken.metaData)
            .addParam(newToken.modDate).loadResult();

            return newToken;
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }

    }


    @CloseDBIfOpened
    protected List<ApiToken> findJWTokensIssuedByUserIdDB(final String userId, final boolean showRevoked) {

        final String SQL = (showRevoked) ? sql.SELECT_BY_TOKEN_USER_ID_SQL_ALL : sql.SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE;


        try {
            return new ApiTokenDBTransformer(new DotConnect().setSQL(SQL).addParam(userId).loadObjectResults()).asList();
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        }

    }


}
