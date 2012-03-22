package com.dotmarketing.portlets.contentlet.util;

import java.util.Date;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ContentletHTMLUtil {
	
	/**
	 * 
	 * @param contentlet
	 * @return
	 */
	public static String toPrettyHTMLString(Contentlet contentlet) {
    	ContentletAPI conAPI = APILocator.getContentletAPI();
    	User user = null;
    	try{
    		user = APILocator.getUserAPI().getSystemUser();
    	}catch (DotDataException e) {
			Logger.error(Contentlet.class, "Unable to get system user", e);
		}
        StringBuffer sb = new StringBuffer();
        Structure st = StructureCache.getStructureByInode(contentlet.getStructureInode());
        List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
        for (Field f : fields) {
            if (f.isListed()) {
                if (f.getFieldType().equals(Field.FieldType.DATE.toString())) {
                    Date value = new Date();
					
					value = (Date) conAPI.getFieldValue(contentlet, f);
					
                    sb.append(f.getFieldName() + ": " + UtilMethods.dateToHTMLDate(value) + "<br>");
                } else if (f.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
                    Date value = new Date();
					
					value = (Date) conAPI.getFieldValue(contentlet, f);
					
                    sb.append(f.getFieldName() + ": " + UtilMethods.dateToHTMLDate(value) + " "
                            + UtilMethods.dateToHTMLTime(value) + "<br>");
                } else if (f.getFieldType().equals(Field.FieldType.TIME.toString())) {
                    Date value = new Date();
					
					value = (Date) conAPI.getFieldValue(contentlet, f);
					
                    sb.append(f.getFieldName() + ": " + UtilMethods.dateToHTMLTime(value) + "<br>");
                } else if (f.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) || f.getFieldType().equals(Field.FieldType.RADIO.toString())
                        || f.getFieldType().equals(Field.FieldType.SELECT.toString()) || f.getFieldType().equals(Field.FieldType.TEXT.toString())
                        || f.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || f.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
                    String value = "";
					
					value = ((Object) conAPI.getFieldValue(contentlet, f)).toString();
					
                    sb.append(f.getFieldName() + ": " + UtilMethods.shortenString(value, 30) + "<br>");
                }
            }
        }
        return sb.toString();
    }
}
