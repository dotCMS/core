package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.Logger;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Conditionlet implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String i18nKey;

    protected Conditionlet(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    /**
     * The unique id for this Conditionlet implementation.
     *
     * @return a unique id for this Conditionlet
     */
    @NotNull
    public final String getId() {
        return this.getClass().getSimpleName();

    }

    /**
     * Returns the human readable name for this Conditionlet
     *
     * @return the name of this Conditionlet
     */
    @NotNull
    public final String getI18nKey() {
        return this.i18nKey;
    }

    /**
     * Returns a Map object whoose keys are the operators' names and values are the operators' labels (for presentation)
     * @return a Map of operators' names and labels
     */
    public abstract Set<Comparison> getComparisons();


    /**
     * Performs a validation to the given inputValues(s), determined by the given operator
     * @param comparison the selected comparison in the condition
     * @param inputValues the given values in the condition
     * @return the result of whether the given values are valid or not, determined by the given operator
     */
    public abstract ValidationResults validate(Comparison comparison, Set<ConditionletInputValue> inputValues);

    protected abstract ValidationResult validate(Comparison comparison, ConditionletInputValue inputValue);

    /**
     * Returns a {@link ConditionletInput} containing all the information and/or data needed to build the input for a Condition,
     * determined by the given operator
     * @param comparisonId the id of the selected comparison in the condition
     * @return
     */
    public abstract Collection<ConditionletInput> getInputs(String comparisonId);

    public abstract boolean evaluate(HttpServletRequest request, HttpServletResponse response, String comparisonId, List<ConditionValue> values);


	/**
	 * Traverses the list of {@link Comparison} criteria and returns the one
	 * associated to the specified ID.
	 * 
	 * @param id
	 *            - The {@link Comparison} ID.
	 * @return The {@link Comparison} object.
	 * @throws IllegalArgumentException
	 *             If the comparison ID is not found or the list of comparisons
	 *             is empty.
	 */
	protected Comparison getComparisonById(String id) {
		Comparison comparison = null;
		for (Comparison c : getComparisons()) {
			if (c.getId().equals(id)) {
				comparison = c;
				break;
			}
		}
		if (comparison == null) {
			Logger.error(this, "Comparison ID not found.");
			throw new IllegalArgumentException("Comparison ID not found.");
		}
		return comparison;
	}

}
