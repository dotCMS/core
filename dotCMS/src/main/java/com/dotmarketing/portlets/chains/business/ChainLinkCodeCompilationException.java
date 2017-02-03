package com.dotmarketing.portlets.chains.business;

import com.dotmarketing.compilers.DotCompilationProblems;

public class ChainLinkCodeCompilationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8619495783114515368L;
	
	private DotCompilationProblems problems; 
	
	public ChainLinkCodeCompilationException(String message, DotCompilationProblems problems) {
		super(message);
		this.setProblems(problems);
	}

	protected void setProblems(DotCompilationProblems problems) {
		this.problems = problems;
	}

	public DotCompilationProblems getProblems() {
		return problems;
	}

	@Override
	public String toString() {
		
		return problems.toString();
		
	}
	
	
}
