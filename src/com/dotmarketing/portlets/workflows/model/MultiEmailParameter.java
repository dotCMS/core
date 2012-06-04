package com.dotmarketing.portlets.workflows.model;

import java.util.StringTokenizer;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;


public class MultiEmailParameter extends WorkflowActionletParameter {

	public MultiEmailParameter(String key, String displayName,
			String defaultValue, boolean isRequired) {
		super(key, displayName, defaultValue, isRequired);
	}

	public String hasError(String stringToValidate) {
		String results = null;		
		StringBuffer uIdsEmails = new StringBuffer();		
		if((stringToValidate != null) || (stringToValidate != "")){
			StringTokenizer st = new StringTokenizer(stringToValidate, ", ");						
			while (st.hasMoreTokens()) {
				String x = st.nextToken();
				if (Validator.isEmailAddress(x)) {
					try {
						User u = APILocator.getUserAPI().loadByUserByEmail(x, APILocator.getUserAPI().getSystemUser(), false);
						
					} catch (Exception e) {
						Logger.error(this.getClass(), "Unable to find user with email:" + x);										
						uIdsEmails.append("Unable to find user with email:"+ x +"</br>");						
					}
				} else {
					try {
						User u = APILocator.getUserAPI().loadUserById(x, APILocator.getUserAPI().getSystemUser(), false);
						
					} catch (Exception e) {						
						Logger.error(this.getClass(), "Unable to find user with userID:" + x);
						uIdsEmails.append("Unable to find user with userID:" + x +"</br>");						
					}
				}
			}				
		}
		if(UtilMethods.isSet(uIdsEmails.toString())){
			return uIdsEmails.toString();
		}else return results;
	}
}
