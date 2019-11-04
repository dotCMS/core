package com.dotcms.visitor.domain;

import java.io.Serializable;
import java.io.StringWriter;
import javax.annotation.Nonnull;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.vavr.control.Try;


@JsonDeserialize(builder = Geolocation.Builder.class)
public class Geolocation implements Serializable {

    private static final long serialVersionUID = 1L;
    private final double latitude, longitude;
    private final String country,countryCode, city, continent, continentCode, company, timezone, subdivision,subdivisionCode, ipAddress;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((city == null) ? 0 : city.hashCode());
        result = prime * result + ((company == null) ? 0 : company.hashCode());
        result = prime * result + ((continent == null) ? 0 : continent.hashCode());
        result = prime * result + ((continentCode == null) ? 0 : continentCode.hashCode());
        result = prime * result + ((country == null) ? 0 : country.hashCode());
        result = prime * result + ((countryCode == null) ? 0 : countryCode.hashCode());
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((subdivision == null) ? 0 : subdivision.hashCode());
        result = prime * result + ((subdivisionCode == null) ? 0 : subdivisionCode.hashCode());
        result = prime * result + ((timezone == null) ? 0 : timezone.hashCode());
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
        Geolocation other = (Geolocation) obj;
        if (city == null) {
            if (other.city != null)
                return false;
        } else if (!city.equals(other.city))
            return false;
        if (company == null) {
            if (other.company != null)
                return false;
        } else if (!company.equals(other.company))
            return false;
        if (continent == null) {
            if (other.continent != null)
                return false;
        } else if (!continent.equals(other.continent))
            return false;
        if (continentCode == null) {
            if (other.continentCode != null)
                return false;
        } else if (!continentCode.equals(other.continentCode))
            return false;
        if (country == null) {
            if (other.country != null)
                return false;
        } else if (!country.equals(other.country))
            return false;
        if (countryCode == null) {
            if (other.countryCode != null)
                return false;
        } else if (!countryCode.equals(other.countryCode))
            return false;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
            return false;
        if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
            return false;
        if (subdivision == null) {
            if (other.subdivision != null)
                return false;
        } else if (!subdivision.equals(other.subdivision))
            return false;
        if (subdivisionCode == null) {
            if (other.subdivisionCode != null)
                return false;
        } else if (!subdivisionCode.equals(other.subdivisionCode))
            return false;
        if (timezone == null) {
            if (other.timezone != null)
                return false;
        } else if (!timezone.equals(other.timezone))
            return false;
        return true;
    }


    public String getCountry() {
        return country;
    }


    public String getContinentCode() {
        return continentCode;
    }


    public String getSubdivisionCode() {
        return subdivisionCode;
    }


    public double getLatitude() {
        return latitude;
    }


    public double getLongitude() {
        return longitude;
    }


    public String getCountryCode() {
        return countryCode;
    }


    public String getCity() {
        return city;
    }


    public String getContinent() {
        return continent;
    }


    public String getCompany() {
        return company;
    }


    public String getTimezone() {
        return timezone;
    }


    public String getSubdivision() {
        return subdivision;
    }


    public String getIpAddress() {
        return ipAddress;
    }


    private Geolocation(Builder builder) {
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.countryCode = builder.countryCode;
        this.city = builder.city;
        this.continent = builder.continent;
        this.company = builder.company;
        this.timezone = builder.timezone;
        this.subdivision = builder.subdivision;
        this.ipAddress = builder.ipAddress;
        this.country = builder.country;
        this.subdivisionCode = builder.subdivisionCode;
        this.continentCode = builder.continentCode;
    }

    @JsonIgnore
    public String getLatLong() {

        StringWriter sw = new StringWriter();
        sw.append(String.valueOf(this.latitude));
        sw.append(",");
        sw.append(String.valueOf(this.longitude));
        return sw.toString();
    }
    
    @Override
    public String toString() {
        return Try.of(()->DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValueAsString(this)).getOrElse(super.toString());
        
    }
    /**
     * Creates a builder to build {@link Geolocation} and initialize it with the given object.
     * 
     * @param geolocation to initialize the builder with
     * @return created builder
     */
    public static Builder from(Geolocation geolocation) {
        return new Builder(geolocation);
    }

    /**
     * Builder to build {@link Geolocation}.
     */
    public static final class Builder {
        private  double latitude, longitude;
        private  String country,countryCode, city, continent, continentCode, company, timezone, subdivision,subdivisionCode, ipAddress;

        public Builder() {}

        private Builder(Geolocation geolocation) {
            this.latitude = geolocation.latitude;
            this.longitude = geolocation.longitude;
            this.countryCode = geolocation.countryCode;
            this.city = geolocation.city;
            this.continent = geolocation.continent;
            this.company = geolocation.company;
            this.timezone = geolocation.timezone;
            this.subdivision = geolocation.subdivision;
            this.ipAddress = geolocation.ipAddress;
            this.country = geolocation.country;
            this.subdivisionCode = geolocation.subdivisionCode;
            this.continentCode = geolocation.continentCode;
        }

        public Builder withLatitude(@Nonnull double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder withLongitude(@Nonnull double longitude) {
            this.longitude = longitude;
            return this;
        }
        
        public Builder withLatitude(Double latitude) {
            this.latitude = (latitude==null) ? 0d : latitude;
            return this;
        }

        public Builder withLongitude(Double longitude) {
            this.longitude = (longitude==null) ? 0d : longitude;
            return this;
        }

        public Builder withCountryCode(@Nonnull String countryCode) {
            this.countryCode = countryCode;
            return this;
        }
        public Builder withCountry(@Nonnull String country) {
            this.country = country;
            return this;
        }
        public Builder withCity(@Nonnull String city) {
            this.city = city;
            return this;
        }

        public Builder withContinent(@Nonnull String continent) {
            this.continent = continent;
            return this;
        }
        public Builder withContinentCode(@Nonnull String continentCode) {
            this.continentCode = continentCode;
            return this;
        }
        public Builder withSubdivisionCode(@Nonnull String subdivisionCode) {
            this.subdivisionCode = subdivisionCode;
            return this;
        }
        public Builder withCompany(@Nonnull String company) {
            this.company = company;
            return this;
        }

        public Builder withTimezone(@Nonnull String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder withSubdivision(@Nonnull String subdivision) {
            this.subdivision = subdivision;
            return this;
        }

        public Builder withIpAddress(@Nonnull String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Geolocation build() {
            return new Geolocation(this);
        }
    }



}
