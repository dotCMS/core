package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotmarketing.portlets.rules.RuleComponentDefinition;
import com.dotmarketing.portlets.rules.exception.InvalidActionInstanceException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class RuleActionlet extends RuleComponentDefinition {

    private static final long serialVersionUID = 1L;

    public RuleActionlet(String i18nKey, ParameterDefinition... parameterDefinitions) {
        super(i18nKey, parameterDefinitions);
    }


	/**
	 * This method is called if the owning Rule's conditions {@link com.dotmarketing.portlets.rules.conditionlet.Conditionlet} evaluate to true.
     * Note that for performance reasons you should typically assume that the provided parameters are valid, rather than calling the
     * {@link #getI18nKey()} method again.
	 */
	public abstract void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, ParameterModel> params);

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
