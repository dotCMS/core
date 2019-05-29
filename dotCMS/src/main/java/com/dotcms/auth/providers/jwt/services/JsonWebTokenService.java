package com.dotcms.auth.providers.jwt.services;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.UserToken;

import java.io.Serializable;

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
	 * This token is mostly to encapsulate a login user into a JWT and it is associated to an specific user.
	 *
	 * @param JWToken -
	 *            {@link UserToken}
	 * @return String the actual encrypted token
	 */
    public String generateUserToken(UserToken JWToken);

	/**
	 * Generates the json token based
	 * ttlMillis determines how long will be valid the token
	 * This token is mostly to access to the API, it is not associated to any user itself.
	 *
	 * @param apiToken {@link ApiToken}
	 * @return String the actual encrypted token
	 */
	public String generateApiToken(ApiToken apiToken);
    
    
	/**
	 * Based on a json token returns the User JWToken
	 * 
	 * @param jsonWebToken
	 *            - {@link String}
	 * @return JWToken
	 */
    public JWToken parseToken(String jsonWebToken);

	/**
	 * Based on a json token returns the API JWToken
	 * @param jsonWebToken String encrypted token
	 * @param requestingIp String ip that is requesting the api token
	 * @return JWToken
	 */
    public JWToken parseToken(String jsonWebToken, String requestingIp);

} // E:O:F:JsonWebTokenService.
