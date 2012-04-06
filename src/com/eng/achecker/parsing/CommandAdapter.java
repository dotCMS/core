package com.eng.achecker.parsing;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.eng.achecker.validation.FunctionRepository;

public class CommandAdapter extends PostfixMathCommand {
	
	private FunctionRepository delegate;
	
	private JEP parser;
	
	private Method method;
	
	public CommandAdapter(JEP parser, FunctionRepository delegate, String name) {
		super();
		
		this.parser = parser;
		this.delegate = delegate;
		
		for ( Method method : delegate.getClass().getMethods()) {
			if ( method.getName().equals(name) ) {
				this.method = method;
				numberOfParameters = this.method.getParameterTypes().length;
				return;
			}
		}

		throw new IllegalArgumentException("Method not found: " + name);
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run(Stack inStack) throws ParseException {
				
		// check the stack
		checkStack(inStack);

		// Prepare global variable for function 
		delegate.setGlobalVariable("global_e", parser.getVarValue("global_e"));
		delegate.setGlobalVariable("global_check_id", parser.getVarValue("global_check_id"));
		delegate.setGlobalVariable("global_content_dom", parser.getVarValue("global_content_dom"));
		
		try {

			if ( numberOfParameters == 0 ) {
				Object ret = method.invoke(delegate);
				if ( ret != null )
					inStack.push(ret);
			}
			else {
				if ( numberOfParameters == -1 ) {
					int realSize = inStack.size();
					List<Object> list = new ArrayList<Object>(realSize);
					for ( int i = 0; i < realSize; i ++ )
						list.add(inStack.pop());
					Object ret = method.invoke(delegate, list);
					if ( ret != null )
						inStack.push(ret);
				}
				else {
					Object[] params = new Object[numberOfParameters];
					for ( int i = 0; i < numberOfParameters; i ++ ) {
						params[numberOfParameters - i - 1] = inStack.pop();
					}
					Object ret = method.invoke(delegate, params);
					if ( ret != null ) {
						inStack.push(ret);
					}
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw new ParseException("Error in method: " + t.getMessage());
		}

	}

}
