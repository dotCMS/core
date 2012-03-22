package com.dotmarketing.portlets.webforms.struts;

import java.io.Serializable;

public class WebForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String webFormId;

    private String formType;

    private String title;
    
    private String prefix;
    
    private String firstName;
    
    private String middleInitial;
    
    private String middleName;
    
    private String lastName;
    
    private String fullName;
    
    private String organization;
    
    private String address;
    
    private String address1;
    
    private String address2;
    
    private String city;

    private String state;

    private String zip;
    
    private String country;

    private String phone;
    
    private String email;
    
    private String customFields;

    private String userInode;
	private String categories;
   
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(String middleInitial) {
        this.middleInitial = middleInitial;
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

    public String getWebFormId() {
        return webFormId;
    }

    public void setWebFormId(String webFormId) {
        this.webFormId = webFormId;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }
    
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof WebForm) {
            WebForm form = (WebForm) arg0;
            return form.webFormId.equalsIgnoreCase(this.webFormId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return  this.webFormId.hashCode();
    }
    
	/**
	 * @return the userInode
	 */
	public String getUserInode() {
		return userInode;
	}

	/**
	 * @param userInode the userInode to set
	 */
	public void setUserInode(String userInode) {
		this.userInode = userInode;
	}
    
    /**
	 * @return the categories
	 */
	public String getCategories() {
		return categories;
	}

	/**
	 * @param categories the categories to set
	 */
	public void setCategories(String categories) {
		this.categories = categories;
	}
}
