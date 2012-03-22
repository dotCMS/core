package com.dotcms.sync.exception;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.sync.Exportable;

public class DotDependencyException extends Exception {

	
	private List<Exportable> unmet = new ArrayList<Exportable>();
	
	
	
}
