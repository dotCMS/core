package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ComparisonOption;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geoff M. Granum
 */
public class ComparisonParameterDefinition extends ParameterDefinition<TextType> {

    private final Map<String, Comparison> comparisons;

    public ComparisonParameterDefinition(int uiIndex, Comparison... comparisons) {
        super(uiIndex, "comparison", dropdownInput(comparisons).minSelections(1), defaultComparison(comparisons));
        HashMap<String, Comparison> map = Maps.newHashMap();
        for (Comparison comparison : comparisons) {
            map.put(comparison.getId(), comparison);
        }
        this.comparisons = ImmutableMap.copyOf(map);
    }

    private static String defaultComparison(Comparison... comparisons) {
        Preconditions.checkNotNull(comparisons, "At least one comparison is required.");
        Preconditions.checkArgument(comparisons.length > 0, "At least one comparison is required.");
        return comparisons[0].getId();
    }

    private static DropdownInput dropdownInput(Comparison[] comparisons) {
        DropdownInput input = new DropdownInput();
        for (Comparison comparison : comparisons) {
            ComparisonOption option = new ComparisonOption(comparison.getId(), comparison.getRightHandArgCount());
            input.option(option);
        }
        return input;
    }

    public Comparison comparisonFrom(String id) {
        Comparison comparison = comparisons.get(id);
        if(comparison == null) {
            throw new ComparisonNotPresentException(id);
        }
        return comparison;
    }
}
 
