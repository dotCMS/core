package com.dotmarketing.cms.registration.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.EmailValidator;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import com.dotmarketing.util.UtilMethods;

public class RegistrationForm extends ActionForm
{
	private static final long serialVersionUID = 1L;
	
	//Liferay User fields
	private String userID;
	private String emailAddress;
	private String password;
	private String verifyPassword;
	private boolean passChanged = false;
	private String firstName;
	private String lastName;
	
	//Extender user fields
	private String extUserInode;
	private String prefix;
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
	private String zip;
	private String phone;
	private String fax;
	private String country;
	
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
	
	//From parameter
	private String from;
	
	//Event Registration fields
	private String findMeEmailAddress;
	private String findMePassword;
	private long howDidYouHear;
	private String ceoName;
	
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
		this.emailAddress = emailAddress;
	}
	public String getFax() {
		return fax;
	}
	public void setFax(String fax) {
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
		this.phone = phone;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
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
		this.suffix = suffix;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
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

	public String getSelectedOrganization() {
		return selectedOrganization;
	}
	public void setSelectedOrganization(String selectedOrganization) {
		this.selectedOrganization = selectedOrganization;
	}	
	
	public String getOrganizationCity() {
		return organizationCity;
	}
	public void setOrganizationCity(String organizationCity) {
		this.organizationCity = organizationCity;
	}
	public String getOrganizationCountry() {
		return organizationCountry;
	}
	public void setOrganizationCountry(String organizationCountry) {
		this.organizationCountry = organizationCountry;
	}
	public String getOrganizationFax() {
		return organizationFax;
	}
	public void setOrganizationFax(String organizationFax) {
		this.organizationFax = organizationFax;
	}
	public String getOrganizationInodeAux() {
		return organizationInodeAux;
	}
	public void setOrganizationInodeAux(String organizationInodeAux) {
		this.organizationInodeAux = organizationInodeAux;
	}
	public String getOrganizationPhone() {
		return organizationPhone;
	}
	public void setOrganizationPhone(String organizationPhone) {
		this.organizationPhone = organizationPhone;
	}
	public String getOrganizationState() {
		return organizationState;
	}
	public void setOrganizationState(String organizationState) {
		this.organizationState = organizationState;
	}
	public String getOrganizationStreet1() {
		return organizationStreet1;
	}
	public void setOrganizationStreet1(String organizationStreet1) {
		this.organizationStreet1 = organizationStreet1;
	}
	public String getOrganizationStreet2() {
		return organizationStreet2;
	}
	public void setOrganizationStreet2(String organizationStreet2) {
		this.organizationStreet2 = organizationStreet2;
	}
	public String getOrganizationTitle() {
		return organizationTitle;
	}
	public void setOrganizationTitle(String organizationTitle) {
		this.organizationTitle = organizationTitle;
	}
	public String getOrganizationZip() {
		return organizationZip;
	}
	public void setOrganizationZip(String organizationZip) {
		this.organizationZip = organizationZip;
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
	public String getExtUserInode() {
		return extUserInode;
	}
	public void setExtUserInode(String extUserInode) {
		this.extUserInode = extUserInode;
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
	
	public boolean isNoOrganization() {
		return noOrganization;
	}
	public void setNoOrganization(boolean noOrganization) {
		this.noOrganization = noOrganization;
	}
	public String getReferrer() {
		return referrer;
	}
	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}
	
	
	public ActionErrors validateOrganization()
	{
		ActionErrors ae = new ActionErrors();
		if (!UtilMethods.isSet(organizationTitle))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Organization Name"));
		}
		if (!UtilMethods.isSet(organizationStreet1))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Street1"));
		}		
		if (!UtilMethods.isSet(organizationCity))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","City"));
		}
		if (!UtilMethods.isSet(organizationState))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","State"));
		}
		if (!UtilMethods.isSet(organizationZip))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Zip"));
		}
		if (!UtilMethods.isSet(organizationPhone))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Phone"));
		}
		return ae;
	}
	
	public ActionErrors validateRegistry(HttpServletRequest request)
	{
		ActionErrors ae = new ActionErrors();
		if (!UtilMethods.isSet(emailAddress))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Email"));
		}
		else
		{				
			EmailValidator emailvalidator = EmailValidator.getInstance();
			boolean isValid = emailvalidator.isValid(emailAddress);			
			if (!isValid) 
			{			
				ae.add(Globals.ERROR_KEY, new ActionMessage("error.form.format", "Email"));
			}					
		}
		
		if (!UtilMethods.isSet(password))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Password"));
		}
		if (!UtilMethods.isSet(verifyPassword))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Verify Password"));
		}
		if (passChanged && UtilMethods.isSet(password) && UtilMethods.isSet(verifyPassword) && !password.equals(verifyPassword))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.verifyPassword"));
		}
		
		if (!UtilMethods.isSet(firstName))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","First Name"));
		}
		if (!UtilMethods.isSet(lastName))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Last Name"));
		}
		if (!UtilMethods.isSet(description))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Address Type"));
		}
		if (!UtilMethods.isSet(street1))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Street 1"));
		}
		if (!UtilMethods.isSet(city))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","City"));
		}
		if (!UtilMethods.isSet(state))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","State"));
		}
		if (!UtilMethods.isSet(zip))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Zip"));
		}
		if (!UtilMethods.isSet(phone))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Phone"));
		}
		if (!UtilMethods.isSet(country))
		{
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.form.mandatory","Country"));
		}
		
		return ae;
	}
	
	
	@Override
	public void reset(ActionMapping arg0, HttpServletRequest arg1) {
		super.reset(arg0, arg1);
		this.noOrganization = false;
	}
	
	//Event Regristration Parameters
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public long getHowDidYouHear() {
		return howDidYouHear;
	}
	public void setHowDidYouHear(long howDidYouHear) {
		this.howDidYouHear = howDidYouHear;
	}
	public String getCeoName() {
		return ceoName;
	}
	public void setCeoName(String ceoName) {
		this.ceoName = ceoName;
	}
	public String getFindMeEmailAddress() {
		return findMeEmailAddress;
	}
	public void setFindMeEmailAddress(String findMeEmailAddress) {
		this.findMeEmailAddress = findMeEmailAddress;
	}
	public String getFindMePassword() {
		return findMePassword;
	}
	public void setFindMePassword(String findMePassword) {
		this.findMePassword = findMePassword;
	}
	public boolean isPassChanged() {
		return passChanged;
	}
	public void setPassChanged(boolean passChanged) {
		this.passChanged = passChanged;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	
	
}

