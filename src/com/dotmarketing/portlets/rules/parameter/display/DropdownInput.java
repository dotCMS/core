package com.dotmarketing.portlets.rules.parameter.display;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
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
        super("dropdown", new TextType());
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
        public final int priority;

        public Option(String i18nKey, String value, int priority) {
            this.i18nKey = i18nKey;
            this.value = value;
            this.priority = priority;
        }
    }
}
 
