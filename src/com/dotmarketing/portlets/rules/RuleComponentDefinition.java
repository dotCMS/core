package com.dotmarketing.portlets.rules;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author Geoff M. Granum
 */
public abstract class RuleComponentDefinition implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final String id;
    protected final String i18nKey;

    public RuleComponentDefinition(String i18nKey) {
        this.id = this.getClass().getSimpleName();
        this.i18nKey = i18nKey;
    }

    /**
     * The unique type id for this Actionlet implementation.
     *
     * @return a unique id for this Actionlet type
     */
    @NotNull
    public final String getId() {
        return this.getClass().getSimpleName();
    }

    public String getI18nKey(){
        return this.i18nKey;
    }
}
 
