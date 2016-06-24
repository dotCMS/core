package com.dotmarketing.portlets.rules.parameter.display;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;

import java.util.Map;

/**
 * @author Geoff M. Granum
 */
public class DropdownInput extends TextInput<TextType> {

    private final Map<String, Option> options = Maps.newLinkedHashMap();
    private boolean allowAdditions = false;
    private int minSelections = 0;
    private int maxSelections = 1;

    public DropdownInput() {
        this(new TextType());
    }

    public DropdownInput(TextType type) {
        super("dropdown", type);
    }

    public DropdownInput option(String optionKey) {
        return this.option(optionKey, optionKey);
    }

    public DropdownInput option(String optionKey, String optionValue) {
        return this.option(optionKey, optionValue, options.size() + 1);
    }

    public DropdownInput option(String optionKey, String optionValue, int priority) {
        options.put(optionKey, new Option(optionKey, optionValue, priority));
        return this;
    }

    public DropdownInput option(Option option) {
        option.priority = options.size() + 1;
        options.put(option.i18nKey, option);
        return this;
    }

    /**
     * Use to allow new values on the dropdown, and is used by the REST API to validate parameters.
     * @return
     */
    public DropdownInput allowAdditions() {
        this.allowAdditions = true;
        return this;
    }

    public boolean isAllowAdditions() {
        return allowAdditions;
    }

    public DropdownInput minSelections(int minSelections) {
        this.minSelections = minSelections;
        return this;
    }

    public DropdownInput maxSelections(int maxSelections) {
        this.maxSelections = maxSelections;
        return this;
    }

    public int getMinSelections() {
        return minSelections;
    }

    public int getMaxSelections() {
        return maxSelections;
    }

    public Map<String, Option> getOptions() {
        return options;
    }

    public static class Option {

        public final String i18nKey;
        public final String value;
        public int priority;
        private String icon;

        public Option(String i18nKey, String value, int priority) {
            this.i18nKey = i18nKey;
            this.value = value;
            this.priority = priority;
        }

        public Option(String i18nKey, String value) {
            this.i18nKey = i18nKey;
            this.value = value;
            this.priority = priority;
        }

        public Option icon(String icon) {
            this.icon = icon;
            return this;
        }

        public String getIcon() {
            return icon;
        }
    }

    @Override
    public void checkValid(String value) throws InvalidRuleParameterException, RuleEngineException{
        if(allowAdditions)
        	try{
        		this.getDataType().checkValid(value);
        	}catch(Exception e){
        		Logger.error(this.getClass(), e.getMessage(), e);
        		throw new InvalidRuleParameterException(e.getMessage());
        	}
        else
        	if(!options.containsKey(value))
        		throw new InvalidRuleParameterException("Parameter '%s' is not allowed.  Additions are not allowed on the dropdown",value);
    }
}

