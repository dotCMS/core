package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.portlets.chains.model.ChainLinkCode;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Conditionlet implements Serializable {

    private static final long serialVersionUID = -8179010054316951177L;

    private String languageId;

    /**
     * This method looks for the name in the language.properties
     * file using property "com.my.classname.name" If that is not there it will return the value
     * set in the getName() method.
     *
     * @return the name in the language.properties, if exists, value of getName() if not.
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
     * Returns the human readable name for this Conditionlet
     *
     * @return the name of this Conditionlet
     */
    protected abstract String getName();

    /**
     * Returns a Map object whoose keys are the operators' names and values are the operators' labels (for presentation)
     * @return a Map of operators' names and labels
     */
    public abstract Set<Operator> getOperators();


    /**
     * Performs a validation to the given value(s), determined by the given operator
     * @param operator the name of the selected operator in the condition
     * @param values the given values in the condition
     * @return the result of whether the given value is valid or not, determined by the given operator
     */
    public abstract ValidationResults validate(Operator operator, Collection<String> values);

    protected abstract ValidationResult validate(Operator operator, String value);

    /**
     * Returns a {@link ConditionletInput} containing all the information and/or data needed to build the input for a Condition,
     * determined by the given operator
     * @param operator the name of the selected operator in the condition
     * @return
     */
    public abstract ConditionletInput getInput(Operator operator);

    public abstract boolean evaluate(Operator operator, HttpServletRequest request);

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }
}
