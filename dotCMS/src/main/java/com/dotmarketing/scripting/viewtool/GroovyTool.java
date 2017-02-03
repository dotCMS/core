package com.dotmarketing.scripting.viewtool;

import com.dotmarketing.scripting.util.BSFUtil;
import com.dotmarketing.util.Logger;

public class GroovyTool extends AbstractScriptingTool {

	public String eval(String scriptletOrFile) {
		if(!engineInited){
			initEngine();
		}
		try{
			if(!canUserEvalute()){
				Logger.error(this, "User Has No Permission To Evalute Code"); 
				throw new Exception("User Has No Permission To Evalute Code");
			}else{
				if(scriptletOrFile.endsWith(".groovy") || scriptletOrFile.endsWith(".gy")){
					return bsfUtil.evalFile(scriptletOrFile, host).toString();
				}else{
					return bsfUtil.evalExpression(BSFUtil.LANGUAGE_GROOVY, scriptletOrFile).toString();
				}
			}
		}catch (Exception e) {
			Logger.error(GroovyTool.class,e.getMessage(),e);
			return null;
		}
	}

	public String evalExpression(String file, String evalExpression) {
		if(!engineInited){
			initEngine();
		}
		try{
			if(!canUserEvalute()){
				Logger.error(this, "User Has No Permission To Evalute Code"); 
				throw new Exception("User Has No Permission To Evalute Code");
			}else{
				bsfUtil.execFile(file, host);
				return bsfUtil.evalExpressionOnFile(file,host,evalExpression).toString();
			}
		}catch (Exception e) {
			Logger.error(GroovyTool.class,e.getMessage(),e);
			return null;
		}
	}

}
