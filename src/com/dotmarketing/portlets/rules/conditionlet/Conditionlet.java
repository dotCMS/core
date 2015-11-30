package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.portlets.rules.RuleComponentDefinition;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.exception.RuleConstructionFailedException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.util.Logger;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Conditionlet<T extends RuleComponentInstance> extends RuleComponentDefinition {

    private static final long serialVersionUID = 1L;
    private final Set<Comparison> comparisons;

    protected Conditionlet(String i18nKey, Set<Comparison> comparisons) {
        super(i18nKey);
        this.comparisons = ImmutableSet.copyOf(comparisons);
    }

    public Set<Comparison> getComparisons() {
        return comparisons;
    }

    /**
     * Returns a {@link ConditionletInput} containing all the information and/or data needed to build the input for a Condition,
     * determined by the given operator
     *
     * @param comparisonId the id of the selected comparison in the condition
     */
    public abstract Collection<ConditionletInput> getInputs(String comparisonId);

    /**
     * Traverses the list of {@link Comparison} criteria and returns the one
     * associated to the specified ID.
     *
     * @param id - The {@link Comparison} ID.
     *
     * @return The {@link Comparison} object.
     *
     * @throws IllegalArgumentException If the comparison ID is not found or the list of comparisons
     *                                  is empty.
     */
    protected Comparison getComparisonById(String id) {
        Comparison comparison = null;
        for (Comparison c : getComparisons()) {
            if(c.getId().equals(id)) {
                comparison = c;
                break;
            }
        }
        if(comparison == null) {
            Logger.error(this, "Comparison ID not found.");
            throw new IllegalArgumentException("Comparison ID not found.");
        }
        return comparison;
    }

    public final boolean evaluate(HttpServletRequest request, HttpServletResponse response, Condition model) {
        T instance;
        try {
            instance = instanceFrom(model);
        } catch (RuleEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleConstructionFailedException(e, "Could not create Conditionlet instance of type %s from provided model %s.",
                                                      this.getId(), model.toString());
        }
        try {
            return this.evaluate(request, response, instance);
        } catch (RuleEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate Condition from model: " + model.toString());
        }
    }

    public abstract boolean evaluate(HttpServletRequest request, HttpServletResponse response, T instance);

    public T instanceFrom(Condition model) {
        return this.instanceFrom(Comparison.get(model.getComparison()), model.getValues());
    }

    public abstract T instanceFrom(Comparison comparison, List<ParameterModel> values);
}
