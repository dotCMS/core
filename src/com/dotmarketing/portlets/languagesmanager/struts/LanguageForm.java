/*
 * Created on Oct 20, 2004
 *
 */
package com.dotmarketing.portlets.languagesmanager.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.liferay.portal.util.Constants;

/**
 * @author alex
 *
 */
public class LanguageForm extends ValidatorForm {

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

	
	
	
	
	 public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.SAVE)) {
            return super.validate(mapping, request);
        }
        return null;
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
}
