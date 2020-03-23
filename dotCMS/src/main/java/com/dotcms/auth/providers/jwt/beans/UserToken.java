package com.dotcms.auth.providers.jwt.beans;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

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

    private UserToken(final Builder builder) {
        this.id                 = builder.id;
        this.subject            = builder.subject;
        this.issuer             = builder.issuer;
        this.modificationDate   = builder.modificationDate;
        this.expiresDate        = builder.expiresDate;
        this.claims             = builder.claims;
    }

    /**
     * @deprecated use the builder
     * @param identifier
     * @param subject
     * @param issuer
     * @param modificationDate
     * @param expiresDate
     * @param claims
     */
    @Deprecated
    public UserToken(final String identifier, final String subject, final String issuer, final Date modificationDate,
            final Date expiresDate, final Map<String,Object> claims) {
        this.id = identifier;
        this.subject = subject;
        this.issuer = issuer;
        this.modificationDate = modificationDate;
        this.expiresDate = expiresDate;
        this.claims=ImmutableMap.copyOf(claims);
    }
    
    
    
	/**
	 * Creates a JWT with its required information.
	 * 
	 * @param identifier
	 *            - The ID of the token.
	 * @param subject
	 *            - The subject of the token
	 * @param issuer
	 *            - The user issuing the token
	 * @param ttlMillis
	 *            - The expiration date of the token.
     * @deprecated use the builder
	 */
    @Deprecated
    public UserToken(final String identifier, final String subject, final String issuer, final Date modificationDate,
            final long ttlMillis, final Map<String,Object> claims) {
        this(identifier,subject,issuer,modificationDate,new Date(System.currentTimeMillis()+ ttlMillis), claims);
    }

    /**
     * Creates a JWT with its required information.
     *
     * @param identifier - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     * @deprecated use the builder
     */
    @Deprecated
    public UserToken(final String identifier, final String subject, final Date modificationDate, final long ttlMillis) {
        this(identifier, subject, ClusterFactory.getClusterId(), modificationDate,ttlMillis, ImmutableMap.of());
    }
    
    /**
     * Creates a JWT with its required information.
     *
     * @param identifier - The ID of the token.
     * @param subject - The subject of the token
     * @param ttlMillis - The expiration date of the token.
     * @deprecated use the builder
     */
    @Deprecated
    public UserToken(final String identifier, final String subject, final String issuer, final Date modificationDate, final long ttlMillis) {
        this(identifier, subject, issuer, modificationDate,ttlMillis, ImmutableMap.of());
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

    public static final class Builder {
        @JsonProperty(required = true)  private String id;
        @JsonProperty(required = true)  private String subject;
        @JsonProperty(required = true)  private String issuer = ClusterFactory.getClusterId();
        @JsonProperty(required = true)  private Date   modificationDate = new Date();
        @JsonProperty(required = true)  private Date   expiresDate;
        @JsonProperty(required = true)  private ImmutableMap<String,Object> claims = ImmutableMap.of();

        public UserToken.Builder id(final String id) {
            this.id = id;
            return this;
        }

        public UserToken.Builder subject(final String subject) {
            this.subject = subject;
            return this;
        }

        public UserToken.Builder issuer(final String issuer) {
            this.issuer = issuer;
            return this;
        }

        public UserToken.Builder modificationDate(final Date modificationDate) {
            this.modificationDate = modificationDate;
            return this;
        }
        public UserToken.Builder expiresDate(final Date expiresDate) {
            this.expiresDate = expiresDate;
            return this;
        }

        public UserToken.Builder expiresDate(final long ttlMillis) {
            this.expiresDate = new Date(System.currentTimeMillis()+ ttlMillis);
            return this;
        }

        public UserToken.Builder claims(final Map<String,Object> claims) {
            this.claims = ImmutableMap.copyOf(claims);
            return this;
        }

        public UserToken build() {
            return new UserToken(this);
        }
    }
} // E:O:F:JWTBean.
