package com.dotmarketing.portlets.languagesmanager.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import static com.liferay.util.StringPool.BLANK;

/**
 * This class represents a language in the system.
 * <p>Languages must be created and configured in dotCMS before contributors may create and edit content in different
 * languages on your site. Each dotCMS instance has a single Default Language, which both controls which language
 * version of content is returned when a request is made without a specified language, and which affects how other
 * language features operate, such as the Default Language Fallthrough Configuration.</p>
 *
 * @author  maria
 * @since Mar 22nd, 2012
 */
public class Language implements Serializable, ManifestItem {
    private static final long serialVersionUID = 1L;

    private long id;

    private String languageCode;

    private String countryCode;

    private String language;

    private String country;

    private String isoCode;

    /**
     * @param languageCode
     * @param countryCode
     * @param language
     * @param country
     */
    public Language(final long id, final String languageCode, final String countryCode, final String language, final String country) {
        this(id, languageCode, countryCode, language, country, null);
    }

    public Language() {
        this(0L);
    }

    public Language(long id) {
        this(id, BLANK, BLANK, BLANK, BLANK);
    }

    /**
     * Creates a new Language object with the specified properties.
     *
     * @param id           The unique identifier of the language.
     * @param languageCode The language code of the language.
     * @param countryCode  The country code of the language.
     * @param language     The name of the language.
     * @param country      The name of the country.
     * @param isoCode      The ISO code of the language.
     */
    public Language(final long id, final String languageCode, final String countryCode,
                    final String language, final String country, final String isoCode) {
        super();
        this.id = id;
        this.languageCode = languageCode;
        this.countryCode = countryCode;
        this.language = language;
        this.country = country;
        this.isoCode = isoCode;
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

    @JsonIgnore
    public Locale asLocale() {
      if(this.languageCode==null) {
        throw new DotRuntimeException("Locale requires a language code");
      }
      else if(this.countryCode==null) {
        return new Locale(this.languageCode);
      }else {
        return new Locale(this.languageCode, this.countryCode);
      }
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

    public String getIsoCode() {
        if (isoCode == null) {
            isoCode = ((StringUtils.isNotBlank(countryCode)) ? languageCode.toLowerCase() + "-"
                    + countryCode : languageCode).toLowerCase();
        }
        return isoCode;
    }

	@Override
	public String toString() {
		return getIsoCode();
	}

    @JsonIgnore
    @Override
    public ManifestInfo getManifestInfo(){
        return new ManifestInfoBuilder()
                .objectType(PusheableAsset.LANGUAGE.getType())
                .id(String.valueOf(this.id))
                .title(this.language)
                .build();
    }

    /**
     * Returns a map representation of the properties of this Language.
     *
     * @return a map representation of this object.
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "id", CollectionsUtils.orElseGet(this.id, 0L),
                "languageCode", CollectionsUtils.orElseGet(this.languageCode, BLANK),
                "countryCode", CollectionsUtils.orElseGet(this.countryCode, BLANK),
                "language", CollectionsUtils.orElseGet(this.language, BLANK),
                "country", CollectionsUtils.orElseGet(this.country, BLANK),
                "isoCode", CollectionsUtils.orElseGet(this.isoCode, BLANK));
    }

}
