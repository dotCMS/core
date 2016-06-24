package com.dotmarketing.util.jwt;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ReflectionUtils;
import com.dotmarketing.util.UtilMethods;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.crypto.MacProvider;

import java.io.Serializable;
import java.security.Key;
import java.util.Date;

/**
 * This class in is charge of create the Token Factory.
 * It use the "json.web.token.signing.key.factory" on dotmarketing-config.properties ({@link SigningKeyFactory}
 * in order to get a custom implementation
 */
public class JsonWebTokenFactory implements Serializable {

    private static JsonWebTokenFactory instance = null;

    private JsonWebTokenService jsonWebTokenService = null;

    private JsonWebTokenFactory () {
        // singleton
    }

    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static JsonWebTokenFactory getInstance() {

        if (instance == null) {
            // Thread Safe. Might be costly operation in some case
            synchronized (JsonWebTokenFactory.class) {

                if (instance == null) {
                    instance = new JsonWebTokenFactory();
                }
            }
        }

        return instance;
    } // getInstance.

    /**
     * Get the signing key factory implementation from the dotmarketing-config.properties
     */
    public static final String JSON_WEB_TOKEN_SIGNING_KEY_FACTORY =
            "json.web.token.signing.key.factory";

    /**
     * Creates the Json Web Token Service based on the configuration on dotmarketing-config.properties
     * @return JsonWebTokenService
     */
    public  JsonWebTokenService getJsonWebTokenService () {

        Key key = null;
        SigningKeyFactory signingKeyFactory = null;
        String signingKeyFactoryClass = null;

        if (null == this.jsonWebTokenService) {

            synchronized (JsonWebTokenFactory.class) {

                try {
                    // Thread Safe. Might be costly operation in some case

                    if (null == this.jsonWebTokenService) {

                        signingKeyFactoryClass =
                                Config.getStringProperty(JSON_WEB_TOKEN_SIGNING_KEY_FACTORY, null);

                        if (null != signingKeyFactoryClass  && !"null".equals(signingKeyFactoryClass)) {

                            if (Logger.isDebugEnabled(JsonWebTokenService.class)) {

                                Logger.debug(JsonWebTokenService.class,
                                        "Using the singning key factory class: " + signingKeyFactoryClass);
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

                    this.jsonWebTokenService =
                            new JsonWebTokenServiceImpl();
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

        JsonWebTokenServiceImpl() {

            signingKey = MacProvider.generateKey();
        }


        JsonWebTokenServiceImpl(final Key signingKey) {

            this.signingKey = (null != signingKey)?
                    signingKey:
                    MacProvider.generateKey();
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

            //This line will throw an exception if it is not a signed JWS (as expected)
            final Jws<Claims> jws = Jwts.parser()
                    .setSigningKey(this.signingKey)
                    .parseClaimsJws(jsonWebToken);

            if ( null != jws ) {

                final Claims body = jws.getBody();
                if (null != body) {

                    jwtBean =
                            new JWTBean(body.getId(),
                                    body.getSubject(),
                                    body.getIssuer(),
                                    (null != body.getExpiration())?
                                            body.getExpiration().getTime():
                                            0);
                }
            }

            return jwtBean;
        } // parseToken.
    } // JsonWebTokenServiceImpl.

} // E:O:F:JsonWebTokenFactory.
