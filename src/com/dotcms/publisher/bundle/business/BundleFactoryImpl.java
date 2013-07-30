package com.dotcms.publisher.bundle.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.util.PublisherUtil;
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
		dc.addParam(bundle.isForcePush());
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

    @Override
    public List<Bundle> findUnsendBundles ( String userId ) throws DotDataException {
        return findUnsendBundles( userId, -1, 0 );
    }

    @Override
    public List<Bundle> findUnsendBundles ( String userId, int limit, int offset ) throws DotDataException {

        List<Bundle> bundles = new ArrayList<Bundle>();

        if ( !UtilMethods.isSet( userId ) ) {
            return bundles;
        }

        DotConnect dc = new DotConnect();
        dc.setSQL( SELECT_UNSEND_BUNDLES );
        dc.addParam( userId );
        dc.setMaxRows( limit );
        dc.setStartRow( offset );

        List<Map<String, Object>> res = dc.loadObjectResults();

        for ( Map<String, Object> row : res ) {
            Bundle bundle = PublisherUtil.getBundleByMap( row );
            bundles.add( bundle );
        }

        return bundles;
    }

    @Override
    public List<Bundle> findUnsendBundlesByName ( String userId, String likeName, int limit, int offset ) throws DotDataException {

        List<Bundle> bundles = new ArrayList<Bundle>();

        if ( !UtilMethods.isSet( userId ) ) {
            return bundles;
        }

        DotConnect dc = new DotConnect();
        dc.setSQL( SELECT_UNSEND_BUNDLES_LIKE_NAME );
        dc.addParam( userId );
        dc.addParam( "%" + likeName + "%" );
        dc.setMaxRows( limit );
        dc.setStartRow( offset );

        List<Map<String, Object>> res = dc.loadObjectResults();

        for ( Map<String, Object> row : res ) {
            Bundle bundle = PublisherUtil.getBundleByMap( row );
            bundles.add( bundle );
        }

        return bundles;
    }

    @Override
    public Bundle getBundleByName ( String bundleName ) throws DotDataException {

        if ( !UtilMethods.isSet( bundleName ) ) {
            return null;
        }

        DotConnect dc = new DotConnect();
        dc.setSQL( SELECT_BUNDLE_BY_NAME );
        dc.addParam( bundleName );

        List<Map<String, Object>> res = dc.loadObjectResults();

        if ( res != null && !res.isEmpty() ) {
            return PublisherUtil.getBundleByMap( res.get( 0 ) );
        }

        return null;
    }

	@Override
	public Bundle getBundleById(String id) throws DotDataException {
		if(!UtilMethods.isSet(id)) {
			return null;
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_BUNDLE_BY_ID);
		dc.addParam(id);

		List<Map<String, Object>> res = dc.loadObjectResults();

		if(res.size()>0)
			return PublisherUtil.getBundleByMap(res.get(0));
		else
			return null;
	}

	@Override
	public void deleteBundle(String id) throws DotDataException {
		if(!UtilMethods.isSet(id)) {
			return;
		}

		deleteBundleEnvironmentByBundle(id);

		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_BUNDLE);
		dc.addParam(id);

		dc.loadResult();

	}

	@Override
	public void updateBundle(Bundle bundle) throws DotDataException {
		if(!UtilMethods.isSet(bundle) || !UtilMethods.isSet(bundle.getId())) {
			return;
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(UPDATE_BUNDLE);
		dc.addParam(bundle.getName());
		dc.addParam(bundle.getPublishDate());
		dc.addParam(bundle.getExpireDate());
		dc.addParam(bundle.getId());

		dc.loadResult();

	}

	@Override
	public void deleteAssetFromBundle(String assetId, String bundleId)
			throws DotDataException {
		if(!UtilMethods.isSet(assetId) || !UtilMethods.isSet(bundleId)) {
			return;
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_ASSET_FROM_BUNDLE);
		dc.addParam(assetId);
		dc.addParam(bundleId);

		dc.loadResult();

	}

	@Override
	public void deleteBundleEnvironmentByEnvironment(String environmentId)
			throws DotDataException {
		if(!UtilMethods.isSet(environmentId)) {
			return;
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_BUNDLE_ENVIRONMENT_BY_ENV);
		dc.addParam(environmentId);

		dc.loadResult();

	}

	@Override
	public void deleteBundleEnvironmentByBundle(String bundleId)
			throws DotDataException {
		if(!UtilMethods.isSet(bundleId)) {
			return;
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_BUNDLE_ENVIRONMENT_BY_BUNDLE);
		dc.addParam(bundleId);

		dc.loadResult();

	}

}
