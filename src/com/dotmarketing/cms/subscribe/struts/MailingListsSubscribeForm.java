package com.dotmarketing.cms.subscribe.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.UtilMethods;

public class MailingListsSubscribeForm extends ValidatorForm {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String name;
    private String lastName;
    private String emailAddress;
    private String[] mailingListsInodes;
    private boolean unsubscribeFromAll;

    /** default constructor */
    public MailingListsSubscribeForm() {
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return null;
    }

    public String[] getMailingListsInodes() {
        return mailingListsInodes;
    }
    public void setMailingListsInodes(String[] mailingListsInodes) {
        this.mailingListsInodes = mailingListsInodes;
    }
    public boolean isUnsubscribeFromAll() {
        return unsubscribeFromAll;
    }
    public void setUnsubscribeFromAll(boolean unsubscribeFromAll) {
        this.unsubscribeFromAll = unsubscribeFromAll;
    }
    public String getEmailAddress() {
        return emailAddress;
    }
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String firstName) {
        this.name = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

	public ActionErrors validateCreateAndSubscribe(ActionMapping mapping, HttpServletRequest request) {
		
		ActionErrors  errors = new ActionErrors();
		if(!UtilMethods.isSet(this.emailAddress)) {
			errors.add(Globals.ERROR_KEY, new ActionMessage("errors.required", "e-mail"));
		} else if(!UtilMethods.isValidEmail(this.emailAddress)) {
			errors.add(Globals.ERROR_KEY, new ActionMessage("errors.email", this.emailAddress));
		}
		
		return errors;
	}
    
}