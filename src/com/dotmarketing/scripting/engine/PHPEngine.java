package com.dotmarketing.scripting.engine;

/*
 * Copyright 2004,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.util.BSFEngineImpl;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.Path;
import com.caucho.vfs.StringPath;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.WriterStreamImpl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotReindexStateException;
import com.dotmarketing.scripting.util.BSFUtil;
import com.dotmarketing.scripting.util.php.DotCMSPHPCauchoVFS;
import com.dotmarketing.scripting.util.php.PHPEvalWrapper;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This class was written originally written for the Scripting Plugin
 * of dotCMS.  It returns a wrapper object which attempts to wrap the 
 * Out and the Value which Quercus provides. 
 * 
 * @author  Jason Tesser
 */
public class PHPEngine extends BSFEngineImpl {

	private static final Quercus php = new Quercus();
	private Map<String,Object> globals = new HashMap<String, Object>();
	
	private static boolean inited = false;
		
	private synchronized static void init(){
		if(inited)
			return;
		php.setStrict(false);
		php.setRequireSource(false);
		php.init();
		php.setIni("allow_url_include", BooleanValue.create(true).toStringBuilder());
		inited = true;
	}
	
	public PHPEngine() {
		if(!inited){
			init();
		}
	}

	
//	@Override
//	public void exec(String source, int lineNo, int columnNo, Object script)
//			throws BSFException {
//		// TODO Auto-generated method stub
//		super.exec(source, lineNo, columnNo, script);
//	}
	
	@Override
	public void declareBean(BSFDeclaredBean bean) throws BSFException {
		globals.put(bean.name, bean.bean);
	}
	
	@Override
	public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
		globals.remove(bean.name);
	}
	
	 /**
     * Return an object from an extension.
     * @param File to call functions on
     * @param method The name of the method to call.
     * @param args an array of arguments to be
     * passed to the extension, which may be either
     * Vectors of Nodes, or Strings.
     */
	public Object call(Object object, String method, Object[] args) throws BSFException {
		Env env = php.createEnv(null, null, null, null);
		env.start();
		populateEnv(env);
		Value v = null;
		PHPEvalWrapper phpw = null;
		if(object instanceof PHPEvalWrapper){
			phpw = (PHPEvalWrapper)object;
		}else{
			phpw = (PHPEvalWrapper)eval(object.toString(), -1, -1, "");
		}
		List<Function> funs = phpw.getFunctions();
		Value[] values = null;
		if(args != null){
			values = new Value[args.length];
			for (int i = 0; i < args.length; i++) {
				Object o = args[i];
				values[i] = env.wrapJava(o);
			}
		}
		
		try{
			if(funs != null && funs.size() > 0){
				v = callFunctionFromFunctionList(funs, method, env, values);
			}else{
				v = callFunctionOnPage(phpw.getPage(), method, env, values);
			}
		}catch (Exception e) {
			Logger.error(this, e.getMessage(),e );
			throw new BSFException("Unable to find function name " + method != null ? method : "" + " to call");
		}
		return v;
	}
	
	public Object eval(String source, int lineNo, int columnNo, Object oscript) throws BSFException {
		String scriptText = oscript.toString();
		Path path;
		PHPEvalWrapper wrapper = new PHPEvalWrapper();
		if(source.endsWith(".php")){
			String filePath = source.substring(source.indexOf('/'),source.length());
			path = buildRootPath(source).lookup(filePath);
			if(UtilMethods.isSet(scriptText)){
				wrapper = evaluateCodePage(path, scriptText);
			}else{
				wrapper = evaluatePage(path);
			}
		}else{
			if(shouldParsePage(scriptText)){
				path = new StringPath(scriptText);
				wrapper = evaluatePage(path);
			}else{
				wrapper = evaluateCode(scriptText);
			}
		}
		return wrapper;
	}
	
	private boolean shouldParsePage(String code){
		return code.contains("<?php");
	}
	
	private PHPEvalWrapper evaluateCodePage(Path path, String code) throws BSFException{
		QuercusPage page = null;
		try{	
			page = php.parse(path);
			
		} catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
			throw new BSFException(BSFException.REASON_IO_ERROR, e.getMessage(), e);
		}
		QuercusProgram qp = null;
		try {
			qp = php.parseCode(code);
		} catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
			throw new BSFException(BSFException.REASON_IO_ERROR, e.getMessage(), e);
		}
		
		PHPEvalWrapper wrapper = new PHPEvalWrapper();
		StringWriter sw = new StringWriter();
		WriterStreamImpl writerImpl = new WriterStreamImpl();
        writerImpl.setWriter(sw);

		WriteStream writeStream = new WriteStream(writerImpl);
		
		Env env = php.createEnv(page, writeStream, null, null);
		env.start();
		populateEnv(env);
		page.executeTop(env);
		Value v = qp.execute(env);
		
		List<Function> funcs = qp.getFunctionList();
		if(funcs != null){
			wrapper.setFunctions(funcs);
		}
	
		PrintWriter pw = writeStream.getPrintWriter();
		pw.flush();
		
		wrapper.setValue(v);
		wrapper.setOut(sw.toString());
		return wrapper;
	}
	
	private PHPEvalWrapper evaluatePage(Path path) throws BSFException{
		QuercusPage page = null;
		try{	
			page = php.parse(path);
		} catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
			throw new BSFException(BSFException.REASON_IO_ERROR, e.getMessage(), e);
		}
		PHPEvalWrapper wrapper = new PHPEvalWrapper();
		StringWriter sw = new StringWriter();
		WriterStreamImpl writerImpl = new WriterStreamImpl();
        writerImpl.setWriter(sw);

		WriteStream writeStream = new WriteStream(writerImpl);
		
		Env env = php.createEnv(page, writeStream, null, null);
		env.start();
		populateEnv(env);
		Value v = page.executeTop(env);
		PrintWriter pw = writeStream.getPrintWriter();
		pw.flush();
		wrapper.setPage(page);
		wrapper.setOut(sw.toString());
		wrapper.setValue(v);
		return wrapper;
	}
	
	private PHPEvalWrapper evaluateCode(String code) throws BSFException{
		QuercusProgram qp = null;
		try {
			qp = php.parseCode(code);
		} catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
			throw new BSFException(BSFException.REASON_IO_ERROR, e.getMessage(), e);
		}
		
		PHPEvalWrapper wrapper = new PHPEvalWrapper();
		StringWriter sw = new StringWriter();
		WriterStreamImpl writerImpl = new WriterStreamImpl();
        writerImpl.setWriter(sw);

		WriteStream writeStream = new WriteStream(writerImpl);
		
		Env env = php.createEnv(null, writeStream, null, null);
		env.start();
		populateEnv(env);
		Value v = qp.execute(env);
		
		List<Function> funcs = qp.getFunctionList();
		if(funcs != null){
			wrapper.setFunctions(funcs);
		}
	
		PrintWriter pw = writeStream.getPrintWriter();
		pw.flush();
		
		wrapper.setValue(v);
		wrapper.setOut(sw.toString());
		return wrapper;
	}
	
	private Value callFunctionOnPage(QuercusPage qp, String functionName, Env env, Value[] values){
		return qp.findFunction(functionName).call(env,values);
	}
	
	private Value callFunctionFromFunctionList(List<Function> funs, String functionName, Env env, Value[] values){
		Value v = null;
		for (Function function : funs) {
			if(function.getName().equals(functionName)){
				v = function.call(env, values);
			}
		}
		return v;
	}

	private Path buildRootPath(String filePath){
		Host h;
		try {
			h = APILocator.getHostAPI().findByName(filePath.substring(0,filePath.indexOf("DOTHOST")), APILocator.getUserAPI().getSystemUser(), true);
		} catch (DotDataException e) {
			Logger.error(PHPEngine.class,e.getMessage(),e);
			throw new DotReindexStateException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(PHPEngine.class,e.getMessage(),e);
			throw new DotReindexStateException(e.getMessage(), e);
		}
		DotCMSPHPCauchoVFS path = new DotCMSPHPCauchoVFS(h);
		return path;
	}
	
	private void populateEnv(Env env){
		for(String key : globals.keySet()){
			env.setGlobalValue(key, env.wrapJava(globals.get(key)));
		}
	}
	
}

