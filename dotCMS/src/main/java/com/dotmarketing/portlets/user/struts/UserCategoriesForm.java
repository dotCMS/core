package com.dotmarketing.portlets.user.struts;

import org.apache.struts.validator.ValidatorForm;

public class UserCategoriesForm extends ValidatorForm {

	private static final long serialVersionUID = 1L;
	
	private String userProxy;
	private boolean nonclicktracking;
	
	private String[] categories;

	
	
	public boolean isNonclicktracking() {
		return nonclicktracking;
	}

	public void setNonclicktracking(boolean nonclicktracking) {
		this.nonclicktracking = nonclicktracking;
	}

	public String[] getCategories() {
		return categories;
	}

	public void setCategories(String[] categories) {
		this.categories = categories;
	}

	public String getUserProxy() {
		return userProxy;
	}

	public void setUserProxy(String userProxy) {
		this.userProxy = userProxy;
	}
}
