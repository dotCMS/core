package com.dotmarketing.util.jwt;

import java.io.Serializable;

/**
 * Encapsulates the logic for the Json Web Token
 * @author jsanca
 */
public interface JsonWebTokenService extends Serializable {

    /**
     * Generates the json token based on an id, subject and issuer.
     * The ttlMillis determines how long will be valid the token
     *
     * @param jwtBean {@link JWTBean}
     * @return String the actual encrypted token
     */
    public String generateToken(JWTBean jwtBean);

    /**
     * Based on a json token return the JWTBean
     * @param jsonWebToken {@link String}
     * @return JWTBean
     */
    public JWTBean parseToken(String jsonWebToken);
} // E:O:F:JsonWebTokenService.
