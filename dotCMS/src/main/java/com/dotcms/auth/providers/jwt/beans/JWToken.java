package com.dotcms.auth.providers.jwt.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

/**
 * Encapsulates all the different pieces of information that make up the JSON Web Token (JWT).
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public interface JWToken extends Serializable {


    public String getId();

    public String getSubject();

    public ImmutableMap<String, Object> getClaims();

    public String getIssuer();
   
    public Date getModificationDate();

    public Date getExpiresDate();
    
    public String getAllowNetworks();
    
    public Optional<User> getUser();
    
    default TokenType getTokenType() {
        return TokenType.getTokenType(getSubject());
    }

} // E:O:F:JWTBean.
