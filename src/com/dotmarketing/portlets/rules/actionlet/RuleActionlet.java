package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;

public abstract class RuleActionlet implements Serializable {

    private static final long serialVersionUID = -6721673381070066205L;

    private final String id;
    private final String name;

    public RuleActionlet(String name) {
        this.name = name;
        this.id = this.getClass().getSimpleName();
    }

    /**
     * The unique type id for this Actionlet implementation.
     *
     * @return a unique id for this Actionlet type
     */
    @NotNull
    public final String getId() {
        return this.id;
    }

    /**
	 * Returns the human readable name for this Actionlet
	 */
	public final String getName(){
        return this.name;
    }

    public String getI18nKey(){
        return "ruleengine.actionlet." + getId();
    }

	/**
	 * returns the list of parameters that are accepted by the implementing actionlet
	 * @return
	 */
//	public abstract List<WorkflowActionletParameter> getParameters();

	/**
	 * This method looks for the name in the language.properties
	 * file using property "ruleengine.actionlet.{id}.name" If that is not there it will return the value
	 * set in the getName() method.
	 */
	public String getLocalizedName() {
        Optional<String> val = Optional.empty();
        try {
			String key = getI18nKey() + ".name";
			val = LanguageUtil.getOpt(PublicCompanyFactory.getDefaultCompanyId(),
                                      PublicCompanyFactory.getDefaultCompany().getLocale(),
                                      key);
		} catch (LanguageException e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
		return val.orElse(getName());
	}

	/**
	 * if this is set, the all subsequent actionlets will not be fired.  This is true when executing both the
	 * preactions and the postactions
	 */
	public boolean stopProcessing(){
		return false;
	}



	/**
	 * Returns the human readable instructions for this Actionlet
	 */
	public abstract String getHowTo();

	/**
	 * Action that gets executed when the owner {@link com.dotmarketing.portlets.rules.conditionlet.Conditionlet} evaluates to true
	 */
	public abstract void executeAction(HttpServletRequest request, Map<String, RuleActionParameter> params);

    @Override
    public boolean equals(Object o) {
        if(this == o) { return true; }
        if(!(o instanceof RuleActionlet)) { return false; }
        RuleActionlet that = (RuleActionlet)o;
        return Objects.equal(getId(), that.getId()) &&
               Objects.equal(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getName());
    }
}
