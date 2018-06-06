package com.dotcms.auth.providers.jwt.factories;

import static com.dotcms.auth.providers.jwt.JsonWebTokenUtils.CLAIM_UPDATED_AT;

import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.Serializable;
import java.security.Key;
import java.util.Date;

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
            "com.dotcms.auth.providers.jwt.factories.impl.SecretKeySpecFactoryImpl";

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

        if (null == this.jsonWebTokenService) {

            synchronized (JsonWebTokenFactory.class) {

                try {

                    if (null == this.jsonWebTokenService) {
                        this.jsonWebTokenService = new JsonWebTokenServiceImpl();
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

        private Key signingKey;
        private String issuerId;

        private Key getSigningKey() {

            if (null == signingKey) {
                String signingKeyFactoryClass = Config
                        .getStringProperty(JSON_WEB_TOKEN_SIGNING_KEY_FACTORY,
                                DEFAULT_JSON_WEB_TOKEN_SIGNING_KEY_FACTORY_CLASS);

                if (UtilMethods.isSet(signingKeyFactoryClass)) {

                    if (Logger.isDebugEnabled(JsonWebTokenService.class)) {
                        Logger.debug(JsonWebTokenService.class,
                                "Using the signing key factory class: " + signingKeyFactoryClass);
                    }

                    SigningKeyFactory signingKeyFactory =
                            (SigningKeyFactory) ReflectionUtils.newInstance(signingKeyFactoryClass);
                    if (null != signingKeyFactory) {

                        signingKey = signingKeyFactory.getKey();
                    }
                }
            }

            return signingKey;
        }

        private String getIssuer() {

            if (null == issuerId) {
                issuerId = ClusterFactory.getClusterId();
            }

            return issuerId;
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
                    .claim(CLAIM_UPDATED_AT, jwtBean.getModificationDate())
                    .setSubject(jwtBean.getSubject())
                    .setIssuer(this.getIssuer())
                    .signWith(signatureAlgorithm, this.getSigningKey());

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
                        .setSigningKey(this.getSigningKey())
                        .parseClaimsJws(jsonWebToken);

                if (null != jws) {

                    final Claims body = jws.getBody();
                    if (null != body) {

                        // Validate the issuer is correct, meaning the same cluster id
                        if (!this.getIssuer().equals(body.getIssuer())) {
                            IncorrectClaimException claimException = new IncorrectClaimException(
                                    jws.getHeader(), jws.getBody(), "Invalid issuer");
                            claimException.setClaimName(Claims.ISSUER);
                            claimException.setClaimValue(body.getIssuer());

                            throw claimException;
                        }

                        jwtBean =
                                new JWTBean(body.getId(),
                                        body.getSubject(),
                                        body.getIssuer(),
                                        body.get(CLAIM_UPDATED_AT, Date.class),
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
