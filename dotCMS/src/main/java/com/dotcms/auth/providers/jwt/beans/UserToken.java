package com.dotcms.auth.providers.jwt.beans;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.google.common.collect.ImmutableMap;

/**
 * Encapsulates all the different pieces of information that make up the JSON
 * Web Token (JWT).
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public class UserToken implements JWToken {

    private static final long serialVersionUID = 1L;
    private final String id;
    private final String subject;
    private final String issuer;
    private final Date modificationDate;
    private final Date expiresDate;
    private final String skinId;
    private final ImmutableMap<String,Object> claims;

    
    public UserToken(final String id, final String subject, final String issuer, final Date modificationDate,
            final Date expiresDate, final Map<String,Object> claims, final String skinId) {
        this.id = id;
        this.subject = subject;
        this.issuer = issuer;
        this.modificationDate = modificationDate;
        this.expiresDate = expiresDate;
        this.claims=ImmutableMap.copyOf(claims);
        this.skinId=skinId;
    }
    
    
    
	/**
	 * Creates a JWT with its required information.
	 * 
	 * @param id
	 *            - The ID of the token.
	 * @param subject
	 *            - The subject of the token
	 * @param issuer
	 *            - The user issuing the token
	 * @param ttlMillis
	 *            - The expiration date of the token.
	 */
    public UserToken(final String id, final String subject, final String issuer, final Date modificationDate,
            final long ttlMillis, final Map<String,Object> claims, final String skinId) {
        this(id,subject,issuer,modificationDate,new Date(System.currentTimeMillis()+ ttlMillis), claims, skinId);
        
    }

    /**
     * Creates a JWT with its required information.
     *
     * @param id - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     */
    public UserToken(String id, String subject, Date modificationDate, long ttlMillis, final String skinId) {
        this(id, subject, ClusterFactory.getClusterId(), modificationDate,ttlMillis, ImmutableMap.of(), skinId);
    }
    
    /**
     * Creates a JWT with its required information.
     *
     * @param id - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     */
    public UserToken(final String id, final String subject, final String issuer, final Date modificationDate, final long ttlMillis, final String skinId) {
        this(id, subject, issuer, modificationDate,ttlMillis, ImmutableMap.of(), skinId);
    }
    /**
     * Returns the ID of this token.
     * 
     * @return The token ID.
     */
    @Override
    public String getId() {
        return id;
    }

    
    public Optional<ApiToken> getApiToken() {
        return APILocator.getApiTokenAPI().findApiToken(this.subject);
    }
    
    /**
     * Returns the subject of this token.
     * 
     * @return The token subject.
     */
    @Override
    public String getSubject() {
        return subject;
    }
    
    @Override
    public ImmutableMap<String,Object> getClaims() {
        return this.claims;
    }
    

    /**
     * Returns the issuer of this token.
     * 
     * @return The token issuer.
     */
    @Override
    public String getIssuer() {
        return issuer;
    }
    @Override
    public Date getModificationDate() {
        return modificationDate;
    }

    /**
     * Returns the time-to-live date of this token.
     * 
     * @return The token time-to-live date.
     */
    @Override
    public Date getExpiresDate() {
        return expiresDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserToken jwtBean = (UserToken) o;

        if (!expiresDate.equals(jwtBean.expiresDate)) return false;
        if (modificationDate != jwtBean.modificationDate) {
            return false;
        }
        if (id != null ? !id.equals(jwtBean.id) : jwtBean.id != null) return false;
        if (subject != null ? !subject.equals(jwtBean.subject) : jwtBean.subject != null) return false;
        return issuer != null ? issuer.equals(jwtBean.issuer) : jwtBean.issuer == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (issuer != null ? issuer.hashCode() : 0);
        result = 31 * result + (claims != null ? claims.hashCode() : 0);
        result = 31 * result + (modificationDate != null ? modificationDate.hashCode() : 0);
        result = 31 * result + (expiresDate != null ? expiresDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JWTBean{" +
                "id='" + id + '\'' +
                ", subject='" + subject + '\'' +
                ", modificationDate='" + modificationDate + '\'' +
                ", issuer='" + issuer + '\'' +
                ", expiresDate=" + expiresDate +
                ", claims=" + claims +
                '}';
    }



    @Override
    public String getAllowNetwork() {
        return null;
    }

    @Override
    public String getUserId() {
        return this.subject;
    }

    public String getSkinId() {
        return skinId;
    }
} // E:O:F:JWTBean.
