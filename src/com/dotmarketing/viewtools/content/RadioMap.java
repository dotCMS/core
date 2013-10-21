package com.dotmarketing.viewtools.content;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;

/**
 * The class helps get at the values and options of a Select Field belonging to a piece of content. 
 * 
 * @author Jason Tesser
 * @since 1.9.1.3
 */
public class RadioMap {

	/**
	 * All the possible options
	 */
	private List<String> options = new ArrayList<String>();
	public List<String> getValues() {
		return values;
	}

	private List<String> values = new ArrayList<String>();
	private Object selectValue;	
	
	public RadioMap(Field field, Contentlet content) {
		String[] pairs = (field.getValues()!=null?field.getValues():"").split("\r\n");
		for (int j = 0; j < pairs.length; j++) {
		    String pair = pairs[j];
		    String[] tokens = pair.split("\\|");
		    String name = tokens.length > 0 ? tokens[0] : "";
		    options.add(name);
		    values.add(tokens.length > 1 ? tokens[1].trim() : name.trim());
		}
		selectValue = APILocator.getContentletAPI().getFieldValue(content, field);
	}

	/**
	 * @return the options
	 */
	public List<String> getOptions() {
		return options;
	}

	/**
	 * @return the value
	 */
	public List<String> getValue() {
		return values;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	/**
	 * @return the selectValue
	 */
	public Object getSelectValue() {
		return selectValue;
	}
	
	
	
	
	
	/**
	 * This will return the label of the
	 * selected value
	 * @return
	 */
	public Object getSelectOption() {
		if(selectValue !=null && values != null){
			int i=0;
			for(String x : values){
				if(x.equals(selectValue)){
					return options.get(i);
				}
				i++;
			}
		}
		return null;
	}
	
	
	
	
	
	
}
