package com.dotcms.enterprise.achecker;


public interface AChecker {

	public ACheckerResponse validate(ACheckerRequest request) throws Exception;
}
