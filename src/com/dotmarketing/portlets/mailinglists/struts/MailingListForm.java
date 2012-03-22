package com.dotmarketing.portlets.mailinglists.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.util.Constants;

/** @author Hibernate CodeGenerator */
public class MailingListForm extends ValidatorForm {

	private static final long serialVersionUID = 1L;

	/*** WEB ASSET FIELDS FOR THE FORM ***/
    /** nullable persistent field */
    private String title;
    private boolean publicList;
    private String newSubscribers;
    private boolean ignoreHeaders;
    private int subscriberCount;
    private String inode;
    
    //Fields to add a single subscriber
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phone;

    public MailingListForm() {
    }

   
	/**
	 * Returns the publicList.
	 * @return boolean
	 */
	public boolean isPublicList() {
		return publicList;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the publicList.
	 * @param publicList The publicList to set
	 */
	public void setPublicList(boolean publicList) {
		this.publicList = publicList;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the newSubscribers.
	 * @return String
	 */
	public String getNewSubscribers() {
		return newSubscribers;
	}

	/**
	 * Sets the newSubscribers.
	 * @param newSubscribers The newSubscribers to set
	 */
	public void setNewSubscribers(String newSubscribers) {
		this.newSubscribers = newSubscribers;
	}

	/**
	 * Returns the subscriberCount.
	 * @return int
	 */
	public int getSubscriberCount() {
		return subscriberCount;
	}

	/**
	 * Sets the subscriberCount.
	 * @param subscriberCount The subscriberCount to set
	 */
	public void setSubscriberCount(int subscriberCount) {
		this.subscriberCount = subscriberCount;
	}

	/**
	 * Returns the inode.
	 * @return String
	 */
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}

	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
            return super.validate(mapping, request);
        } else  if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.UPDATE)) {
            ActionErrors errors = new ActionErrors ();
            if (email.equals("")) {
            	ActionMessage error = new ActionMessage ("prompt.mailingListEmailRequired");
                errors.add("email", error);
            }
            if (firstName.equals("")) {
            	ActionMessage error = new ActionMessage ("prompt.mailingListFirstName");
                errors.add("firstName", error);
            }
            if (lastName.equals("")) {
            	ActionMessage error = new ActionMessage ("prompt.mailingListLastName");
                errors.add("lastName", error);
            }
            if (errors.size() == 0)
                return super.validate(mapping, request);
            return errors;
        }
        return null;
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
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getMiddleName() {
        return middleName;
    }
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
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
    public String getZip() {
        return zip;
    }
    public void setZip(String zip) {
        this.zip = zip;
    }
    
	/**
	 * @return Returns the ignoreHeaders.
	 */
	public boolean isIgnoreHeaders() {
		return ignoreHeaders;
	}
	/**
	 * @param ignoreHeaders The ignoreHeaders to set.
	 */
	public void setIgnoreHeaders(boolean ignoreHeaders) {
		this.ignoreHeaders = ignoreHeaders;
	}
}
