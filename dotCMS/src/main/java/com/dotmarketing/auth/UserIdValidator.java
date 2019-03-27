package com.dotmarketing.auth;

public interface UserIdValidator {

	public boolean validate(String userId, String companyId);
}
