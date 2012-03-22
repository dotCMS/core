package com.dotmarketing.portlets.workflows.actionlet;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

public abstract class WorkFlowActionlet implements Serializable {

	/**
	 * 
	 * @return
	 */
	private static final long serialVersionUID = -3399186955215452961L;
	
	/**
	 * returns the list of parameters that are accepted by the implementing actionlet
	 * @return
	 */
	public abstract List<WorkflowActionletParameter> getParameters();

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
	 * This method fires before a piece of content is checked in.
	 * @param processor
	 * @param params
	 * @throws WorkflowActionFailureException
	 * @throws DotContentletValidationException
	 */
	public void executePreAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
			throws WorkflowActionFailureException, DotContentletValidationException {

	}

	/**
	 * This method fires after a piece of content is checked in
	 * @param processor
	 * @param params
	 * @throws WorkflowActionFailureException
	 */
	public abstract void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
			throws WorkflowActionFailureException;


	@Override
	public boolean equals(Object obj) {
		if(obj instanceof WorkFlowActionlet){
			if(obj != null){
				return getClass().equals(obj.getClass());
			}
		}
		return false;
	}


}
