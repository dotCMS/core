package com.dotcms.achecker;

import com.dotcms.achecker.impl.ACheckerImpl;

public class ACheckerFactory {
	
	public AChecker getChecker() {
		return new ACheckerImpl();
	}

}
