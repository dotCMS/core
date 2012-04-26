package com.dotcms.achecker;


public interface AChecker {

	public ACheckerResponse validate(ACheckerRequest request) throws Exception;
}
