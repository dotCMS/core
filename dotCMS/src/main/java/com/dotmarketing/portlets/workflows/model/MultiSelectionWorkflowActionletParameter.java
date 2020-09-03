package com.dotmarketing.portlets.workflows.model;

import java.util.Collection;

/**
 * Defines an actionlet parameter that allows to select the value from a collection
 * @param <T>
 */
public class MultiSelectionWorkflowActionletParameter extends WorkflowActionletParameter {

    private final MultiValueProvider multiValueProvider;


    public MultiSelectionWorkflowActionletParameter(final String key,
                                                    final String displayName,
                                                    final String defaultValue,
                                                    final boolean isRequired,
                                                    final MultiValueProvider multiValueProvider) {

        super(key, displayName, defaultValue, isRequired);
        this.multiValueProvider = multiValueProvider;
    }

    public Collection<MultiKeyValue> getMultiValues() {

        return multiValueProvider.values();
    }
}
