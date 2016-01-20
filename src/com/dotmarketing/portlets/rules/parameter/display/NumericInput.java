package com.dotmarketing.portlets.rules.parameter.display;


import com.dotmarketing.portlets.rules.parameter.type.NumericType;

/**
 * @author Erick Gonzalez
 */
public class NumericInput<T extends NumericType> extends Input<T> {

	private String placeholder = "";

    public NumericInput(T dataType) {
        this("number", dataType);
    }

    protected NumericInput(String id, T dataType) {
        super(id, dataType);
    }

    public NumericInput placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public String getPlaceholder() {
        return placeholder;
    }
}