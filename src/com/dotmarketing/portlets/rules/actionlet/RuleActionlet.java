package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public abstract class RuleActionlet implements Serializable {

    private static final long serialVersionUID = -6721673381070066205L;

	/**
	 * returns the list of parameters that are accepted by the implementing actionlet
	 * @return
	 */
//	public abstract List<WorkflowActionletParameter> getParameters();

	/**
	 * This method looks for the name in the language.properties
	 * file using property "com.my.classname.name" If that is not there it will return the value
	 * set in the getName() method.
	 * @return
	 */
	public String getLocalizedName() {
 		String val = null;
		try {
			String key = this.getClass().getCanonicalName() + ".name";
			val = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(), key);
			if (val != null &&! key.equals(val)) {
				return val;
			}
		} catch (LanguageException e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
		return getName();
	}
	/**
	 * This method looks for the how to instructions in the language.properties
	 * file using property "com.my.classname.howTo" If that is not there it will return the value
	 * set in the getHowTo() method.
	 * @return
	 */
	public String getLocalizedHowto() {
		String val = null;
		try {
			String key = this.getClass().getCanonicalName() + ".howTo";
			val = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(), key);
			if (val != null || !key.equals(val)) {
				return val;
			}
		} catch (LanguageException e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
		return getHowTo();
	}

	/**
	 * if this is set, the all subsequent actionlets will not be fired.  This is true when executing both the
	 * preactions and the postactions
	 * @return
	 */
	public boolean stopProcessing(){
		return false;
	}


	/**
	 * Returns the human readable name for this Actionlet
	 * @return
	 */
	public abstract String getName();
	/**
	 * Returns the human readable instructions for this Actionlet
	 * @return
	 */
	public abstract String getHowTo();

	/**
	 * Action that gets executed when the owner {@link com.dotmarketing.portlets.rules.conditionlet.Conditionlet} evaluates to true
	 * @param params
	 * @throws
	 */
	public abstract void executeAction(HttpServletRequest request, Map<String, RuleActionParameter> params);


	@Override
	public boolean equals(Object obj) {
		if(obj instanceof RuleActionlet){
			if(obj != null){
				return getClass().equals(obj.getClass());
			}
		}
		return false;
	}


}
