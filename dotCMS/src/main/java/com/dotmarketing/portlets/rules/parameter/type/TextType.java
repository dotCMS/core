package com.dotmarketing.portlets.rules.parameter.type;

import com.dotmarketing.portlets.rules.parameter.type.constraint.StandardConstraints;
import com.dotmarketing.portlets.rules.parameter.type.constraint.TypeConstraint;

/**
 * @author Geoff M. Granum
 */
public class TextType extends DataType<String> {

    public TextType() {
        this("text");
    }

    public TextType(String id) {
        super(id, "api.system.type.text");
    }

    public TextType required() {
        return this.restrict(StandardConstraints.required);
    }

    public TextType minLength(int minLength) {
        return this.restrict(StandardConstraints.minLength(minLength));
    }

    public TextType maxLength(int maxLength) {
        return this.restrict(StandardConstraints.maxLength(maxLength));
    }

    /**
     * Overridden for mutable return type.
     */
    @Override
    public TextType defaultValue(String defaultValue){
        return (TextType)super.defaultValue(defaultValue);
    }

    public String convert(String value){
        return value;
    }

    @Override
    public TextType restrict(TypeConstraint restriction) {
        return (TextType)super.restrict(restriction);
    }


}
 
