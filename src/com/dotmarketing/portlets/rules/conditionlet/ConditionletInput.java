package com.dotmarketing.portlets.rules.conditionlet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class contains all the information and/or data needed to build the input for a Condition
 */

public class ConditionletInput {

    public enum ResponseType {
        RAW_DATA,
        VELOCITY,
        HTML, // ??
        JAVASCRIPT // ??
    }

    public enum InputType {
        SELECT,
        FILTERING_SELECT,
        CHECKBOX,
        RADIOBUTTON,
        TEXTBOX,
        TEXTAREA // etc
    }

    private ResponseType responseType;
    private LinkedHashMap<String, String> data;
    private Boolean allowUserInput;
    private Boolean multipleChoice;
    private InputType inputType;
    private String defaultValue;

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(LinkedHashMap<String, String> data) {
        this.data = data;
    }

    public Boolean getAllowUserInput() {
        return allowUserInput;
    }

    public void setAllowUserInput(Boolean allowUserInput) {
        this.allowUserInput = allowUserInput;
    }

    public Boolean getMultipleChoice() {
        return multipleChoice;
    }

    public void setMultipleChoice(Boolean multipleChoice) {
        this.multipleChoice = multipleChoice;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
