package com.dotmarketing.portlets.rules.parameter.display;

import com.dotmarketing.portlets.rules.parameter.type.TextType;

/**
 * @author Geoff M. Granum
 */
public class TextInput<T extends TextType> extends Input<T> {

    private String placeholder = "";

    public TextInput(T dataType) {
        super("text", dataType);
    }

    protected TextInput(String id, T dataType) {
        super(id, dataType);
    }

    public TextInput placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public String getPlaceholder() {
        return placeholder;
    }
}
 
