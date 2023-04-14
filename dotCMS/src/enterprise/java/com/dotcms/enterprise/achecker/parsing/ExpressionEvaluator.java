/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.achecker.parsing;


import java.lang.reflect.Method;

import com.dotcms.repackage.org.nfunk.jep.ASTVarNode;
import com.dotcms.repackage.org.nfunk.jep.EvaluatorVisitor;
import com.dotcms.repackage.org.nfunk.jep.JEP;
import com.dotcms.repackage.org.nfunk.jep.ParseException;

import com.dotcms.enterprise.achecker.validation.BasicFunctions;
import com.dotcms.enterprise.achecker.validation.CustomChecks;
import com.dotcms.enterprise.achecker.validation.FunctionRepository;
import com.dotcms.enterprise.achecker.validation.UtilityFunctions;
//import com.dotcms.enterprise.achecker.parsing.CommandAdapter;
//import com.dotcms.enterprise.achecker.parsing.ExpressionIterable;

public class ExpressionEvaluator {

	private JEP parser;
	
	private EvaluatorVisitor visitor;
	
	private void addDelegateMethods(String prefix, FunctionRepository delegate) {
		for (Method method : delegate.getClass().getMethods()) {
			CommandAdapter adapter = new CommandAdapter(parser, delegate, method.getName());
			parser.addFunction(prefix + method.getName(), adapter);
		}
	}
	
	public void setVariable(String name, Object value) {
		if ( this.parser.getVar(name) == null )
			this.parser.addVariable(name, value);
		else
			this.parser.setVarValue(name, value);
	}
	
	public ExpressionEvaluator() {
		
		this.parser = new JEP();
	
		parser.addStandardFunctions();
		parser.setAllowUndeclared(true);
		parser.setAllowAssignment(true);
		parser.setImplicitMul(true);
				
		setVariable("true", Boolean.TRUE);
		setVariable("false", Boolean.FALSE);
		
		addDelegateMethods("BasicFunctions$$", new BasicFunctions());
		
		addDelegateMethods("CustomChecks$$", new CustomChecks());
		
		// Utility functions (wrapper to php constructors like 'array')
		addDelegateMethods("", new UtilityFunctions());

		this.visitor = new EvaluatorVisitor() {
		
			public Object visit(ASTVarNode var, Object data) throws ParseException {
				
				Object value = this.symTab.getValue(var.getName());
				
				if ( value == null )
					this.symTab.setVarValue(var.getName(), 0.0);
				
				return super.visit(var, data);
				
			};
						
		};
	}
	
	public Object evaluate(String expression) {
		ExpressionIterable e = new ExpressionIterable(parser, visitor, expression);
		Object result = null;
		for ( Object r : e ) { result = r; }
		return result;
	}

	public void dumpVariables() {
		System.out.println("--- Variables ----");
		for ( Object key : parser.getSymbolTable().keySet()) {
			System.out.println("\t * " + key + " = " + parser.getSymbolTable().getValue(key));
		}
		System.out.println("------------------");
	}
}

