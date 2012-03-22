package com.dotmarketing.auth.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * The object is intended to be used to sync attributes from an outside directory to dotCMS.
 * 
 * @author jtesser
 * @version 1.5
 *
 */

public class UserAttribute {

	private String firstName;
	private String middleName;
	private String lastName;
	private String nickName;
	private String emailAddress;
	private boolean male;
	private Date birthday;
	/**
	 * Set to Locale.US by default
	 */
	private Locale locale = Locale.US;
	/**
	 * This should be filled with name/value pairs for the user_proxy table in the database.
	 * You must use the exact name of the databse field in the user_proxy table for each
	 * property.  
	 */
	private HashMap<String, Object> customProperties;
	/**
	 * Set to true if you want to sync addresses to the dotmcs.  If set to true all addresses that 
	 * exist in the dotcms and are not in the directory will be delete and all address in the directory 
	 * will be synced to the dotcms.
	 */
	private boolean syncAddress;
	private ArrayList<UserAddressAttribute> addressAttributes;
	
	public UserAttribute() {
		firstName = "";
		middleName = "";
		lastName = "";
		nickName = "";
		emailAddress = "changeme@dotcms.org";
		male = false;
		birthday = new Date();
		locale = Locale.US;
		syncAddress = false;
	}
	
	/**
	 * @return the birthday
	 */
	public Date getBirthday() {
		return birthday;
	}
	/**
	 * @param birthday the birthday to set
	 */
	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return the male
	 */
	public boolean isMale() {
		return male;
	}
	/**
	 * @param male the male to set
	 */
	public void setMale(boolean male) {
		this.male = male;
	}
	/**
	 * @return the middleName
	 */
	public String getMiddleName() {
		return middleName;
	}
	/**
	 * @param middleName the middleName to set
	 */
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	/**
	 * @return the nickName
	 */
	public String getNickName() {
		return nickName;
	}
	/**
	 * @param nickName the nickName to set
	 */
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	/**
	 * @return the customProperties
	 */
	public HashMap<String, Object> getCustomProperties() {
		return customProperties;
	}
	/**
	 * @param customProperties the customProperties to set
	 */
	public void setCustomProperties(HashMap<String, Object> customProperties) {
		this.customProperties = customProperties;
	}
	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}
	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	/**
	 * @return the syncAddress
	 */
	public boolean isSyncAddress() {
		return syncAddress;
	}
	/**
	 * @param syncAddress the syncAddress to set
	 */
	public void setSyncAddress(boolean syncAddress) {
		this.syncAddress = syncAddress;
	}
	/**
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}
	/**
	 * @param locale the locale to set
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * @return the addressAttributes
	 */
	public ArrayList<UserAddressAttribute> getAddressAttributes() {
		return addressAttributes;
	}

	/**
	 * @param addressAttributes the addressAttributes to set
	 */
	public void setAddressAttributes(
			ArrayList<UserAddressAttribute> addressAttributes) {
		this.addressAttributes = addressAttributes;
	}
}
