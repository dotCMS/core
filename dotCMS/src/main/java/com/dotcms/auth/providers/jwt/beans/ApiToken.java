package com.dotcms.auth.providers.jwt.beans;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import io.vavr.control.Try;


@JsonDeserialize(builder = ApiToken.Builder.class)
public class ApiToken implements JWToken {


    private static final long serialVersionUID = 1L;
    public final String id;
    public final String userId;
    public final String requestingUserId;
    public final String requestingIp;
    public final Date expiresDate;
    public final Date revoked;
    public final String allowNetwork;
    public final Date issueDate;
    public final Map<String,Object> claims;
    public final String issuer;
    public final Date modificationDate;


    private ApiToken(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.requestingUserId = builder.requestingUserId;
        this.requestingIp = builder.requestFromIp;
        this.expiresDate = builder.expiresDate;
        this.revoked = builder.revoked;
        this.allowNetwork = builder.allowNetwork;
        this.issueDate = builder.issueDate;
        this.claims = (builder.claims==null) ? ImmutableMap.of() : builder.claims;
        this.modificationDate = builder.modificationDate;
        this.issuer = builder.issuer;
    }


    public boolean isValid() {
        return isValid(null);
    }



    public boolean isRevoked() {
        return this.revoked != null && this.revoked.before(new Date());
    }

    public boolean isNotBeforeDate() {
        return this.issueDate != null && this.issueDate.after(new Date());
    }

    
    
    
    
    public boolean isInIpRange(final String ipAddress) {
        if (this.allowNetwork == null || "0.0.0.0/0".equals(this.allowNetwork)) {
            return true;
        }
        if(ipAddress==null) {
            return false;
        }
        try {
            SubnetUtils utils = new SubnetUtils(this.allowNetwork);
            utils.setInclusiveHostCount(true);
            return utils.getInfo().isInRange(ipAddress);

        } catch (Exception e) {
            Logger.warn(this.getClass(), "unable to validate ip address :" + ipAddress + " was part of network " + this.allowNetwork);
            Logger.warn(this.getClass(), e.getMessage());
            return false;
        }
    }

    public boolean isValid(final String ipAddress) {

        if (this.id == null || this.userId == null) {
            return false;
        }

        if (isRevoked()) {
            return false;
        }
        if (isExpired()) {
            return false;
        }


        if (isNotBeforeDate()) {
            return false;
        }
        if (!isInIpRange(ipAddress)) {
            return false;
        }


        return true;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((allowNetwork == null) ? 0 : allowNetwork.hashCode());
        result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
        result = prime * result + ((expiresDate == null) ? 0 : expiresDate.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((issueDate == null) ? 0 : issueDate.hashCode());
        result = prime * result + ((claims == null) ? 0 : claims.hashCode());
        result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
        result = prime * result + ((requestingIp == null) ? 0 : requestingIp.hashCode());
        result = prime * result + ((requestingUserId == null) ? 0 : requestingUserId.hashCode());
        result = prime * result + ((revoked == null) ? 0 : revoked.hashCode());

        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ApiToken other = (ApiToken) obj;
        if (allowNetwork == null) {
            if (other.allowNetwork != null)
                return false;
        } else if (!allowNetwork.equals(other.allowNetwork))
            return false;
        if (issuer == null) {
            if (other.issuer != null)
                return false;
        } else if (!issuer.equals(other.issuer))
            return false;
        if (expiresDate == null) {
            if (other.expiresDate != null)
                return false;
        } else if (!expiresDate.equals(other.expiresDate))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (issueDate == null) {
            if (other.issueDate != null)
                return false;
        } else if (!issueDate.equals(other.issueDate))
            return false;
        if (claims == null) {
            if (other.claims != null)
                return false;
        } else if (!claims.equals(other.claims))
            return false;
        if (requestingIp == null) {
            if (other.requestingIp != null)
                return false;
        } else if (!requestingIp.equals(other.requestingIp))
            return false;
        if (requestingUserId == null) {
            if (other.requestingUserId != null)
                return false;
        } else if (!requestingUserId.equals(other.requestingUserId))
            return false;
        if (revoked == null) {
            if (other.revoked != null)
                return false;
        } else if (!revoked.equals(other.revoked))
            return false;
        if (userId == null) {
            if (other.userId != null)
                return false;
        } else if (!userId.equals(other.userId))
            return false;
        return true;
    }


    /**
     * Creates builder to build {@link ApiToken}.
     * 
     * @return created builder
     */

    public static Builder builder() {
        return new Builder();
    }


    /**
     * Creates a builder to build {@link ApiToken} and initialize it with the given object.
     * 
     * @param apiToken to initialize the builder with
     * @return created builder
     */

    public static Builder from(ApiToken apiToken) {
        return new Builder(apiToken);
    }


    /**
     * Builder to build {@link ApiToken}.
     */

    public static final class Builder {
        private String id;
        private String userId;
        private String requestingUserId;
        private String requestFromIp;
        private Date expiresDate;
        private Date revoked;
        private String allowNetwork;
        private Date issueDate;
        private Map<String,Object> claims;
        private Date modificationDate;
        private String issuer;

        private Builder() {}

        private Builder(ApiToken apiToken) {
            this.id = apiToken.id;
            this.userId = apiToken.userId;
            this.requestingUserId = apiToken.requestingUserId;
            this.requestFromIp = apiToken.requestingIp;
            this.expiresDate = apiToken.expiresDate;
            this.revoked = apiToken.revoked;
            this.allowNetwork = apiToken.allowNetwork;
            this.issueDate = apiToken.issueDate;
            this.claims = apiToken.claims;
            this.modificationDate = apiToken.modificationDate;
            this.issuer = apiToken.issuer;
        }

        public Builder withId(@Nonnull String id) {
            this.id = id;
            return this;
        }

        public Builder withUserId(@Nonnull String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder withUser(@Nonnull User user) {
            this.userId = (user!=null) ? user.getUserId() : null;
            return this;
        }
        
        public Builder withRequestingUserId(@Nonnull String requestingUserId) {
            this.requestingUserId = requestingUserId;
            return this;
        }

        public Builder withRequestingIp(@Nonnull String requestFromIp) {
            this.requestFromIp = requestFromIp;
            return this;
        }

        public Builder withExpires(@Nonnull Date expiresDate) {

            this.expiresDate = expiresDate == null ? null : Date.from(expiresDate.toInstant().truncatedTo(ChronoUnit.SECONDS));
            return this;
        }

        public Builder withRevoked(@Nonnull Date revoked) {
            this.revoked = revoked == null ? null : Date.from(revoked.toInstant().truncatedTo(ChronoUnit.SECONDS));
            return this;
        }


        public Builder withAllowNetwork(@Nonnull String allowNetwork) {
            this.allowNetwork = allowNetwork;
            return this;
        }

        public Builder withIssueDate(@Nonnull Date issueDate) {
            this.issueDate = issueDate == null ? null : Date.from(issueDate.toInstant().truncatedTo(ChronoUnit.SECONDS));;
            return this;
        }

        public Builder withClaims(@Nonnull String claims) {
            this.claims = Try.of(()-> new ObjectMapper().readValue(claims,HashMap.class)).getOrElse(new HashMap<String, Object>());
            return this;
        }

        public Builder withClaims(@Nonnull Map<String,Object> claims) {
            this.claims = claims;
            return this;
        }
        public Builder withIssuer(@Nonnull String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder withModDate(@Nonnull Date modificationDate) {
            this.modificationDate = modificationDate == null ? null : Date.from(modificationDate.toInstant().truncatedTo(ChronoUnit.SECONDS));
            return this;
        }

        public ApiToken build() {
            if (ApiTokenAPI.TOKEN_404_STR.equals(id)) {
                return new ApiToken(this);
            }

            
            if (this.userId == null ) {
                throw new DotStateException("JWToken is is not valid - needs an userId");
            }
            
            if (this.expiresDate == null ) {
                throw new DotStateException("JWToken is is not valid - expire date not set or set in the past");
            }


            
            return new ApiToken(this);
        }
    }


    @Override
    public String toString() {

        return "{id:" + this.id + ", userId:" + this.userId + ", issueDate:" + this.issueDate + ", expiresDate:" + this.expiresDate + ", revoked:" + this.revoked + ", requestingUserId:" + this.requestingUserId + ", issuer:" + this.issuer + ", allowNetwork:" + this.allowNetwork +"}";
    }


    @Override
    public String getId() {
        return this.id;
    }


    @JsonIgnore
    public Optional<ApiToken> getApiToken() {
        return APILocator.getApiTokenAPI().findApiToken(this.id);
    }
    


    @Override
    public String getSubject() {
        return this.getId();
    }


    @Override
    public ImmutableMap<String, Object> getClaims() {

        return ImmutableMap.copyOf(claims);
    }

    @Override
    public String getIssuer() {
        return this.issuer;
    }


    @Override
    public Date getModificationDate() {
        return this.modificationDate;
    }


    @Override
    public Date getExpiresDate() {
        return this.expiresDate;
    }


    @Override
    public String getAllowNetwork() {
        return this.allowNetwork;
    }

    @Override
    public String getUserId() {
        return this.userId;
    }
    
    
    public Date getRevokedDate() {
        return this.revoked;
    }

    /**
     * Creates a simplified view map for API token responses.
     * This method extracts only the essential fields needed for API responses:
     * id, userId, expiresDate (as Date), and issueDate (as Date).
     * 
     * @param token The ApiToken to convert to a view map
     * @return Map containing simplified token data for API responses
     */
    public static Map<String, Object> toResponseView(ApiToken token) {
        return Map.of(
            "id", token.id,
            "userId", token.userId,
            "expiresDate", token.getExpiresDate(),
            "issueDate", token.issueDate
        );
    }

    /**
     * Creates a list of simplified view maps for API token responses.
     * This method transforms a list of ApiTokens into the format expected
     * by the API response structure.
     * 
     * @param tokens List of ApiTokens to convert to view maps
     * @return List of maps containing simplified token data for API responses
     */
    public static List<Map<String, Object>> toResponseViewList(List<ApiToken> tokens) {
        return tokens.stream()
            .map(ApiToken::toResponseView)
            .collect(Collectors.toList());
    }
    
    

}

