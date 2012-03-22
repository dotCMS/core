package com.dotmarketing.compilers;

import java.util.ArrayList;
import java.util.List;


/**
 * Holds a list of compilation problems (errors and/or warnings)
 * @author davidtorresv
 *
 */
public class DotCompilationProblems {
	
	private List<DotCompilationProblem> problems = new ArrayList<DotCompilationProblem>();
	
	DotCompilationProblems(List<DotCompilationProblem> problems) {
		this.problems = problems;
	}

	public List<DotCompilationProblem> getProblems() {
		return problems;
	}
	
	public boolean hasCompilationErrors() {
		for(DotCompilationProblem problem : problems) {
			if(problem.isError())
				return true;
		}
		return false;
	}

	public boolean hasCompilationWarnings() {
		for(DotCompilationProblem problem : problems) {
			if(problem.isWarning())
				return true;
		}
		return false;
	}
	
	public List<DotCompilationProblem> getCompilationErrors () {
		List<DotCompilationProblem> errors = new ArrayList<DotCompilationProblem>(problems.size());
		for(DotCompilationProblem problem : problems) {
			if(problem.isError())
				errors.add(problem);
		}
		return errors;
	}
	
	public List<DotCompilationProblem> getCompilationWarnings () {
		List<DotCompilationProblem> errors = new ArrayList<DotCompilationProblem>(problems.size());
		for(DotCompilationProblem problem : problems) {
			if(problem.isWarning())
				errors.add(problem);
		}
		return errors;
	}

	@Override
	public String toString() {
		
		return problems.toString();
	}	
	
	
}
