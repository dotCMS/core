package com.dotcms.auth.providers.jwt.factories;

import io.jsonwebtoken.*;

import java.io.Serializable;
import java.security.Key;
import java.util.Date;

import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This class in is charge of create the Token Factory. It use the
 * "json.web.token.signing.key.factory" on dotmarketing-config.properties (
 * {@link SigningKeyFactory} in order to get a custom implementation
 * 
 * @author jsanca
 * @version 3.7
 * @since June 14, 2016
 */
public class JsonWebTokenFactory implements Serializable {

    /**
     * Used to keep the instance of the JWT Service.
     * Should be volatile to avoid thread-caching
     */
    private volatile JsonWebTokenService jsonWebTokenService = null;
    /**
     * Get the signing key factory implementation from the dotmarketing-config.properties
     */
    public static final String JSON_WEB_TOKEN_SIGNING_KEY_FACTORY =
            "json.web.token.signing.key.factory";
    /**
     * The default Signing Key class
     */
    public static final String DEFAULT_JSON_WEB_TOKEN_SIGNING_KEY_FACTORY_CLASS =
            "com.dotcms.auth.providers.jwt.factories.impl.HashSigningKeyFactoryImpl";

    private JsonWebTokenFactory () {
        // singleton
    }

    private static class SingletonHolder {
        private static final JsonWebTokenFactory INSTANCE = new JsonWebTokenFactory();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static JsonWebTokenFactory getInstance() {

        return JsonWebTokenFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Creates the Json Web Token Service based on the configuration on dotmarketing-config.properties
     * @return JsonWebTokenService
     */
    public JsonWebTokenService getJsonWebTokenService () {

        Key key = null;
        SigningKeyFactory signingKeyFactory = null;
        String signingKeyFactoryClass = null;

        if (null == this.jsonWebTokenService) {

            synchronized (JsonWebTokenFactory.class) {

                try {

                    if (null == this.jsonWebTokenService) {

						signingKeyFactoryClass = Config.getStringProperty(JSON_WEB_TOKEN_SIGNING_KEY_FACTORY,
								DEFAULT_JSON_WEB_TOKEN_SIGNING_KEY_FACTORY_CLASS);

                        if (UtilMethods.isSet(signingKeyFactoryClass)) {

                            if (Logger.isDebugEnabled(JsonWebTokenService.class)) {

                                Logger.debug(JsonWebTokenService.class,
                                        "Using the signing key factory class: " + signingKeyFactoryClass);
                            }

                            signingKeyFactory =
                                    (SigningKeyFactory) ReflectionUtils.newInstance(signingKeyFactoryClass);

                            if (null != signingKeyFactory) {

                                key = signingKeyFactory.getKey();
                            }
                        }

                        this.jsonWebTokenService =
                                new JsonWebTokenServiceImpl(key);
                    }
                } catch (Exception e) {

                    if (Logger.isErrorEnabled(JsonWebTokenService.class)) {

                        Logger.error(JsonWebTokenService.class, e.getMessage(), e);
                    }

                    if (Logger.isDebugEnabled(JsonWebTokenService.class)) {

                        Logger.debug(JsonWebTokenService.class,
                                "There is an error trying to create the Json Web Token Service, going with the default implementation...");
                    }
                }
            }
        }

        return this.jsonWebTokenService;
    } // getJsonWebTokenService

    /**
     * Default implementation
     * @author jsanca
     */
    private class JsonWebTokenServiceImpl implements JsonWebTokenService {

        private final Key signingKey;

		/**
		 * Instantiates the JWT Service using a valid signing key.
		 * 
		 * @param signingKey
		 *            - A secure signing key.
		 * @throws IllegalArgumentException
		 *             The provided key is null.
		 */
        JsonWebTokenServiceImpl(final Key signingKey) {
        	if (null == signingKey) {
        		throw new IllegalArgumentException("Signing key cannot be null");
        	}
            this.signingKey = signingKey;
        }

        @Override
        public String generateToken(final JWTBean jwtBean) {

            //The JWT signature algorithm we will be using to sign the token
            final SignatureAlgorithm signatureAlgorithm =
                    SignatureAlgorithm.HS256;

            final long nowMillis = System.currentTimeMillis();
            final Date now = new Date(nowMillis);

            //Let's set the JWT Claims
            final JwtBuilder builder = Jwts.builder()
                    .setId(jwtBean.getId())
                    .setIssuedAt(now)
                    .setSubject(jwtBean.getSubject())
                    .setIssuer(jwtBean.getIssuer())
                    .signWith(signatureAlgorithm, this.signingKey);

            //if it has been specified, let's add the expiration
            if ( jwtBean.getTtlMillis() >= 0 ) {

                final long expMillis = nowMillis + jwtBean.getTtlMillis();
                final Date exp = new Date(expMillis);
                builder.setExpiration(exp);
            }

            //Builds the JWT and serializes it to a compact, URL-safe string
            return builder.compact();
        } // generateToken.

        @Override
        public JWTBean parseToken(final String jsonWebToken) {

            JWTBean jwtBean = null;

            if ( !UtilMethods.isSet(jsonWebToken) ) {

                throw new IllegalArgumentException("Security Token not found");
            }

            try {
                //This line will throw an exception if it is not a signed JWS (as expected)
                final Jws<Claims> jws = Jwts.parser()
                        .setSigningKey(this.signingKey)
                        .parseClaimsJws(jsonWebToken);

                if (null != jws) {

                    final Claims body = jws.getBody();
                    if (null != body) {

                        jwtBean =
                                new JWTBean(body.getId(),
                                        body.getSubject(),
                                        body.getIssuer(),
                                        (null != body.getExpiration()) ?
                                                body.getExpiration().getTime() :
                                                0);
                    }
                }
            } catch (ExpiredJwtException e) {

                if (Logger.isDebugEnabled(this.getClass())) {

                    Logger.debug(this.getClass(),
                            e.getMessage(), e);
                }

                jwtBean = null;
            }

            return jwtBean;
        } // parseToken.
    } // JsonWebTokenServiceImpl.

} // E:O:F:JsonWebTokenFactory.
