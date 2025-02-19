package com.dotcms.auth.providers.jwt.factories;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.TokenType;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.jsonwebtoken.*;

import java.io.Serializable;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.dotcms.auth.providers.jwt.JsonWebTokenUtils.CLAIM_ALLOWED_NETWORK;
import static com.dotcms.auth.providers.jwt.JsonWebTokenUtils.CLAIM_UPDATED_AT;

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
     * 
     * @return JsonWebTokenService
     */
    public JsonWebTokenService getJsonWebTokenService() {

        if (null == this.jsonWebTokenService) {

            synchronized (JsonWebTokenFactory.class) {

                try {

                    if (null == this.jsonWebTokenService) {
                        this.jsonWebTokenService = new JsonWebTokenServiceImpl();
                    }

                } catch (Exception e) {


                    Logger.error(JsonWebTokenService.class, e.getMessage(), e);


                    Logger.debug(JsonWebTokenService.class,
                            "There is an error trying to create the Json Web Token Service, going with the default implementation...");

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

        private volatile Key signingKey;  // it is volatile since it is lazy loading, to avoid cache issues
        private volatile String issuerId; // it is volatile since it is lazy loading, to avoid cache issues

        private Key getSigningKey() {

            if (null == signingKey) {
                String signingKeyFactoryClass = Config
                        .getStringProperty(JSON_WEB_TOKEN_SIGNING_KEY_FACTORY,
                                DEFAULT_JSON_WEB_TOKEN_SIGNING_KEY_FACTORY_CLASS);

                if (UtilMethods.isSet(signingKeyFactoryClass)) {
                    Logger.debug(JsonWebTokenService.class, "Using the signing key factory class: " + signingKeyFactoryClass);

                    SigningKeyFactory signingKeyFactory =   (SigningKeyFactory) ReflectionUtils.newInstance(signingKeyFactoryClass);
                    if (null != signingKeyFactory) {

                        signingKey = signingKeyFactory.getKey();
                    }
                }
            }

            return signingKey;
        }

        private String getIssuer() {

            if (null == issuerId) {
                issuerId = Config.getStringProperty("JWT_CLAIMS_ISSUER" , ClusterFactory.getClusterId());
            }

            return issuerId;
        }

        
        @Override
        public String generateApiToken(final ApiToken apiToken) {

            final Map<String,Object> claims = new HashMap<>();

            apiToken.getClaims().forEach((key,value)->{
                if(value!=null) {

                    claims.put(key,value);
                }
            });
            
            claims.put(CLAIM_UPDATED_AT, apiToken.modificationDate);
            if(apiToken.allowNetwork!=null) {

                claims.put(CLAIM_ALLOWED_NETWORK, apiToken.allowNetwork);
            }

            //Let's set the JWT Claims
            final JwtBuilder builder = Jwts.builder()
                    .setClaims(claims)
                    .setId(UUID.randomUUID().toString())
                    .setSubject(apiToken.id)
                    .setExpiration(apiToken.expiresDate)
                    .setIssuedAt(apiToken.issueDate)
                    .setIssuer(this.getIssuer())
                    .setNotBefore(apiToken.issueDate);
                    
            return signToken(builder);
            
        }


        private String signToken(final JwtBuilder jwtBuilder) {
            //The JWT signature algorithm we will be using to sign the token
            jwtBuilder.setHeaderParam("typ", "JWT");
            jwtBuilder.signWith(signatureAlgorithm(), this.getSigningKey());
            
            return jwtBuilder.compact();
        }
        
        /**
         * this method returns the signatureAlgorithm
         * @return
         */
        private SignatureAlgorithm signatureAlgorithm() {
          final String algoFromKey  = this.getSigningKey().getAlgorithm();
          return Arrays.asList(SignatureAlgorithm.values())
            .stream()
            .filter(algo -> algoFromKey.equals(algo.getJcaName()))
            .findFirst()
            .get();
        }

        @Override
        public String generateUserToken(final UserToken jwtBean) {

            //Let's set the JWT Claims
            final JwtBuilder builder = Jwts.builder()
                    .setId(jwtBean.getId())
                    .claim(CLAIM_UPDATED_AT, jwtBean.getActiveUser().get().getModificationDate().getTime())
                    .claim(CLAIM_ALLOWED_NETWORK, jwtBean.getClaims().get(CLAIM_ALLOWED_NETWORK))
                    .setSubject(jwtBean.getSubject())
                    .setIssuedAt(new Date())
                    .setIssuer(this.getIssuer())
                    .setExpiration(jwtBean.getExpiresDate());

            //Builds the JWT and serializes it to a compact, URL-safe string
            return signToken(builder);
        } // generateToken.

        
        @Override
        public JWToken parseToken(final String jsonWebToken) {
            return parseToken(jsonWebToken, null);
        } // parseToken.
        
        @Override
        public JWToken parseToken(final String jsonWebToken, final String requestingIp) {
            final Jws<Claims> jws = Jwts.parser().setSigningKey(this.getSigningKey()).parseClaimsJws(jsonWebToken);
            
            return validateToken(jws, resolveJWTokenType(jws), requestingIp);

        } // parseToken.

        private JWToken validateToken(final Jws<Claims> jws, final JWToken jwtToken, final String requestingIp) {

            final Claims body = jws.getBody();
            if(jwtToken==null) {
                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), body, "No Valid Token Found for:" + body);
                claimException.setClaimName(Claims.SUBJECT);
                claimException.setClaimValue(body.getSubject());
                throw claimException;
            }

            // Insure that we do not accept null or NONE algo and that it matches the
            final String algo = jws.getHeader().getAlgorithm();
            if(algo==null || algo.equalsIgnoreCase("none") || ! algo.equalsIgnoreCase(signatureAlgorithm().getValue())) {
              throw new SignatureException( "Invalid JWT Signature Algorithm");
            }

            // Validate the issuer is correct, meaning the same cluster id
            if (!this.getIssuer().equals(body.getIssuer())  && Config.getBooleanProperty("JWT_CLAIM_ISSUER_VALIDATE", false)) {
                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), body, "Invalid JWT issuer. Expected:"  + this.getIssuer() + " and got:" + body.getIssuer());
                claimException.setClaimName(Claims.ISSUER);
                claimException.setClaimValue(body.getIssuer());
                throw claimException;
            }

            if(jwtToken.isExpired()) {
                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), body, "Token Expired:" + jwtToken.getExpiresDate());
                claimException.setClaimName(Claims.EXPIRATION);
                claimException.setClaimValue(jwtToken.getExpiresDate());
                throw claimException;
            }
            
            // get the user tied to the token
            final User user = jwtToken.getActiveUser().orElse(null);
            if(user==null || !user.isActive()) {
                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), body, "JWT Token user: " + jwtToken.getUserId() + " is not found or is not active");
                claimException.setClaimName(Claims.SUBJECT);
                claimException.setClaimValue(body.getSubject());
                throw claimException;
            }
            
            if(jwtToken.getTokenType() == TokenType.USER_TOKEN) {

                final String uuid = jwtToken.getId();

                if (null == uuid || !uuid.equals(user.getRememberMeToken())) {
                    final IncorrectClaimException claimException = new IncorrectClaimException(jws.getHeader(), body, "JWT Token user: " + jwtToken.getUserId() + " has been modified, old tokens are invalid");
                    claimException.setClaimName(Claims.SUBJECT);
                    claimException.setClaimValue(body.getSubject());
                    throw claimException;
                }

                return jwtToken;
            }

            ApiToken apiToken = (ApiToken) jwtToken;

            if(apiToken.isRevoked()) {
                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), body, "API Token Revoked:" + apiToken.revoked);
                claimException.setClaimName(Claims.EXPIRATION);
                claimException.setClaimValue(apiToken.revoked);
                throw claimException;
                
            }
            if(apiToken.isNotBeforeDate()) {
                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), body, "API Token Not Before:" + apiToken.issueDate);
                claimException.setClaimName(Claims.NOT_BEFORE);
                claimException.setClaimValue(apiToken.issueDate);
                throw claimException;
                
            }
            if(!apiToken.isInIpRange(requestingIp)) {
                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), body, "API Token not allowed for ip:" + requestingIp + ". Accepted range:" + apiToken.allowNetwork);
                claimException.setClaimName(Claims.AUDIENCE);
                claimException.setClaimValue(apiToken.allowNetwork);
                throw claimException;
            }

            return apiToken;
        }

        private JWToken resolveJWTokenType(final Jws<Claims> jws) {

           final Claims body = jws.getBody();
           return TokenType.getTokenType(body.getSubject()) == TokenType.USER_TOKEN ? 
                    new UserToken.Builder().id(body.getId())
                            .subject(body.getSubject())
                            .issuer(body.getIssuer())
                            .modificationDate(body.get(CLAIM_UPDATED_AT, Date.class))
                            .expiresDate(null != body.getExpiration()? body.getExpiration() : new Date(0))
                            .claims(body).build()
                   : APILocator.getApiTokenAPI().findApiToken(body.getSubject()).orElseGet(()->null);
        }

        /**
         * Security Complaining format validation
         * This is used to verify format on remote IP token received from a PP receiver environment
         * @param jsonWebToken
         * @return
         */
        public boolean isWellFormed(String jsonWebToken){
            final Jws<Claims> jws = Jwts.parser().setSigningKey(this.getSigningKey()).parseClaimsJws(jsonWebToken);
            return (jws.getBody() != null);
        }


    } // JsonWebTokenServiceImpl.

} // E:O:F:JsonWebTokenFactory.
