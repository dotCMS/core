package com.dotcms.publisher.bundle.business;

import java.util.UUID;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

public class BundleFactoryImpl extends BundleFactory {

	@Override
	public void saveBundle(Bundle bundle) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(INSERT_BUNDLE);
		bundle.setId(UUID.randomUUID().toString());
		dc.addParam(bundle.getId());
		dc.addParam(UtilMethods.isSet(bundle.getName())?bundle.getName():bundle.getId());
		dc.addParam(bundle.getPublishDate());
		dc.addParam(bundle.getExpireDate());
		dc.addParam(bundle.getOwner());
		dc.loadResult();
	}

	@Override
	public void saveBundleEnvironment(Bundle bundle, Environment e) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(INSERT_BUNDLE_ENVIRONMENT);
		dc.addParam(UUID.randomUUID().toString());
		dc.addParam(bundle.getId());
		dc.addParam(e.getId());
		dc.loadResult();
	}

}