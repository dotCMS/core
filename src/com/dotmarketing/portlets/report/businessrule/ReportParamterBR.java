package com.dotmarketing.portlets.report.businessrule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import javax.sql.DataSource;

import com.dotmarketing.portlets.report.model.ReportParameter;
import com.dotmarketing.util.Logger;

public class ReportParamterBR {
    
    public static boolean isAllowedParameter(String clazz){
	if(isStringParameter(clazz)){
	    return true;
	}else if(isDateParameter(clazz)){
	    return true;
	}else if(isBooleanParameter(clazz)){
	    return true;
	}else if(isDataSourceParameter(clazz)){
	    return true;
	}else if(isBigDecimalParameter(clazz)){
	    return true;
	}else if(isIntegerParameter(clazz)){
	    return true;
	}else if(isFloatParameter(clazz)){
	    return true;
	}else if(isLongParameter(clazz)){
	    return true;
	}else if(isDoubleParameter(clazz)){
	    return true;
	}else if(isObjectParameter(clazz)){
	    return true;
	}else{
	    return false;
	}
    }
    
    public static boolean isNumberParameter(String clazz){
	if(isBigDecimalParameter(clazz)){
	    return true;
	}else if(isIntegerParameter(clazz)){
	    return true;
	}else if(isFloatParameter(clazz)){
	    return true;
	}else if(isLongParameter(clazz)){
	    return true;
	}else if(isDoubleParameter(clazz)){
	    return true;
	}else{
	    return false;
	}
    }
    
    public static boolean isStringParameter(String clazz){
	if(clazz.equals(String.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isDateParameter(String clazz){
	if(clazz.equals(Date.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isBooleanParameter(String clazz){
	if(clazz.equals(Boolean.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isDataSourceParameter(String clazz){
	if(clazz.equals(DataSource.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isBigDecimalParameter(String clazz){
	if(clazz.equals(BigDecimal.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isIntegerParameter(String clazz){
	if(clazz.equals(Integer.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isFloatParameter(String clazz){
	if(clazz.equals(Float.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isLongParameter(String clazz){
	if(clazz.equals(Long.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    public static boolean isDoubleParameter(String clazz){
	if(clazz.equals(Double.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }

    /**
     * Verifies if the class of a parameter is java.lang.Obeject
     * @param String clazz - par.getClassType()
     * @return boolean - true if it is a SelecteParameter, false otherwise
     */
    public static boolean isObjectParameter(String clazz){
	Logger.debug(ReportParamterBR.class, "Call to isObjectParameter. Class: " + clazz);
	if(clazz.equals(java.lang.Object.class.getName())){
	    return true;
	}else{
	    return false;
	}
    }
    
    public static long getCalendarNumber(ArrayList<ReportParameter> pars){
	long result = 0;
	for (ReportParameter parameter : pars) {
	    if(isDateParameter(parameter.getClassType())){
		result++;
	    }
	}
	return result;
    }
}
