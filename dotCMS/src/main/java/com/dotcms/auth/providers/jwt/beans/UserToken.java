package com.dotcms.auth.providers.jwt.beans;

import java.time.temporal.ChronoUnit;
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
    private final ImmutableMap<String,Object> claims;

    
    public UserToken(final String id, final String subject, final String issuer, final Date modificationDate,
            Date expiresDate, final Map<String,Object> claims) {
        this.id = id;
        this.subject = subject;
        this.issuer = issuer;
        // set mod date one minute in the future so as not to conflict with user sql update on login
        this.modificationDate = modificationDate!=null ? Date.from(modificationDate.toInstant().plus(1, ChronoUnit.MINUTES)): Date.from(new Date().toInstant().plus(1, ChronoUnit.MINUTES));
        this.expiresDate = expiresDate;
        this.claims=ImmutableMap.copyOf(claims);
        
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
            long ttlMillis, final Map<String,Object> claims) {
        this(id,subject,issuer,modificationDate,new Date(System.currentTimeMillis()+ ttlMillis), claims);
        
    }

    /**
     * Creates a JWT with its required information.
     *
     * @param id - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     */
    public UserToken(String id, String subject, Date modificationDate, long ttlMillis) {
        this(id, subject, ClusterFactory.getClusterId(), modificationDate,ttlMillis, ImmutableMap.of());
    }
    
    /**
     * Creates a JWT with its required information.
     *
     * @param id - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     */
    public UserToken(String id, String subject, String issuer, Date modificationDate, long ttlMillis) {
        this(id, subject, issuer, modificationDate,ttlMillis, ImmutableMap.of());
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


} // E:O:F:JWTBean.
