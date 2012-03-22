package com.dotmarketing.cms.forgotpassword.struts;


import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.UtilMethods;

public class ForgotPasswordForm extends ValidatorForm {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String email;
    private String dispatch;
    private String accKey;
    private String newPassword;
    private String verifyPassword;
    

	public String getAccKey() {
		return accKey;
	}

	public void setAccKey(String accKey) {
		this.accKey = accKey;
	}

	/** default constructor */
    public ForgotPasswordForm() {
    }
	
    /**
     * @return Returns the dispatch.
     */
    public String getDispatch() {
        return this.dispatch;
    }
    /**
     * @param dispatch The dispatch to set.
     */
    public void setDispatch(String dispatch) {
        this.dispatch = dispatch;
    }

    /**
     * @return Returns the email.
     */
    public String getEmail() {
        return this.email;
    }
    /**
     * @param email The email to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getVerifyPassword() {
		return verifyPassword;
	}

	public void setVerifyPassword(String verifyPassword) {
		this.verifyPassword = verifyPassword;
	}

	@Override
	public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
		ActionErrors errors = super.validate(arg0, arg1);
		if(getDispatch().equals("forgotPassword")) {
			if(!UtilMethods.isSet(email)) {
				errors.add(Globals.ERROR_KEY, new ActionMessage("prompt.email"));
			}
		}
		if(getDispatch().equals("resetPassword")) {
			if(!UtilMethods.isSet(newPassword)) {
				errors.add(Globals.ERROR_KEY, new ActionMessage("error.forgot.password.new.password.required"));
			} else if(!newPassword.equals(verifyPassword)) {
				errors.add(Globals.ERROR_KEY, new ActionMessage("error.forgot.password.passwords.dont.match"));
			}
		}
		return errors;
	}
    
    
}
