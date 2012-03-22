package com.dotmarketing.portlets.usermanager.struts;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * 
 * @author Oswaldo Gallango
 * 
 */
public class UserManagerForm extends ActionForm {

	private static final long serialVersionUID = 1L;

	// Liferay User fields
	private String userID;

	private String emailAddress;

	private String password;

	private String passChanged;

	private String newPassword;

	private String verifyPassword;

	private String challengeQuestionId;

	private String challengeQuestionAnswer;

	private String firstName;

	private String middleName;

	private String lastName;

	private String nickName;

	private String dateOfBirth;

	private Date dateOfBirthDate;

	private String sex;

	// Address fields
	private String description;

	private String addressID;

	private String typeAddress;

	private String street1;

	private String street2;

	private String city;

	private String state;

	private String zip;

	private String phone;

	private String fax;

	private String cell;

	private String school;

	private int graduation_year;

	private String country;

	// UserProxy
	private String userProxyInode;

	private String referrer;

	private String longLivedCookie;

	private String website;

	private String company;

	private String howHeard;

	private String prefix;

	private String otherPrefix;

	private String suffix;

	private String title;
	
	private String chapterOfficer;

	// Categories
	private String[] category;

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getHowHeard() {
		return howHeard;
	}

	public void setHowHeard(String howHeard) {
		this.howHeard = howHeard;
	}

	public String getLongLivedCookie() {
		return longLivedCookie;
	}

	public void setLongLivedCookie(String longLivedCookie) {
		this.longLivedCookie = longLivedCookie;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public UserManagerForm() {
		this.city = "";
		this.description = "";
		this.emailAddress = "";
		this.fax = "";
		this.firstName = "";
		this.lastName = "";
		this.passChanged = "false";
		this.otherPrefix = "";
		this.password = "";
		this.challengeQuestionAnswer = "";
		this.phone = "";
		this.prefix = "";
		this.state = "";
		this.street1 = "";
		this.street2 = "";
		this.suffix = "";
		this.title = "";
		this.typeAddress = "";
		this.verifyPassword = "";
		this.zip = "";
		this.school = "";
		this.cell = "";
		this.nickName = "";
		this.middleName = "";
		this.chapterOfficer = "";
	}

	/**
	 * @return the dateOfBirth
	 */
	public String getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * @param dateOfBirth the dateOfBirth to set
	 */
	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
		if (dateOfBirth != null && !dateOfBirth.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (dateOfBirth, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setDateOfBirthDate(cal.getTime());
		}
	}

	/**
	 * @return the dateOfBirthDate
	 */
	public Date getDateOfBirthDate() {
		return dateOfBirthDate;
	}

	/**
	 * @param dateOfBirthDate the dateOfBirthDate to set
	 */
	public void setDateOfBirthDate(Date dateOfBirthDate) {
		this.dateOfBirthDate = dateOfBirthDate;
	}

	@Override
	public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
		ActionErrors errors = new ActionErrors();

		boolean challengeQuestionProperty = false;

		try {
			challengeQuestionProperty = com.dotmarketing.util.Config.getBooleanProperty("USE_CHALLENGE_QUESTION");
		} catch (Exception e) {
			Logger.error(this, "UserManagerForm - Need to set USE_CHALLENGE_QUESTION property.");
		}
		if (!UtilMethods.isSet(firstName))
			errors.add("firstName", new ActionMessage("error.usermanager.form.mandatory", "First Name"));
		if (!UtilMethods.isSet(lastName))
			errors.add("lastName", new ActionMessage("error.usermanager.form.mandatory", "Last Name"));
		if (!UtilMethods.isSet(emailAddress))
			errors.add("emailAddress", new ActionMessage("error.usermanager.form.mandatory", "Email Address"));
		if (!emailAddress.matches("[^@]+@[^@]+"))
			errors.add("emailAddress", new ActionMessage("error.usermanager.form.format", "Email Address"));

		boolean isNewUser = !UtilMethods.isSet(getUserID());
		boolean isPwdSet = UtilMethods.isSet(password), isVerifyPwdSet = UtilMethods.isSet(verifyPassword);
		boolean isPwdFullySet = isPwdSet && isVerifyPwdSet;
		boolean isPwdPartiallySet = isPwdSet ^ isVerifyPwdSet;
		
		if(isNewUser){
			if ( ( isPwdFullySet && !password.equals(verifyPassword) ) || isPwdPartiallySet ) {
				// Password has not been properly specified?
				errors.add("newPassword", new ActionMessage("error.usermanager.form.verifyPassword"));
			}
		}
		else if( isNewUser && !isPwdFullySet ) {
			// New user and password has not been set?
			errors.add("newPassword", new ActionMessage("error.usermanager.form.setPassword"));
		}

		if (challengeQuestionProperty && (!UtilMethods.isSet(challengeQuestionId) || challengeQuestionId.equals("0")))
			errors.add("challengeQuestionId", new ActionMessage("error.usermanager.form.mandatory", "Challenge Question"));

		if (challengeQuestionProperty && !UtilMethods.isSet(challengeQuestionAnswer))
			errors.add("challengeQuestion", new ActionMessage("error.usermanager.form.mandatory", "Challenge Question Answer"));

		if (!"none".equals(description)) {
			if (!UtilMethods.isSet(street1) && !UtilMethods.isSet(street2) && !UtilMethods.isSet(city) && !UtilMethods.isSet(state) && !UtilMethods.isSet(zip)
					&& !UtilMethods.isSet(phone) && !UtilMethods.isSet(cell) ) {
				errors.add("phone", new ActionMessage("error.usermanager.form.mandatory", "phone"));
			}
		}
		return errors;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		if(emailAddress != null){
			emailAddress = emailAddress.toLowerCase();
		}
		this.emailAddress = emailAddress;
	}

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		if (fax != null && !fax.equals("null"))
			this.fax = fax;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getVerifyPassword() {
		return verifyPassword;
	}

	public void setVerifyPassword(String verifyPassword) {
		this.verifyPassword = verifyPassword;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		if (phone != null && !phone.equals("null"))
			this.phone = phone;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		if (prefix != null && !prefix.equals("null"))
			this.prefix = prefix;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStreet1() {
		return street1;
	}

	public void setStreet1(String street1) {
		this.street1 = street1;
	}

	public String getStreet2() {
		return street2;
	}

	public void setStreet2(String street2) {
		this.street2 = street2;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		if (suffix != null && !suffix.equals("null"))
			this.suffix = suffix;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (title != null && !title.equals("null"))
			this.title = title;
	}

	public String getTypeAddress() {
		return typeAddress;
	}

	public void setTypeAddress(String typeAddress) {
		this.typeAddress = typeAddress;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String[] getCategory() {
		return category;
	}

	public void setCategory(String[] category) {
		this.category = category;
	}

	public String getAddressID() {
		return addressID;
	}

	public void setAddressIS(String addressID) {
		this.addressID = addressID;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getUserProxyInode() {
		return userProxyInode;
	}

	public void setUserProxyInode(String userProxyInode) {
		this.userProxyInode = userProxyInode;
	}

	public void setAddressID(String addressID) {
		this.addressID = addressID;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getReferrer() {
		return referrer;
	}

	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}

	public String getPassChanged() {
		return passChanged;
	}

	public void setPassChanged(String newValue) {
		this.passChanged = newValue;
	}

	public String getOtherPrefix() {
		return otherPrefix;
	}

	public void setOtherPrefix(String otherPrefix) {
		this.otherPrefix = otherPrefix;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public boolean isCategorySelected(String strCategory) {
		if (this.category != null) {
			for (int i = 0; i < this.category.length; ++i) {
				if (this.category[i].equals(strCategory))
					return true;
			}
		}
		return false;
	}

	public int getGraduation_year() {
		return graduation_year;
	}

	public void setGraduation_year(int graduation_year) {
		this.graduation_year = graduation_year;
	}

	public String getSchool() {
		return school;
	}

	public void setSchool(String school) {
		this.school = school;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCell() {
		return cell;
	}

	public void setCell(String cell) {
		this.cell = cell;
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
	 * @return the sex
	 */
	public String getSex() {
		return sex;
	}

	/**
	 * @param sex the sex to set
	 */
	public void setSex(String sex) {
		this.sex = sex;
	}

	/**
	 * @return the challengeQuestionId
	 */
	public String getChallengeQuestionId() {
		return challengeQuestionId;
	}

	/**
	 * @param challengeQuestionId the challengeQuestionId to set
	 */
	public void setChallengeQuestionId(String challengeQuestionId) {
		this.challengeQuestionId = challengeQuestionId;
	}

	/**
	 * @return the challengeQuestionAnswer
	 */
	public String getChallengeQuestionAnswer() {
		return challengeQuestionAnswer;
	}

	/**
	 * @param challengeQuestionAnswer the challengeQuestionAnswer to set
	 */
	public void setChallengeQuestionAnswer(String challengeQuestionAnswer) {
		this.challengeQuestionAnswer = challengeQuestionAnswer;
	}
	
	/**
	 * @return the chapterOffice
	 */
	public String getChapterOfficer() {
		return chapterOfficer;
	}

	/**
	 * @param chapterOffice the chapterOffice to set
	 */
	public void setChapterOfficer(String chapterOfficer) {
		this.chapterOfficer = chapterOfficer;
	}
}