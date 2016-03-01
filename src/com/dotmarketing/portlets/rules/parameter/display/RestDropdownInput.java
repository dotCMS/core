package com.dotmarketing.portlets.rules.parameter.display;

import java.util.HashSet;
import java.util.Set;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;
import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;

/**
 * Defines a Select input field that populates its Options from the values returned by a call to
 * a specified ReST Endpoint ( <code>optionUrl</code> ).
 * <p>
 * The corresponding ReST endpoint must return either a list of key-value pairs or an Array of values.
 * For example:
 * <p>
 * <pre>
 * GET http://example.com/dropdownOptions
 *
 *  {
 *      foo: {  id: 'optionFoo', value: 'Choose Option Foo'},
 *      bar: {  id: 'optionBar', value: 'Choose Option Bar'},
 *      baz: {  id: 'optionBaz', value: 'Choose Option Baz'}
 *  }
 *
 *
 * Alternatively, as an array:
 *
 * [
 *      {  id: 'optionFoo', value: 'Choose Option Foo'},
 *      {  id: 'optionBar', value: 'Choose Option Bar'},
 *      {  id: 'optionBaz', value: 'Choose Option Baz'}
 * ]
 * </pre>
 *
 * @author Geoff M. Granum
 */
public class RestDropdownInput extends TextInput<TextType> {

    private final String optionUrl;
    private final String jsonValueField;
    private final String jsonLabelField;
    private boolean allowAdditions = false;
    private int minSelections = 0;
    private int maxSelections = 1;

    public RestDropdownInput(String optionUrl, String jsonValueField, String jsonLabelField) {
        super("restDropdown", new TextType());
        this.optionUrl = optionUrl;
        this.jsonValueField = jsonValueField;
        this.jsonLabelField = jsonLabelField;
    }

    public RestDropdownInput allowAdditions() {
        this.allowAdditions = true;
        return this;
    }

    public boolean isAllowAdditions() {
        return allowAdditions;
    }

    public RestDropdownInput minSelections(int minSelections) {
        this.minSelections = minSelections;
        return this;
    }

    public RestDropdownInput maxSelections(int maxSelections) {
        this.maxSelections = maxSelections;
        return this;
    }

    public int getMinSelections() {
        return minSelections;
    }

    public int getMaxSelections() {
        return maxSelections;
    }

    public String getOptionUrl() {
        return optionUrl;
    }

    public String getJsonValueField() {
        return jsonValueField;
    }

    public String getJsonLabelField() {
        return jsonLabelField;
    }
}

