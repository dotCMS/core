package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.io.Serializable;

public abstract class Conditionlet implements Serializable {

    private static final long serialVersionUID = -8179010054316951177L;

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
     * Returns the human readable name for this Conditionlet
     * @return
     */
    public abstract String getName();
}
