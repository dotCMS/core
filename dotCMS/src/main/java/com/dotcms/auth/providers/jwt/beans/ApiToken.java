package com.dotcms.auth.providers.jwt.beans;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
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
    public final Date expires;
    public final Date revoked;
    public final String allowFromNetwork;
    public final Date issueDate;
    public final String claims;
    public final String clusterId;
    public final Date modDate;


    private ApiToken(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.requestingUserId = builder.requestingUserId;
        this.requestingIp = builder.requestFromIp;
        this.expires = builder.expires;
        this.revoked = builder.revoked;

        this.allowFromNetwork = builder.allowFromNetwork;
        this.issueDate = builder.issueDate;
        this.claims = builder.claims;
        this.modDate = builder.modDate;
        this.clusterId = builder.clusterId;
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
        if (this.allowFromNetwork == null || "0.0.0.0/0".equals(this.allowFromNetwork)) {
            return true;
        }
        try {
            return new SubnetUtils(this.allowFromNetwork).getInfo().isInRange(ipAddress);
        } catch (Exception e) {
            Logger.warn(this.getClass(), "unable to validate ip address :" + ipAddress + " was part of network " + this.allowFromNetwork);
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
        result = prime * result + ((allowFromNetwork == null) ? 0 : allowFromNetwork.hashCode());
        result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
        result = prime * result + ((expires == null) ? 0 : expires.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((issueDate == null) ? 0 : issueDate.hashCode());
        result = prime * result + ((claims == null) ? 0 : claims.hashCode());
        result = prime * result + ((modDate == null) ? 0 : modDate.hashCode());
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
        if (allowFromNetwork == null) {
            if (other.allowFromNetwork != null)
                return false;
        } else if (!allowFromNetwork.equals(other.allowFromNetwork))
            return false;
        if (clusterId == null) {
            if (other.clusterId != null)
                return false;
        } else if (!clusterId.equals(other.clusterId))
            return false;
        if (expires == null) {
            if (other.expires != null)
                return false;
        } else if (!expires.equals(other.expires))
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
     * @param jWTokenIssue to initialize the builder with
     * @return created builder
     */

    public static Builder from(ApiToken jWTokenIssue) {
        return new Builder(jWTokenIssue);
    }


    /**
     * Builder to build {@link ApiToken}.
     */

    public static final class Builder {
        private String id;
        private String userId;
        private String requestingUserId;
        private String requestFromIp;
        private Date expires;
        private Date revoked;
        private String allowFromNetwork;
        private Date issueDate;
        private String claims;
        private Date modDate;
        private String clusterId;

        private Builder() {}

        private Builder(ApiToken jWTokenIssue) {
            this.id = jWTokenIssue.id;
            this.userId = jWTokenIssue.userId;
            this.requestingUserId = jWTokenIssue.requestingUserId;
            this.requestFromIp = jWTokenIssue.requestingIp;
            this.expires = jWTokenIssue.expires;
            this.revoked = jWTokenIssue.revoked;
            this.allowFromNetwork = jWTokenIssue.allowFromNetwork;
            this.issueDate = jWTokenIssue.issueDate;
            this.claims = jWTokenIssue.claims;
            this.modDate = jWTokenIssue.modDate;
            this.clusterId = jWTokenIssue.clusterId;
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

        public Builder withExpires(@Nonnull Date expires) {

            this.expires = expires == null ? null : Date.from(expires.toInstant().truncatedTo(ChronoUnit.SECONDS));
            return this;
        }

        public Builder withRevoked(@Nonnull Date revoked) {
            this.revoked = revoked == null ? null : Date.from(revoked.toInstant().truncatedTo(ChronoUnit.SECONDS));
            return this;
        }


        public Builder withAllowFromNetwork(@Nonnull String allowFromNetwork) {
            this.allowFromNetwork = allowFromNetwork;
            return this;
        }

        public Builder withIssueDate(@Nonnull Date issueDate) {
            this.issueDate = issueDate == null ? null : Date.from(issueDate.toInstant().truncatedTo(ChronoUnit.SECONDS));;
            return this;
        }

        public Builder withClaims(@Nonnull String claims) {
            this.claims = claims;
            return this;
        }
        
        public Builder withClaims(@Nonnull Map<String,Object> claims) {
            this.claims = Try.of(()-> new ObjectMapper().writeValueAsString(claims)).getOrNull();
            return this;
        }
        public Builder withClusterId(@Nonnull String clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public Builder withModDate(@Nonnull Date modDate) {
            this.modDate = modDate == null ? null : Date.from(modDate.toInstant().truncatedTo(ChronoUnit.SECONDS));
            return this;
        }

        public ApiToken build() {
            if (ApiTokenAPI.TOKEN_404_STR.equals(id)) {
                return new ApiToken(this);
            }

            if (this.userId == null || this.expires == null || expires.before(new Date())) {
                throw new DotStateException("JWToken is is not valid - needs an userId, a requestingUser and an expires date");
            }


            return new ApiToken(this);
        }
    }


    @Override
    public String toString() {

        return "{id:" + this.id + ", userId:" + this.userId + ", issueDate:" + this.issueDate + ", expires:" + this.expires + ", revoked:"
                + this.revoked + "}";
    }


    @Override
    public String getId() {
        return this.id;
    }



    public Optional<ApiToken> getApiToken() {
        return APILocator.getApiTokenAPI().findApiToken(this.id);
    }
    


    @Override
    public String getSubject() {
        return this.getId();
    }


    @Override
    public ImmutableMap<String, Object> getClaims() {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> map =
                (HashMap<String, Object>) Try.of(() -> new ObjectMapper().readValue(this.claims, HashMap.class)).getOrElse(new HashMap<>());
        return ImmutableMap.copyOf(map);
    }

    @Override
    public String getIssuer() {
        return this.clusterId;
    }


    @Override
    public Date getModificationDate() {
        return this.modDate;
    }


    @Override
    public Date getExpiresDate() {
        return this.expires;
    }


    @Override
    public String getAllowNetworks() {
        return this.allowFromNetwork;
    }

    @Override
    public String getUserId() {
        return this.userId;
    }

}

