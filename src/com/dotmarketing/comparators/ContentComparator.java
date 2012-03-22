package com.dotmarketing.comparators;

import java.util.Comparator;
import java.util.Date;

import org.apache.commons.beanutils.PropertyUtils;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * @author Maria & David (Fat contentlet adaptations)
 * 
 */
public class ContentComparator implements Comparator<Contentlet> {

    private String orderType = "";

    private String orderField = "";
    
    public ContentComparator(String orderField, String orderType) {
        super();
        this.orderType = orderType;
        this.orderField = orderField;
    }

    public ContentComparator(String orderFieldAndType) {
        super();
        String[] values = orderFieldAndType.split(" ");
        if (values.length > 0)
        	this.orderField = values[0];
        
        if (values.length > 1)
        	this.orderType = values[1];
        else 
        	this.orderType = "asc";
    }

    public int compare(Contentlet w1, Contentlet w2) {

        try {
            Object value1 = PropertyUtils.getSimpleProperty(w1, orderField);
            Object value2 = PropertyUtils.getSimpleProperty(w2, orderField);
            
            int ret = 0;
            
            if (value1 == null || value2 == null) {
            	return ret;
            } else if (value1 instanceof Integer) {
            	ret = ((Integer)value1).compareTo((Integer)value2);
            } else if (value1 instanceof Long) {
            	ret = ((Long)value1).compareTo((Long)value2);
            } else if (value1 instanceof Date) {
            	ret = ((Date)value1).compareTo((Date)value2);
            } else if (value1 instanceof String) {
            	ret = ((String)value1).compareTo((String)value2);
            } else if (value1 instanceof Float) {
            	ret = ((Float)value1).compareTo((Float)value2);
            } else if (value1 instanceof Boolean) {
            	ret = ((Boolean)value1).compareTo((Boolean)value2);
            }

            if (orderType.equals("asc")) {
                return ret;
            }

            return ret * -1;

        } catch (Exception e) {

        }
        return 0;
    }
}