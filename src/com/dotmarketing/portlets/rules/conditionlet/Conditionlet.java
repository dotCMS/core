package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;

public abstract class Conditionlet implements Serializable {

    private static final long serialVersionUID = -8179010054316951177L;

    /**
     * This method looks for the name in the language.properties
     * file using property "com.my.classname.name" If that is not there it will return the value
     * set in the getName() method.
     *
     * @return the name in the language.properties, if exists, value of getName() if not.
     */
    public String getLocalizedName() {
            String key = this.getClass().getCanonicalName() + ".name";
            String val = getLabel(key);

            if(val==null || val.equals(key))
                return getName();

            return val;
    }

    /**
     * Returns the human readable name for this Conditionlet
     *
     * @return the name of this Conditionlet
     */
    public abstract String getName();

    /**
     * Returns a Map object whoose keys are the operators' names and values are the operators' labels (for presentation)
     * @return a Map of operators' names and labels
     */
    public abstract LinkedHashMap<String, String> getOperators();


    /**
     * Performs a validation to the given value, determined by the given operator
     * @param operator the name of the selected operator in the condition
     * @param value the given value in the condition
     * @return the result of whether the given value is valid or not, determined by the given operator
     */
    public abstract boolean validate(String operator, String value);

    /**
     * Returns a {@link ConditionletInput} containing all the information and/or data needed to build the input for a Condition,
     * determined by the given operator
     * @param operator the name of the selected operator in the condition
     * @return
     */
    public abstract ConditionletInput getInput(String operator);

    protected String getLabel(String key) {
        try {
            return LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(), key);
        } catch (LanguageException e) {
            Logger.error(this.getClass(), "Could not get Language value for key: " + key, e);
        }

        return null;
    }

    public abstract boolean evaluate(String leftArgument, String operator, String rightArgument);

}
