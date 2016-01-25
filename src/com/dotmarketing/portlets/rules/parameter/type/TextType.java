package com.dotmarketing.portlets.rules.parameter.type;

import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.comparison.MatcherCheck;
import org.hamcrest.Matchers;

/**
 * @author Geoff M. Granum
 */
public class TextType extends DataType {

    private int minLength = 0;
    private int maxLength = 255;
    private String defaultValue = "";

    public TextType() {
        this("text");
    }

    public TextType(String id) {
        super(id, "api.system.type.text");
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public TextType minLength(int minLength) {
        this.minLength = minLength;
        return this;
    }

    public TextType maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public TextType defaultValue(String defaultValue){
        this.defaultValue = defaultValue;
        return this;
    }

    public String convert(String value){
        return value;
    }

    @Override
    public void checkValid(String value) {
        if(minLength != 0){
            MatcherCheck.checkThat(value, Matchers.notNullValue());
        }
    }

    @Override
    public TextType restrict(Comparison restriction) {
        return (TextType)super.restrict(restriction);
    }
}
 
