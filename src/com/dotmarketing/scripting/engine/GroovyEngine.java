/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotmarketing.scripting.engine;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.BSFEngineImpl;
import org.apache.bsf.util.BSFFunctions;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * A BSF Engine for the <a href="http://groovy.codehaus.org/">Groovy</a>
 * scripting language.
 * <p/>
 * It's derived from the Jython / JPython engine
 *
 * @author James Strachan
 */
public class GroovyEngine extends BSFEngineImpl {
	   private static final FileAPI fileAPI = APILocator.getFileAPI();
	private static final UserAPI userAPI = APILocator.getUserAPI();
	protected GroovyShell shell;
	    private static String realPath = null;
		private static String assetPath = "/assets";

	    /*
	     * Convert a non java class name to a java classname
	     * This is used to convert a script name to a name
	     * that can be used as a classname with the script is
	     * loaded in GroovyClassloader#load()
	     * The method simply replaces any invalid characters
	     * with "_".
	     */
	    private String convertToValidJavaClassname(String inName) {
	        if (inName == null || inName.equals("")) {
	            return "_";
	        }
	        StringBuffer output = new StringBuffer(inName.length());
	        boolean firstChar = true;
	        for (int i = 0; i < inName.length(); ++i) {
	            char ch = inName.charAt(i);
	            if (firstChar && !Character.isJavaIdentifierStart(ch)) {
	                ch = '_';
	            } else if (!firstChar
	                    && !(Character.isJavaIdentifierPart(ch) || ch == '.')) {
	                ch = '_';
	            }
	            firstChar = (ch == '.');
	            output.append(ch);
	        }
	        return output.toString();
	    }

	    /**
	     * Allow an anonymous function to be declared and invoked
	     */
	    public Object apply(String source, int lineNo, int columnNo, Object funcBody, Vector paramNames,
	                        Vector arguments) throws BSFException {
	        Object object = eval(source, lineNo, columnNo, funcBody);
	        if (object instanceof Closure) {
	            // lets call the function
	            Closure closure = (Closure) object;
	            return closure.call(arguments.toArray());
	        }
	        return object;
	    }

	    /**
	     * Call the named method of the given object.
	     */
	    public Object call(Object sourceCode, String method, Object[] args) throws BSFException {
	    	GroovyClassLoader gcl = new GroovyClassLoader();
	    	try {
				return ((GroovyObject)gcl.parseClass(sourceCode.toString()).newInstance()).invokeMethod(method, args);
	    	} catch (Exception e) {
	            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "exception from Groovy: " + e, e);
	        }
	    }

	    /**
	     * Evaluate an expression.
	     */
	    public Object eval(String source, int lineNo, int columnNo, Object script) throws BSFException {
	        try {
//	            source = convertToValidJavaClassname(source);
//	            return getEvalShell().evaluate(script.toString(), source);
//	        	return getEvalShell().evaluate(loadFile(source),convertToValidJavaClassname(source.substring(source.indexOf('/'),source.length())));
				if(source.equals("(java)")){
        			return getEvalShell().evaluate(script.toString());
				}else if(UtilMethods.isSet(script)){
    	        		return getEvalShell().evaluate(readFile(source) + "\n" + script,convertToValidJavaClassname(source.substring(source.indexOf('/'),source.length())));
        		}else{
	        		return getEvalShell().evaluate(readFile(source),convertToValidJavaClassname(source.substring(source.indexOf('/'),source.length())));
    	    	}
//	        	return getEvalShell().evaluate(script.toString(),convertToValidJavaClassname(source.substring(source.indexOf('/'),source.length())));
	        } catch (Exception e) {
	            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "exception from Groovy: " + e, e);
	        }
	    }

	    /**
	     * Execute a script.
	     */
	    public void exec(String source, int lineNo, int columnNo, Object script) throws BSFException {
	        try {
	            // use evaluate to pass in the BSF variables
	            //source = convertToValidJavaClassname(source);
	        	getEvalShell().evaluate(readFile(source),convertToValidJavaClassname(source.substring(source.indexOf('/'),source.length())));
//	        	getEvalShell().parse(script.toString(),convertToValidJavaClassname(source.substring(source.indexOf('/'),source.length())));
	        } catch (Exception e) {
	            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "exception from Groovy: " + e, e);
	        }
	    }

	    /**
	     * Initialize the engine.
	     */
	    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
	        super.initialize(mgr, lang, declaredBeans);

	        // create a shell
	        shell = new GroovyShell(mgr.getClassLoader());
//	        shell.
	        // register the mgr with object name "bsf"
	        shell.setVariable("bsf", new BSFFunctions(mgr, this));

	        int size = declaredBeans.size();
	        for (int i = 0; i < size; i++) {
	            declareBean((BSFDeclaredBean) declaredBeans.elementAt(i));
	        }
	        try {
				realPath = Config.getStringProperty("ASSET_REAL_PATH");
			} catch (Exception e) { }
			try {
				assetPath = Config.getStringProperty("ASSET_PATH");
			} catch (Exception e) { }
	    }

	    /**
	     * Declare a bean
	     */
	    public void declareBean(BSFDeclaredBean bean) throws BSFException {
	        shell.setVariable(bean.name, bean.bean);
	    }

	    /**
	     * Undeclare a previously declared bean.
	     */
	    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
	        shell.setVariable(bean.name, null);
	    }

	    /**
	     * @return a newly created GroovyShell using the same variable scope but a new class loader
	     */
	    protected GroovyShell getEvalShell() {
	        return new GroovyShell(shell);
	    }
	    
	    /**
		 * Reads the entire content of a given file into a String and returns it.
		 * @param filePath - dotCMS file path
		 * @return
	     * @throws DotDataException 
	     * @throws NoSuchUserException 
		 * @throws IOException
		 * @throws DotDataException 
		 * @throws NoSuchUserException 
		 */
	    private InputStream loadFile(String filePath) throws DotSecurityException, FileNotFoundException, NoSuchUserException, DotDataException{
			Host h = APILocator.getHostAPI().findByName(filePath.substring(0,filePath.indexOf("DOTHOST")), userAPI.getSystemUser(), true);
			String fp = filePath.substring(filePath.indexOf('/'),filePath.length());
			Identifier ident = APILocator.getIdentifierAPI().find(h, fp);
			
			String uri = LiveCache.getPathFromCache(ident.getURI(), h);

			String inode = UtilMethods.getFileName(new File(Config.CONTEXT.getRealPath(assetPath + uri)).getName());
			com.dotmarketing.portlets.files.model.File file = fileAPI.find(inode,userAPI.getSystemUser(),false);

			User mu = userAPI.loadUserById(file.getModUser(), userAPI.getSystemUser(), true);
			if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
				throw new DotSecurityException("Last Mod User does not have Scripting Developer role");
			}
			if(!APILocator.getRoleAPI().doesUserHaveRole(mu, APILocator.getRoleAPI().loadRoleByKey("Scripting Developer"))){
				throw new DotSecurityException("Last Mod User does not have Scripting Developer role");
			}

			InputStream is;
			FileReader fr = null;
			if(!UtilMethods.isSet(realPath)){
				is = new BufferedInputStream(new FileInputStream(Config.CONTEXT.getRealPath(assetPath + uri)));
			}else{
				is = new BufferedInputStream(new FileInputStream(realPath + uri));
			}
			return is;
		}
		
		/**
		 * Reads the entire content of a given file into a String and returns it.
		 * @param filePath - dotCMS file path
		 * @return
		 * @throws IOException
		 * @throws DotDataException 
		 * @throws NoSuchUserException 
		 */
	    public static String readFile(String filePath) throws IOException,DotSecurityException, DotDataException {
	    	Host h = APILocator.getHostAPI().findByName(filePath.substring(0,filePath.indexOf("DOTHOST")), userAPI.getSystemUser(), true);
			String fp = filePath.substring(filePath.indexOf('/'),filePath.length());
	    	Identifier ident = APILocator.getIdentifierAPI().find(h, fp);
	    	
			String uri = LiveCache.getPathFromCache(ident.getURI(), h);

			String inode = UtilMethods.getFileName(new File(Config.CONTEXT.getRealPath(assetPath + uri)).getName());
			com.dotmarketing.portlets.files.model.File file = fileAPI.find(inode,userAPI.getSystemUser(),false);

			if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
				throw new DotSecurityException("Last Mod User does not have Scripting Developer role");
			}
			User mu = userAPI.loadUserById(file.getModUser(), userAPI.getSystemUser(), true);
			if(!APILocator.getRoleAPI().doesUserHaveRole(mu, APILocator.getRoleAPI().loadRoleByKey("Scripting Developer"))){
				throw new DotSecurityException("Last Mod User does not have Scripting Developer role");
			}

			FileReader fr = null;
			if(!UtilMethods.isSet(realPath)){
				fr = new FileReader(Config.CONTEXT.getRealPath(assetPath + uri));
			}else{
				fr = new FileReader(realPath + uri);
			}
			BufferedReader br = new BufferedReader(fr);
			String content = "";

			while (true) {
				String line = br.readLine();
				if (line == null) break;
				content += line + '\n';
			}

			return content;
		}
}
