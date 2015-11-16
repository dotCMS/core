package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class RuleActionlet implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String i18nKey;
    private final List<ActionParameterDefinition> parameters;

    public RuleActionlet(String i18nKey) {
        this(i18nKey, new ArrayList<>());
    }

    public RuleActionlet(String i18nKey, List<ActionParameterDefinition> parameterDefinitions) {
        this.i18nKey = i18nKey;
        this.id = this.getClass().getSimpleName();
        this.parameters = parameterDefinitions;
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

    public String getI18nKey(){
        return this.i18nKey;
    }

    public List<ActionParameterDefinition> getParameters(){
    	return parameters;
    }

	/**
	 * This method is called if the owning Rule's conditions {@link com.dotmarketing.portlets.rules.conditionlet.Conditionlet} evaluate to true.
     * Note that for performance reasons you should typically assume that the provided parameters are valid, rather than calling the
     * {@link #getI18nKey()} method again.
	 */
	public abstract void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params);

    /**
     * Override this method to provide validations beyond that which the RuleActionParameter's are capable of on their own. For example,
     * to validate a case where the sum of two parameters cannot exceed a threshold.
     */
	public void validateActionInstance(RuleAction actionInstance) throws InvalidActionInstanceException {};


	public final void validateActionInstanceInternal(RuleAction actionInstance) throws InvalidActionInstanceException {

        try {
            /* Gives the subclasses a chance to do validations that require checking multiple params against each other. */
            this.validateActionInstance(actionInstance);
        } catch( InvalidActionInstanceException e){
            throw e; // rethrow as is, no need to double wrap.
        }
        catch (Exception e) {
            throw new InvalidActionInstanceException(e, e.getMessage());
        }
    };

    @Override
    public boolean equals(Object o) {
        if(this == o) { return true; }
        if(!(o instanceof RuleActionlet)) { return false; }
        RuleActionlet that = (RuleActionlet)o;
        return Objects.equal(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
