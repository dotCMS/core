package com.dotcms.contenttype.model.field;

import java.util.ArrayList;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.BooleanUtils;
import com.dotmarketing.business.DotStateException;
import com.liferay.util.StringUtil;

public abstract class SelectableValuesField extends Field{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Value.Lazy
	@Override
	public List<SelectableValue> selectableValues() {
		List<SelectableValue> vals = new ArrayList<>();
		if(values()!=null && values().indexOf('|')>-1){
			String[] olds = values().split("\r\n");
			for(String old : olds){
				String[] keyVal = old.split("|",2);
				vals.add(new SelectableValue(keyVal[0], keyVal[1]));
			}
		}
		return ImmutableList.copyOf(vals);
	}
	
	@Value.Check
	public void check() {
		super.check();
		if(iDate().before(legacyFieldDate))return;
		if(values()!=null){
	        String[] tempVals = StringUtil.split(values().replaceAll("\r\n","|").trim(), "|");
			for(int i=1;i<tempVals.length;i+= 2){
				try{
					if(dataType() == DataTypes.FLOAT){
						Float.parseFloat(tempVals[i]);
					}else if(dataType() == DataTypes.INTEGER){
						Integer.parseInt(tempVals[i]);
					}
					else if(dataType() == DataTypes.BOOL){
						String x = "1".equals(tempVals[i]) ? "true" : "0".equals(tempVals[i]) ? "false" : tempVals[i];
	
						Boolean y = BooleanUtils.toBooleanObject(x);
						if(null==y){
							throw new DotStateException("invalid boolean");
						}
					}
				}catch (Exception e) {
					throw new DotStateException("Values entered are not valid for this datatype" + dataType() + " " + values(),e);
	
				}
			}
		}
	}

}
