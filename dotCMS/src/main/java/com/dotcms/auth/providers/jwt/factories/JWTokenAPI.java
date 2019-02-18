package com.dotcms.auth.providers.jwt.factories;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dotcms.auth.providers.jwt.beans.JWTokenIssued;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

import io.vavr.control.Try;


public class JWTokenAPI {


    private final static String SELECT_BY_TOKEN_USER_ID_SQL_ALL =
            "select * from jwt_token_issue where token_userid=? order by issue_date desc";
    private final static String SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE =
            "select * from jwt_token_issue where token_userid=? and reoke_date is null order by issue_date desc";
    private final static String SELECT_BY_TOKEN_ID_SQL = "select * from jwt_token_issue where token_id=?";
    private final static String UPDATE_REVOKE_TOKEN_SQL = "update jwt_token_issue set revoke_date=?, mod_date=? where token_id=?";
    private final static String INSERT_TOKEN_ISSUE_SQL =
            "insert into jwt_token_issue ( token_id,token_userid,issue_date,expire_date,requested_by_userid,requested_by_ip,revoke_date,allowed_from,cluster_id,meta_data,mod_date) values (?,?,?,?,?,?,?,?,?,?,?) ";
    public final static String TOKEN_404_STR = "TOKEN_404";

    private final JWTokenCache cache;

    public JWTokenAPI() {
        this(CacheLocator.getJWTokenCache());


    }

    public JWTokenAPI(JWTokenCache cache) {
        this.cache = cache;


    }


    private final static JWTokenIssued TOKEN_404 =
            JWTokenIssued.builder().withId(TOKEN_404_STR).withExpires(new Date()).withUserId(TOKEN_404_STR).build();


    public Optional<JWTokenIssued> findJWTokenIssued(final String tokenId) {

        final Optional<JWTokenIssued> optToken = cache.getToken(tokenId);
        if (!optToken.isPresent()) {
            JWTokenIssued token = this.findJWTokenIssuedB(tokenId).orElse(TOKEN_404);
            cache.putJWTokenIssued(tokenId, token);
            return Optional.ofNullable((TOKEN_404.equals(token) ? null : token));
        }


        return optToken;

    }


    @CloseDBIfOpened
    protected Optional<JWTokenIssued> findJWTokenIssuedB(final String tokenId) {

        try {

            return Optional
                    .of(new JWTokenDBTransformer(new DotConnect().setSQL(SELECT_BY_TOKEN_ID_SQL).addParam(tokenId).loadObjectResults())
                            .asList().get(0));
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        } catch (ArrayIndexOutOfBoundsException aar) {

            return Optional.empty();
        }


    }
    
    public boolean revokeToken(JWTokenIssued token) {
        return this.revokeToken(token.id);
    }

    @CloseDBIfOpened
    public boolean revokeToken(final String tokenId) {

        SecurityLogger.logInfo(this.getClass(), "revoking token " + tokenId);
        try {
            new DotConnect().setSQL(UPDATE_REVOKE_TOKEN_SQL).addParam(new Date()).addParam(new Date()).addParam(tokenId).loadResult();
            return true;
        } catch (DotDataException e) {
            throw new DotStateException(e);
        } finally {
            cache.removeToken(tokenId);
        }

    }


    public JWTokenIssued persistJWTokenIssued(final String userId, final Date expireDate, final String requestingUserId,
            final String requestingIpAddress) {

        User user = Try.of(()->APILocator.getUserAPI().loadUserById(requestingUserId)).getOrElseThrow(() -> new DotRuntimeException("Uable to load user" + requestingUserId));

        final JWTokenIssued tokenIssued = JWTokenIssued.builder().withUserId(userId).withExpires(expireDate)
                .withRequestingUserId(requestingUserId).withRequestingIp(requestingIpAddress).build();
        
        
        
        
        return persistJWTokenIssued(tokenIssued, user);


    }

    public JWTokenIssued persistJWTokenIssued(final JWTokenIssued tokenIssued, final User user) {


        return insertJWTokenIssuedDB(tokenIssued);


    }


    @CloseDBIfOpened
    private JWTokenIssued insertJWTokenIssuedDB(final JWTokenIssued token) {


        if (token.userId == null) {
            throw new DotStateException("JWtokenIssued requires a userId");
        }
        if (token.expires == null || token.expires.before(new Date())) {
            throw new DotStateException("JWtokenIssued requires an expiration in the future");
        }
        if (token.id != null) {
            throw new DotStateException("JWtokenIssued token IDs are generated when the JWtokenIssued is created");
        }

        final JWTokenIssued newToken =
                JWTokenIssued.from(token).withId(UUID.randomUUID().toString()).withModDate(new Date()).withIssueDate(new Date()).build();


        try {
            new DotConnect().setSQL(INSERT_TOKEN_ISSUE_SQL).addParam(newToken.id).addParam(newToken.userId).addParam(newToken.issueDate)
                    .addParam(newToken.expires).addParam(newToken.requestingUserId).addParam(newToken.requestingIp)
                    .addParam(newToken.revoked).addParam(newToken.allowFromNetwork).addParam(newToken.clusterId).addParam(newToken.metaData)
                    .addParam(newToken.modDate).loadResult();
            return newToken;
        } catch (DotDataException e) {
            throw new DotStateException(e);
        }


    }

    
    @CloseDBIfOpened
    protected List<JWTokenIssued> findJWTokensIssuedByUserIdDB(final String userId, final boolean showRevoked) {

        final String SQL = (showRevoked) ? SELECT_BY_TOKEN_USER_ID_SQL_ALL : SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE;


        try {
            return new JWTokenDBTransformer(new DotConnect().setSQL(SQL).addParam(userId).loadObjectResults()).asList();
        } catch (DotDataException dde) {

            throw new DotStateException(dde);
        }

    }


}
