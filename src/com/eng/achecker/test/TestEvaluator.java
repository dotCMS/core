package com.eng.achecker.test;

import com.eng.achecker.parsing.ExpressionEvaluator;

public class TestEvaluator {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		ExpressionEvaluator evaluator = new ExpressionEvaluator();
		
		Object ret = evaluator.evaluate("pippo = asso + 33; a = pippo + 1; pippo = 33; pippo + 12");
		
		System.out.println("Ret: " + ret);
		
		evaluator.dumpVariables();

	}

}
