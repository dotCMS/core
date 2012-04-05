package com.eng.achecker.parsing;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.nfunk.jep.EvaluatorVisitor;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;

public class ExpressionIterable implements Iterable<Object>, Iterator<Object> {
	
	private String expression;
	
	private EvaluatorVisitor visitor;
	
	private JEP parser;

	private boolean canContinue;
	
	private int offset;
	
	private boolean onlyIfStatement;
	
	private Object lastValue;
	
	public ExpressionIterable(JEP parser, EvaluatorVisitor visitor, String expression) {
		this.expression = normalizeExpression(expression);
		this.parser = parser;
		this.visitor = visitor;
		this.canContinue = true;
		this.onlyIfStatement = false;
		this.lastValue = null;
		this.offset = 0;
	}
	
	public Iterator<Object> iterator() {
		return this;
	}

	public boolean hasNext() {
		return canContinue;
	}

	public Object next() {
		
		String nextExpression;
		try {
			nextExpression = getNextExpression();
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(0);
			return null;
		}
		
		if ( StringUtils.isBlank(expression) )
			return lastValue;
		
		if ( offset >= expression.length() )
			canContinue = false;
		
		Boolean currentValue = null;
		
		// System.err.println("EXP: " + nextExpression);
		
		// Parse expression tree
		try {
			Node node = parser.parse(nextExpression);
			// System.out.println(">" + nextExpression);
			Object currentResult = visitor.getValue(node, parser.getSymbolTable());
			currentValue = adjustResult(currentResult);
			if ( onlyIfStatement && currentValue != null && currentValue == Boolean.TRUE ) {
				return currentValue;
			}
			lastValue = currentResult;
		}
		catch (Exception e ) {
			e.printStackTrace();
			System.exit(0);
		}
		
		return adjustResult(currentValue);
	}

	public void remove() {
		throw new IllegalStateException("Not supported");
	}
	
	private int indexOfNextSemicolon(int startOffset) {
		boolean escapedDouble = false;
		boolean escapedSingle = false;
		for ( int i = startOffset; i < expression.length(); i ++ ) {
			switch (expression.charAt(i)) {
				case '"' : escapedDouble = !escapedDouble; break;
				case '\'' : escapedSingle = !escapedSingle; break;
				case ';' : 
					if (escapedDouble) continue;
					if (escapedSingle) continue;
					return i;
				default:
					break;
			}
		}
		return expression.length();
	}
			
	private String getNextExpression() throws Exception {
		this.onlyIfStatement = false;
		int oldOffset = offset;
		int nextSemicolonIndex = indexOfNextSemicolon(oldOffset);
		offset = nextSemicolonIndex + 1;
		String statement = expression.substring(oldOffset, nextSemicolonIndex);
		statement = statement.trim();
		if ( statement.contains("if ")) {

			// Verify if this if has an else following
			int nextSemicolonIndex2 = indexOfNextSemicolon(nextSemicolonIndex + 1);
			String statement2 = expression.substring(nextSemicolonIndex + 1, nextSemicolonIndex2);
			statement2 = statement2.trim();
			if ( statement2.contains("else ")) {
				
				offset = nextSemicolonIndex2 + 1;
				String ifElseStatement = statement + ";" + statement2 + ";";
				
				Pattern pattern = Pattern.compile(".*?(if[\\s]*[(](.*?)[)][\\s]*return(.*?);[\\s]*else[\\s]+return(.*?);).*");
				Matcher matcher = pattern.matcher(ifElseStatement);

				if ( matcher.matches() ) {
					String all = matcher.group(1);
					String condition = matcher.group(2);
					String trueExpression = matcher.group(3);
					String falseExpression = matcher.group(4);
					String value = ifElseStatement.replace(all, "if(" + condition.trim() + ", " + trueExpression.trim() + ", " + falseExpression.trim() + ");" );
					value = value.replace("return ", "");
					return value;
				}
				else {
					throw new Exception("bad if/else expression");
				}
				
			}
			else {
				throw new Exception("bad if/else expression");
			}
		}
		else {
			statement = statement.replace("return ", "");
		}
		
		return statement;
	}
	
	/**
	 * Normalize expression for end-of-line, method invocation,
	 * comparison symbol "<>" and comma.
	 * 
	 * @param expression
	 * @return
	 */
	private String normalizeExpression(String expression) {
		expression = expression.replaceAll("\\r\\n", " ");
		expression = expression.replaceAll("\\r", " ");
		expression = expression.replaceAll("\\n", " ");
		expression = expression.replaceAll("::", "\\$\\$");
		expression = expression.replaceAll("'", "\"");
		expression = expression.replaceAll("<>", "!=");
		expression = expression.trim();
		return expression;
	}

//	public static String phpToJavaExpression(String expression) {
//		
//
//		// replace if else
//		
//		int indexIf = expression.indexOf("if");
//		int indexElse = expression.indexOf("else");
//		if ( indexIf != -1 &&  indexElse != -1 ) {
//
//			Pattern pattern = Pattern.compile(".*?(if[\\s]*[(](.*?)[)][\\s]*return(.*?);[\\s]*else[\\s]+return(.*?);).*");
//			Matcher matcher = pattern.matcher(expression);
//
//			if ( matcher.matches() ) {
//				String all = matcher.group(1);
//				String condition = matcher.group(2);
//				String trueExpression = matcher.group(3);
//				String falseExpression = matcher.group(4);
//				expression = expression.replace(all, "if(" + condition.trim() + ", " + trueExpression.trim() + ", " + falseExpression.trim() + ");" );
//			}
//			else {
//
//				System.err.println("Capture");
//
//			}
//			
//		}
//		
//		expression = expression.replace("return ", "");
//		
//		return expression;
//	}

	private Boolean adjustResult(Object check_result) {
		if ( check_result == null )
			return null;
		if ( check_result instanceof Boolean )
			return (Boolean) check_result;
		if ( check_result instanceof Number ) {
			Number num = (Number) check_result;
			if (num.doubleValue() == 0.0)
				return Boolean.FALSE;
			return Boolean.TRUE;
		}
		return null;
	}
}
