package com.dotmarketing.cms.login.struts;


import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.liferay.portal.util.Constants;

public class LoginForm extends ValidatorForm {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    String userName;
    String password;
    String emailAddress;
    String challengeQuestionAnswer;
    String dispatch;
    boolean rememberMe;


	/** default constructor */
    public LoginForm() {
    }

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {

            //return super.validate(mapping, request);
        }
        return null;
    }

	public ActionErrors subValidate(ActionMapping mapping, HttpServletRequest request) {
	    return super.validate(mapping, request);
    }

    /**
     * @return Returns the rememberMe.
     */
    public boolean isRememberMe() {
        return this.rememberMe;
    }
    /**
     * @param rememberMe The rememberMe to set.
     */
    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
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
     * @return Returns the password.
     */
    public String getPassword() {
        return this.password;
    }
    /**
     * @param password The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * @return Returns the userName.
     */
    public String getUserName() {
        return this.userName;
    }
    /**
     * @param userName The userName to set.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }
    /**
     * @return Returns the challengeQuestionAnswer.
     */
	public String getChallengeQuestionAnswer() {
		return challengeQuestionAnswer;
	}
	/**
     * @param challengeQuestionAnswer The challengeQuestionAnswer to set.
     */
	public void setChallengeQuestionAnswer(String challengeQuestionAnswer) {
		this.challengeQuestionAnswer = challengeQuestionAnswer;
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
}