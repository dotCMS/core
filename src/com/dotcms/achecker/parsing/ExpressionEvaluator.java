package com.dotcms.achecker.parsing;


import java.lang.reflect.Method;

import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.EvaluatorVisitor;
import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;

import com.dotcms.achecker.validation.BasicFunctions;
import com.dotcms.achecker.validation.CustomChecks;
import com.dotcms.achecker.validation.FunctionRepository;
import com.dotcms.achecker.validation.UtilityFunctions;
//import com.dotcms.achecker.parsing.CommandAdapter;
//import com.dotcms.achecker.parsing.ExpressionIterable;

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

