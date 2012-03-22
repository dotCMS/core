package com.dotmarketing.cms.myaccount.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

public class MyAccountForm extends ActionForm
{
	private static final long serialVersionUID = 1L;

	//Liferay User fields
	private String userId;
	private String emailAddress;
	private String password;
	private String newPassword;
	private String verifyPassword;
	private String firstName;
	private String lastName;
	
	//Extender user fields
	private String extUserInode;
	private String prefix;
	private String otherPrefix;
	private String suffix;
	private String title;
	
	//Address fields
	private String description;
	private String addressID;
	private String typeAddress;
	private String street1;
	private String street2;
	private String city;
	private String state;
	private String country;
	private String zip;
	private String phone;
	private String fax;
	private String stateOtherCountryText;
	
	//Organization 
	private boolean noOrganization;
	private String organizationInodeAux;
	private String organizationTitle;
	private String organizationCountry;
	private String organizationStreet1;
	private String organizationStreet2;
	private String organizationCity;
	private String organizationState;
	private String organizationZip;
	private String organizationPhone;
	private String organizationFax;
	private String selectedOrganization;
	
	//UserProxy
	private String userProxyInode;
	private String referrer;
	

	//Categories
	private String[] category;
	
    private boolean passwordChanged;

    private String organization;
    private String website;
    private boolean mailSubscription;
	
	private String tags;
	
	@Override
	public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
		ActionErrors errors = new ActionErrors ();
		
		if (arg1.getParameter("dispatch").equals("saveUserInfo")) {
			if (firstName.equals(""))
				errors.add("firstName", new ActionMessage ("error.form.mandatory", "First Name"));
			if (lastName.equals(""))
				errors.add("lastName", new ActionMessage ("error.form.mandatory", "Last Name"));
			if (emailAddress.equals(""))
				errors.add("emailAddress", new ActionMessage ("error.form.mandatory", "Email Address"));
			if (!emailAddress.matches("[^@]+@[^@]+"))
				errors.add("emailAddress", new ActionMessage ("error.form.format", "Email Address"));
			if (!newPassword.equals("") && !newPassword.equals(verifyPassword)) {
					errors.add("newPassword", new ActionMessage ("error.form.verifyPassword"));
			}
		} else if (arg1.getParameter("dispatch").equals("saveUserAddress")) {
			if (street1.equals(""))
				errors.add("street1", new ActionMessage ("error.form.mandatory", "Street 1"));
			if (city.equals(""))
				errors.add("city", new ActionMessage ("error.form.mandatory", "City"));
			if (country.equals(""))
				errors.add("country", new ActionMessage ("error.form.mandatory", "Country"));
			if (state.equals(""))
				errors.add("state", new ActionMessage ("error.form.mandatory", "State"));
			if (country.equals(""))
				errors.add("country", new ActionMessage ("error.form.mandatory", "Country"));
			if (zip.equals(""))
				errors.add("zip", new ActionMessage ("error.form.mandatory", "Zip"));
			if (phone.equals(""))
				errors.add("phone", new ActionMessage ("error.form.mandatory", "Phone"));
		} 
		return errors;
	}


    public String getOrganization() {
        return organization;
    }


    public void setOrganization(String organization) {
        this.organization = organization;
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


    public boolean isMailSubscription() {
        return mailSubscription;
    }


    public void setMailSubscription(boolean mailSubscription) {
        this.mailSubscription = mailSubscription;
    }


    public boolean isPasswordChanged() {
        return passwordChanged;
    }


    public void setPasswordChanged(boolean passwordChanged) {
        this.passwordChanged = passwordChanged;
    }



    public String getWebsite() {
        return website;
    }


    public void setWebsite(String website) {
        this.website = website;
    }


	/**
	 * @return Returns the addressID.
	 */
	public String getAddressID() {
		return addressID;
	}


	/**
	 * @param addressID The addressID to set.
	 */
	public void setAddressID(String addressID) {
		this.addressID = addressID;
	}


	/**
	 * @return Returns the category.
	 */
	public String[] getCategory() {
		return category;
	}


	/**
	 * @param category The category to set.
	 */
	public void setCategory(String[] category) {
		this.category = category;
	}


	/**
	 * @return Returns the city.
	 */
	public String getCity() {
		return city;
	}


	/**
	 * @param city The city to set.
	 */
	public void setCity(String city) {
		this.city = city;
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
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}


	/**
	 * @return Returns the emailAddress.
	 */
	public String getEmailAddress() {
		return emailAddress;
	}


	/**
	 * @param emailAddress The emailAddress to set.
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}


	/**
	 * @return Returns the extUserInode.
	 */
	public String getExtUserInode() {
		return extUserInode;
	}


	/**
	 * @param extUserInode The extUserInode to set.
	 */
	public void setExtUserInode(String extUserInode) {
		this.extUserInode = extUserInode;
	}


	/**
	 * @return Returns the fax.
	 */
	public String getFax() {
		return fax;
	}


	/**
	 * @param fax The fax to set.
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}


	/**
	 * @return Returns the newPassword.
	 */
	public String getNewPassword() {
		return newPassword;
	}


	/**
	 * @param newPassword The newPassword to set.
	 */
	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}


	/**
	 * @return Returns the noOrganization.
	 */
	public boolean isNoOrganization() {
		return noOrganization;
	}


	/**
	 * @param noOrganization The noOrganization to set.
	 */
	public void setNoOrganization(boolean noOrganization) {
		this.noOrganization = noOrganization;
	}


	/**
	 * @return Returns the organizationCity.
	 */
	public String getOrganizationCity() {
		return organizationCity;
	}


	/**
	 * @param organizationCity The organizationCity to set.
	 */
	public void setOrganizationCity(String organizationCity) {
		this.organizationCity = organizationCity;
	}


	/**
	 * @return Returns the organizationCountry.
	 */
	public String getOrganizationCountry() {
		return organizationCountry;
	}


	/**
	 * @param organizationCountry The organizationCountry to set.
	 */
	public void setOrganizationCountry(String organizationCountry) {
		this.organizationCountry = organizationCountry;
	}


	/**
	 * @return Returns the organizationFax.
	 */
	public String getOrganizationFax() {
		return organizationFax;
	}


	/**
	 * @param organizationFax The organizationFax to set.
	 */
	public void setOrganizationFax(String organizationFax) {
		this.organizationFax = organizationFax;
	}


	/**
	 * @return Returns the organizationInodeAux.
	 */
	public String getOrganizationInodeAux() {
		return organizationInodeAux;
	}


	/**
	 * @param organizationInodeAux The organizationInodeAux to set.
	 */
	public void setOrganizationInodeAux(String organizationInodeAux) {
		this.organizationInodeAux = organizationInodeAux;
	}


	/**
	 * @return Returns the organizationPhone.
	 */
	public String getOrganizationPhone() {
		return organizationPhone;
	}


	/**
	 * @param organizationPhone The organizationPhone to set.
	 */
	public void setOrganizationPhone(String organizationPhone) {
		this.organizationPhone = organizationPhone;
	}



	/**
	 * @return Returns the organizationState.
	 */
	public String getOrganizationState() {
		return organizationState;
	}


	/**
	 * @param organizationState The organizationState to set.
	 */
	public void setOrganizationState(String organizationState) {
		this.organizationState = organizationState;
	}


	/**
	 * @return Returns the organizationStreet1.
	 */
	public String getOrganizationStreet1() {
		return organizationStreet1;
	}


	/**
	 * @param organizationStreet1 The organizationStreet1 to set.
	 */
	public void setOrganizationStreet1(String organizationStreet1) {
		this.organizationStreet1 = organizationStreet1;
	}


	/**
	 * @return Returns the organizationStreet2.
	 */
	public String getOrganizationStreet2() {
		return organizationStreet2;
	}


	/**
	 * @param organizationStreet2 The organizationStreet2 to set.
	 */
	public void setOrganizationStreet2(String organizationStreet2) {
		this.organizationStreet2 = organizationStreet2;
	}


	/**
	 * @return Returns the organizationTitle.
	 */
	public String getOrganizationTitle() {
		return organizationTitle;
	}


	/**
	 * @param organizationTitle The organizationTitle to set.
	 */
	public void setOrganizationTitle(String organizationTitle) {
		this.organizationTitle = organizationTitle;
	}


	/**
	 * @return Returns the organizationZip.
	 */
	public String getOrganizationZip() {
		return organizationZip;
	}


	/**
	 * @param organizationZip The organizationZip to set.
	 */
	public void setOrganizationZip(String organizationZip) {
		this.organizationZip = organizationZip;
	}


	/**
	 * @return Returns the otherPrefix.
	 */
	public String getOtherPrefix() {
		return otherPrefix;
	}


	/**
	 * @param otherPrefix The otherPrefix to set.
	 */
	public void setOtherPrefix(String otherPrefix) {
		this.otherPrefix = otherPrefix;
	}


	/**
	 * @return Returns the password.
	 */
	public String getPassword() {
		return password;
	}


	/**
	 * @param password The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * @return Returns the phone.
	 */
	public String getPhone() {
		return phone;
	}


	/**
	 * @param phone The phone to set.
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}


	/**
	 * @return Returns the prefix.
	 */
	public String getPrefix() {
		return prefix;
	}


	/**
	 * @param prefix The prefix to set.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}


	/**
	 * @return Returns the referrer.
	 */
	public String getReferrer() {
		return referrer;
	}


	/**
	 * @param referrer The referrer to set.
	 */
	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}


	/**
	 * @return Returns the selectedOrganization.
	 */
	public String getSelectedOrganization() {
		return selectedOrganization;
	}


	/**
	 * @param selectedOrganization The selectedOrganization to set.
	 */
	public void setSelectedOrganization(String selectedOrganization) {
		this.selectedOrganization = selectedOrganization;
	}


	/**
	 * @return Returns the state.
	 */
	public String getState() {
		return state;
	}


	/**
	 * @param state The state to set.
	 */
	public void setState(String state) {
		this.state = state;
	}


	/**
	 * @return Returns the stateOtherCountryText.
	 */
	public String getStateOtherCountryText() {
		return stateOtherCountryText;
	}


	/**
	 * @param stateOtherCountryText The stateOtherCountryText to set.
	 */
	public void setStateOtherCountryText(String stateOtherCountryText) {
		this.stateOtherCountryText = stateOtherCountryText;
	}


	/**
	 * @return Returns the street1.
	 */
	public String getStreet1() {
		return street1;
	}


	/**
	 * @param street1 The street1 to set.
	 */
	public void setStreet1(String street1) {
		this.street1 = street1;
	}


	/**
	 * @return Returns the street2.
	 */
	public String getStreet2() {
		return street2;
	}


	/**
	 * @param street2 The street2 to set.
	 */
	public void setStreet2(String street2) {
		this.street2 = street2;
	}


	/**
	 * @return Returns the suffix.
	 */
	public String getSuffix() {
		return suffix;
	}


	/**
	 * @param suffix The suffix to set.
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}


	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return title;
	}


	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}


	/**
	 * @return Returns the typeAddress.
	 */
	public String getTypeAddress() {
		return typeAddress;
	}


	/**
	 * @param typeAddress The typeAddress to set.
	 */
	public void setTypeAddress(String typeAddress) {
		this.typeAddress = typeAddress;
	}


	/**
	 * @return Returns the userId.
	 */
	public String getUserId() {
		return userId;
	}


	/**
	 * @param userId The userId to set.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}


	/**
	 * @return Returns the userProxyInode.
	 */
	public String getUserProxyInode() {
		return userProxyInode;
	}


	/**
	 * @param userProxyInode The userProxyInode to set.
	 */
	public void setUserProxyInode(String userProxyInode) {
		this.userProxyInode = userProxyInode;
	}


	/**
	 * @return Returns the verifyPassword.
	 */
	public String getVerifyPassword() {
		return verifyPassword;
	}


	/**
	 * @param verifyPassword The verifyPassword to set.
	 */
	public void setVerifyPassword(String verifyPassword) {
		this.verifyPassword = verifyPassword;
	}


	/**
	 * @return Returns the zip.
	 */
	public String getZip() {
		return zip;
	}


	/**
	 * @param zip The zip to set.
	 */
	public void setZip(String zip) {
		this.zip = zip;
	}

	/**
	 * @param tags The tags to set.
	 */
	public void setTags(String tags) {
		this.tags = tags;
	}

	/**
	 * @return Returns the tags.
	 */
	public String getTags() {
		return tags;
	}
}