package com.dotcms.rendering.velocity.viewtools.content;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The class helps get at the values and options of a Select Field belonging to a piece of content. 
 * 
 * @author Jason Tesser
 * @since 1.9.1.3
 */
public class SelectMap {

	/**
	 * All the possible options
	 */
	private List<String> options = new ArrayList<String>();
	private List<String> values = new ArrayList<String>();
	private Object selectValue;
	
	public SelectMap(com.dotcms.contenttype.model.field.Field field, Contentlet content) {
		String[] pairs = (field.values()!=null?field.values():"").split("\r\n");
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
	public List<String> getValues() {
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
}
