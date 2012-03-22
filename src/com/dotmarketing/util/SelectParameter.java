package com.dotmarketing.util;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Util class to build select parameters for reports
 *
 * @author Gabriela Gonzalez
 * @since 1.5.0
 * @version 1.5.0
 **/

public class SelectParameter {

    public List<String> options;
    public int[] selOptions;
    public String type;

    /**
     * SelectParameter class constructor
     * @param options - Array of options to be displayed in the select field
     * @param selOptions - Position of the options that should be displayed as selected
     * @param type - Type of selection: single or multiple
     */
    public SelectParameter(List<String> options, int[] selOptions, String type){
	this.options = options;
	this.selOptions = selOptions;
	this.type = type;
    }

    /**
     * Get method for options member
     * @return Array of String with the options of the select field
     */
    public List<String> getOptions(){
	return this.options;
    }

    /**
     * Get method for selOptions member
     * @return Array of int with the position of the selected options
     */
    public int[] getSelOptions() {
	return this.selOptions;
    }

    /**
     * Get method for type member
     * @return String indicating if the select allows single or multiple options to be selected
     */
    public String getType(){
	return this.type;
    }

    /**
     * Set method for options member
     * @param Array of String with options
     */
    public void setOptions(List<String> options){
	this.options = options;
    }

    /**
     * Set method for selOptions member
     * @param Array of int with selected option positions
     */
    public void setSelOptions(int[] selOptions) {
	this.selOptions = selOptions; 
    }

    /**
     * Set method for type member
     * @param String indicating the type. It should be set as: "single" or "multiple"
     * @return
     * @exception
     * @see
     */
    public void setType(String type){
	this.type = type;
    }

    /**
     * Method that returns the number of options available in the select field
     * @return Number of options in the option array
     */
    public int getNumOptions(){
	return options.size();
    }

    /**
     * Method that returns the number of options currently selected
     * @return Number of options in the selOption array
     */
    public int getNumSelected(){
	return Array.getLength(this.selOptions);
    }

    /**
     * Method to get the value of a given option
     * @param pos - position of the option to be returned
     * @return String value of the option required
     */
    public String getOption(int pos) {
	return options.get(pos);
    }

    /**
     * Method that indicates if an option is selected
     * @param pos - position of the option that needs to be validated
     * @return true if the options is selected, false otherwise
     */
    public boolean isSelected (int pos) {
	boolean isSel = false;
	
	for (int i=0; i < this.getNumSelected(); i++) {
	    if (pos == this.selOptions[i]) {
		isSel = true;
		break;
	    }
	}
	
	return isSel;
    }

    /**
     * Method that indicates if the selection is single
     * @return true if one single option can be selected, false otherwise
     */
    public boolean isSingle () {
	return this.getType().equals("single");
    }

    /**
     * Method that indicates if the selection is multiple
     * @return true if multiple options can be selected, false otherwise
     */
    public boolean isMultiple () {
	return this.getType().equals("multiple");
    }
    
    /**
     * Method that returns the value or the list of values of the selected options
     * @return String with the selected option/s. If the type of the parameter is single, it just returns the value of the option.
     *                If the type of the parameter is multiple, it returns a list with all the options: ("opt1", "opt2", "opt3").
     */
    public String getSelected () {
	String paramType = this.getType();
	String selOptions = "";

	if (paramType.equals("single")){
	    selOptions = this.getOption(this.getSelOptions()[0]);
	}

	if (paramType.equals("multiple")){
	    
	    selOptions = "(";
	    for (int i=0; i < this.getNumSelected(); i++) {
		if (i != 0) {
		    selOptions = selOptions.concat(",");
		}
		selOptions = selOptions.concat("'").concat(this.getOption(this.getSelOptions()[i])).concat("'");
	    }
	    
	    selOptions = selOptions.concat(")");
	}
	
	return selOptions;
	
    }

    private void test() {
	//	String[] opt = { "Opt1", "Opt2" };
	//	boolean[] sel = { true, false };
    java.util.ArrayList<String> options = new java.util.ArrayList<String>();
    options.add("Opt3");
    options.add("Opt4");
	SelectParameter selparam = new com.dotmarketing.util.SelectParameter(options, new int[] { 1 }, "single");
    }

}