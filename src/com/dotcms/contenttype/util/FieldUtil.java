package com.dotcms.contenttype.util;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.repackage.org.apache.commons.lang.BooleanUtils;
import com.dotmarketing.business.DotStateException;
import com.google.common.base.Preconditions;
import com.liferay.util.StringUtil;

public class FieldUtil {

	
	public void checkFieldValues(DataTypes type, String values){
		if(values==null || type == DataTypes.TEXT) return;
        String[] tempVals = StringUtil.split(values.replaceAll("\r\n","|").trim(), "|");
		for(int i=1;i<tempVals.length;i+= 2){
			try{
				if(type == DataTypes.FLOAT){
					Float.parseFloat(tempVals[i]);
				}else if(type == DataTypes.INTEGER){
					Integer.parseInt(tempVals[i]);
				}
				else if(type == DataTypes.BOOL){
					String x = "1".equals(tempVals[i]) ? "true" : "0".equals(tempVals[i]) ? "false" : tempVals[i];

					Boolean y = BooleanUtils.toBooleanObject(x);
					if(null==y){
						throw new DotStateException("invalid boolean");
					}
					
		
				}
			}catch (Exception e) {
				throw new DotStateException("Values entered are not valid for this datatype" + type + " " + values);

			}
		}
	}
}
