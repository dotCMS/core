package com.dotcms.enterprise;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.liferay.portal.model.User;
import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import org.xml.sax.SAXException;

public class HTMLDiffUtilProxy extends ParentProxy {

	public String htmlDiffPage(final IHTMLPage page,
							   final User user,
							   final String contentId,
							   final long languageId) throws IOException, SAXException,

	TransformerConfigurationException, DotSecurityException, DotDataException {
		if (allowExecution()) {
			return HTMLDiffUtil.htmlDiffPage(page, user, contentId, languageId);
		} else {
			return "";
		}

	}

	@Override
	protected int[] getAllowedVersions() {
		return new int[] { LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level, LicenseLevel.PRIME.level,
				LicenseLevel.PLATFORM.level };
	}

}
