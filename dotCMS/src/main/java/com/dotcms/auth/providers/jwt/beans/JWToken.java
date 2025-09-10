package com.dotcms.auth.providers.jwt.beans;

import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.Logger;
import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import io.vavr.control.Try;

/**
 * Encapsulates all the different pieces of information that make up the JSON Web Token (JWT).
 * 
 */
public interface JWToken extends Serializable {

    /**
     * Gets a unique id of this token
     * 
     * @return
     */
    public String getId();

    /**
     * Either an api token id or a user id
     * 
     * @return
     */
    public String getSubject();

    public ImmutableMap<String, Object> getClaims();

    /**
     * this is the cluster Id in dotCMS land
     * 
     * @return
     */
    public String getIssuer();

    /**
     * when this token was last modified
     * @return
     */
    public Date getModificationDate();

    /**
     * When this token expires
     * @return
     */
    public Date getExpiresDate();

    /**
     * gets an cidr network which is validated when this token is used
     * 
     * @return
     */
    public String getAllowNetwork();

    /**
     * Optionally gets the user associated with this token. If the user is not active, no user will be
     * returned
     *
     * @return
     */
    @JsonIgnore
    default Optional<User> getActiveUser() {
        String subjectString = getUserId();

        String userIdString = (this instanceof ApiToken)
                ? subjectString
                : Try.of(()-> PublicEncryptionFactory.decryptString(subjectString)).onFailure(e-> Logger.debug(JWToken.class,"Subject Not Encrypted:" + e,e)).getOrElse(subjectString);
        User user = Try.of(() -> APILocator.getUserAPI().loadUserById(userIdString)).getOrNull();
        if (user != null && user.isActive()) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Returns the type of Token, either User or API token
     * 
     * @return
     */
    default TokenType getTokenType() {
        return TokenType.getTokenType(getSubject());
    }

    /**
     * Returns the associated userId for the token
     * 
     * @return
     */
    String getUserId();

    default boolean isExpired() {
        return getExpiresDate() == null || getExpiresDate().before(new Date());
    }


} // E:O:F:JWTBean.
