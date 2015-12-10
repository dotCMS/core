package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
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
        super(uiIndex, "comparison", dropdownInput(comparisons).minSelections(1));
        HashMap<String, Comparison> map = Maps.newHashMap();
        for (Comparison comparison : comparisons) {
            map.put(comparison.getId(), comparison);
        }
        this.comparisons = ImmutableMap.copyOf(map);
    }

    private static DropdownInput dropdownInput(Comparison[] comparisons) {
        DropdownInput input = new DropdownInput();
        for (Comparison comparison : comparisons) {
            input.option(comparison.getId());
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
 
