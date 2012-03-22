package com.dotmarketing.scripting.viewtool;

import java.io.StringWriter;

import org.apache.bsf.BSFException;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.scripting.util.BSFUtil;
import com.dotmarketing.scripting.util.php.PHPEvalWrapper;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.XMLUtils;


public class PHPTool extends AbstractScriptingTool {

	public String evalExpression(String file, String evalExpression) {
		if(!engineInited){
			initEngine();
		}
		try {
			if(!canUserEvalute()){
				Logger.warn(this, "Last mod user of content does nto have scripting developer role");
				return null;
			}
			return ((PHPEvalWrapper)bsfUtil.evalExpressionOnFile(file, host, evalExpression)).getOut();
		} catch (Exception e) {
			Logger.error(PHPTool.class,e.getMessage(),e);
			return null;
		}
	}
	
	public String eval(String scriptletOrFile) {
		if(!engineInited){
			initEngine();
		}
		try {
			if(!canUserEvalute()){
				Logger.warn(this, "Last mod user of content does nto have scripting developer role");
				return null;
			}
			if(scriptletOrFile.contains("<")){
				return ((PHPEvalWrapper)renderPHP(scriptletOrFile)).getOut();	
			}
			return ((PHPEvalWrapper)evalFile(scriptletOrFile)).getOut();
		} catch (Exception e) {
			Logger.error(PHPTool.class,e.getMessage(),e);
			return null;
		}
	}

	private Object renderPHP(String code) throws Exception{
		if(code == null){
			return null;
		}
		if(code.indexOf("<?") < 0){
			return evalPHP(code);
		}
		StringWriter sw = new StringWriter();
		String[] codelets = code.split("(\\<\\?|\\?\\>)");
	
		
		for(int i=0 ; i<codelets.length ; i++){
			
			if((i % 2) ==0) 
				sw.append("echo \"" + codelets[i].replaceAll("\\\"", "\\\\\"") + "\";\n");
			else
				sw.append(codelets[i] );
		}	
				
		return   evalPHP(sw.toString());
	}
	
	/**
	 * Evaluates an expression from the Scripting Engine
	 * @param expression
	 * @return
	 * @throws Exception 
	 */
	private Object evalPHP(String expression) throws Exception{

		try {
			if(!canUserEvalute()){
				Logger.error(this, "User Has No Permission To Evalute Code"); 
				throw new Exception("User Has No Permission To Evalute Code");
			}else{
				if(expression.endsWith(".php")){
					return bsfUtil.evalFile(expression, host);
				}
				if(UtilMethods.isSet(expression)){
					if(expression.indexOf("<?php") > -1){
						expression=	UtilMethods.replace(expression, "<?php", "");
						expression=	UtilMethods.replace(expression, "?>", "");
					}else if(expression.indexOf("<?") > -1){
						expression=	UtilMethods.replace(expression, "<?", "");
						expression=	UtilMethods.replace(expression, "?>", "");
					}
					
					return bsfUtil.evalExpression(BSFUtil.LANGUAGE_PHP, expression);
				}
				
			}
		} catch (BSFException e) {
			Logger.error(this, e.getMessage(), e);
			
			String err = XMLUtils.xmlEscape(e.toString());
			err = err.replaceAll("org.apache.bsf.BSF", "");

			return err;
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
		}
		return null;
	}
	
}
