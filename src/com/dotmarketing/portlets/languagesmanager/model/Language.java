package com.dotmarketing.portlets.languagesmanager.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 *
 * @author  maria
 */
public class Language implements Serializable {
    private static final long serialVersionUID = 1L;

    /** identifier field */
    private long id;

    /** identifier field */
    private String languageCode;

    /** identifier field */
    private String countryCode;

    /** identifier field */
    private String language;

    /** nullable persistent field */
    private String country;

    /**
     * @param languageCode
     * @param countryCode
     * @param language
     * @param country
     */
    public Language(long id, String languageCode, String countryCode, String language, String country) {
        super();
        this.id = id;
        this.languageCode = languageCode;
        this.countryCode = countryCode;
        this.language = language;
        this.country = country;
    }

    public Language() {
        super();
        this.id = 0;
        this.languageCode = "";
        this.countryCode = "";
        this.language = "";
        this.country = "";
    }

    public Language(long id) {
        super();
        this.id = id;
        this.languageCode = "";
        this.countryCode = "";
        this.language = "";
        this.country = "";
    }

    /**
     * @return Returns the serialVersionUID.
     */
    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    /**
     * @return Returns the country.
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country The country to set.
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return Returns the countryCode.
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * @param countryCode The countryCode to set.
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * @return Returns the language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return Returns the languageCode.
     */
    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * @param languageCode The languageCode to set.
     */
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public int hashCode() {
        return new HashCodeBuilder().append(id).toHashCode();
    }

    /**
     * @return Returns the id.
     */
    public long getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(long id) {
        this.id = id;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Language)) {
            return false;
        }

        Language castOther = (Language) other;

        return new EqualsBuilder().append(this.id, castOther.id).isEquals();
    }
}
