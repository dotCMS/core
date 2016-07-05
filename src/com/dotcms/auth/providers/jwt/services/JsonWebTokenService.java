package com.dotcms.auth.providers.jwt.services;

import java.io.Serializable;

import com.dotcms.auth.providers.jwt.beans.JWTBean;

/**
 * Encapsulates the logic that generates and reads the JSON Web Token (JWT).
 * This token is created when a user tries to log into the system.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016.
 */
public interface JsonWebTokenService extends Serializable {

	/**
	 * Generates the json token based on an id, subject and issuer. The
	 * ttlMillis determines how long will be valid the token
	 *
	 * @param jwtBean - 
	 *            {@link JWTBean}
	 * @return String the actual encrypted token
	 */
    public String generateToken(JWTBean jwtBean);

	/**
	 * Based on a json token return the JWTBean
	 * 
	 * @param jsonWebToken
	 *            - {@link String}
	 * @return JWTBean
	 */
    public JWTBean parseToken(String jsonWebToken);

} // E:O:F:JsonWebTokenService.
