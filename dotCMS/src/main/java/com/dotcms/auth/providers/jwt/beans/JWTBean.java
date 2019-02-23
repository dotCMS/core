package com.dotcms.auth.providers.jwt.beans;

import java.io.Serializable;
import java.util.Date;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;

/**
 * Encapsulates all the different pieces of information that make up the JSON
 * Web Token (JWT).
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public class JWTBean implements Serializable {

    private final String id;
    private final String subject;
    private final String issuer;
    private final Date modificationDate;
    private final long ttlMillis;
    private final String allowedNetwork;
    private final ApiToken issued;
    
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
    public JWTBean(String id, String subject, String issuer, Date modificationDate,
            long ttlMillis, String allowedNetwork) {
        this.id = id;
        this.subject = subject;
        this.issuer = issuer;
        this.modificationDate = modificationDate;
        this.ttlMillis = ttlMillis;
        this.allowedNetwork = allowedNetwork;
        this.issued = APILocator.getApiTokenAPI().findJWTokenIssued(subject).orElse(null);
    }

    /**
     * Creates a JWT with its required information.
     *
     * @param id - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     */
    public JWTBean(String id, String subject, Date modificationDate, long ttlMillis) {
        this(id, subject, ClusterFactory.getClusterId(), modificationDate,ttlMillis,"0.0.0./0");
    }
    
    /**
     * Creates a JWT with its required information.
     *
     * @param id - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     */
    public JWTBean(String id, String subject, String issuer, Date modificationDate, long ttlMillis) {
        this(id, subject, issuer, modificationDate,ttlMillis,"0.0.0./0");
    }
    /**
     * Returns the ID of this token.
     * 
     * @return The token ID.
     */
    public String getId() {
        return id;
    }
    /**
     * Gets the allowed network
     * @return
     */
    public String getAllowedNetwork() {
        return allowedNetwork;
    }

    /**
     * Returns the subject of this token.
     * 
     * @return The token subject.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the issuer of this token.
     * 
     * @return The token issuer.
     */
    public String getIssuer() {
        return issuer;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    /**
     * Returns the time-to-live date of this token.
     * 
     * @return The token time-to-live date.
     */
    public long getTtlMillis() {
        return ttlMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JWTBean jwtBean = (JWTBean) o;

        if (ttlMillis != jwtBean.ttlMillis) return false;
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
        result = 31 * result + (modificationDate != null ? modificationDate.hashCode() : 0);
        result = 31 * result + (int) (ttlMillis ^ (ttlMillis >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "JWTBean{" +
                "id='" + id + '\'' +
                ", subject='" + subject + '\'' +
                ", modificationDate='" + modificationDate + '\'' +
                ", issuer='" + issuer + '\'' +
                ", ttlMillis=" + ttlMillis +
                '}';
    }

} // E:O:F:JWTBean.
