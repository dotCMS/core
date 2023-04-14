package com.dotcms.enterprise;

import com.dotcms.enterprise.license.LicenseLevel;
import com.liferay.portal.auth.AuthException;

public class AuthPipeProxy extends ParentProxy {

	public static int authenticateByEmailAddress(String[] classes,
			String companyId, String emailAddress, String password)
			throws AuthException {
		if (classes.length > 0 && !allowExecution(getStaticAllowedVersions() )) {
			return 0;
		}
		return AuthPipelineImpl.authenticateByEmailAddress(classes, companyId,
				emailAddress, password);
	}

	public static int authenticateByUserId(String[] classes, String companyId,
			String userId, String password) throws AuthException {
		if (classes.length > 0 && !allowExecution(getStaticAllowedVersions() )) {
			return 0;
		}
		return AuthPipelineImpl.authenticateByUserId(classes, companyId,
				userId, password);
	}

	public static void onFailureByEmailAddress(String[] classes,
			String companyId, String emailAddress) throws AuthException {
		if (classes.length > 0 && !allowExecution(getStaticAllowedVersions() )) {
			return;
		}
		AuthPipelineImpl.onFailureByEmailAddress(classes, companyId,
				emailAddress);
	}

	public static void onFailureByUserId(String[] classes, String companyId,
			String userId) throws AuthException {
		if (classes.length > 0 && !allowExecution(getStaticAllowedVersions() )) {
			return;
		}
		AuthPipelineImpl.onFailureByUserId(classes, companyId, userId);
	}

	public static void onMaxFailuresByEmailAddress(String[] classes,
			String companyId, String emailAddress) throws AuthException {
		if (classes.length > 0 && !allowExecution(getStaticAllowedVersions() )) {
			return;
		}
		AuthPipelineImpl.onMaxFailuresByEmailAddress(classes, companyId,
				emailAddress);
	}

	public static void onMaxFailuresByUserId(String[] classes,
			String companyId, String userId) throws AuthException {
		if (classes.length > 0 && !allowExecution(getStaticAllowedVersions() )) {
			return;
		}
		AuthPipelineImpl.onMaxFailuresByUserId(classes, companyId, userId);
	}
	
	private static int[] getStaticAllowedVersions() {
		int[] ret = {LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level, LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level };
		return ret;
	}

	

}
