/**
 *  This is a Dummy implementation to test CMIS
 */
package com.dotcms.cmis;

import java.math.BigInteger;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;

public class DotCallContextObjectHandler implements CallContext, ObjectInfoHandler{

	@Override
	public Object get(String arg0) {
		return "";
	}

	@Override
	public String getBinding() {
		return "atompub";
	}

	@Override
	public BigInteger getLength() {
		return null;
	}

	@Override
	public String getLocale() {
		return null;
	}

	@Override
	public int getMemoryThreshold() {
		return 4194304;
	}

	@Override
	public BigInteger getOffset() {
		return null;
	}

	@Override
	public String getPassword() {
		try {
			return PublicEncryptionFactory.decryptString(APILocator.getUserAPI().getSystemUser().getPassword());
		} catch (DotDataException e) {
			return "";
		}
	}

	@Override
	public String getRepositoryId() {
		return "dotcms";
	}

	@Override
	public java.io.File getTempDirectory() {
		return new java.io.File(System.getProperty("java.io.tmpdir"));
	}

	@Override
	public String getUsername() {
		try {
			return APILocator.getUserAPI().getSystemUser().getEmailAddress();
		} catch (DotDataException e) {
			return "";
		}
	}

	@Override
	public boolean isObjectInfoRequired() {
		return true;
	}

	@Override
	public void addObjectInfo(ObjectInfo arg0) {
		return;
	}

	@Override
	public ObjectInfo getObjectInfo(String arg0, String arg1) {
		return null;
	}
}
