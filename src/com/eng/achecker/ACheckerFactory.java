package com.eng.achecker;

import com.eng.achecker.impl.ACheckerImpl;

public class ACheckerFactory {
	
	public AChecker getChecker() {
		return new ACheckerImpl();
	}

}
