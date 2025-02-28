/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.test;

import com.dotcms.enterprise.achecker.parsing.ExpressionEvaluator;

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
