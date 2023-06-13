package com.dotcms.enterprise.achecker;

import com.dotcms.enterprise.achecker.impl.ACheckerImpl;

public class ACheckerFactory {
	
	public AChecker getChecker() {
		return new ACheckerImpl();
	}

}
