package com.dotcms.publisher.environment.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Set;

public class EnvironmentAPIImpl implements EnvironmentAPI {

	private EnvironmentFactory environmentFactory;

	public EnvironmentAPIImpl() {
		environmentFactory = FactoryLocator.getEnvironmentFactory();
	}

	@CloseDBIfOpened
	@Override
	public Environment findEnvironmentById(String id) throws DotDataException {
		if(!UtilMethods.isSet(id))
			return null;

		return environmentFactory.getEnvironmentById(id);
	}

	@WrapInTransaction
	@Override
	public void saveEnvironment(Environment environment, List<Permission> perms) throws DotDataException, DotSecurityException {

		if(!UtilMethods.isSet(environment))
			return;

		environmentFactory.save(environment);
		APILocator.getPermissionAPI().removePermissions(environment);

		if(perms != null){
			for (Permission p : perms) {
				p.setInode(environment.getId());
				APILocator.getPermissionAPI().save(p, environment, APILocator.getUserAPI().getSystemUser(), false);
			}
		}
	}

	@CloseDBIfOpened
	@Override
	public List<Environment> findAllEnvironments() throws DotDataException {
		return environmentFactory.getEnvironments();
	}

	@CloseDBIfOpened
	@Override
	public List<Environment> findEnvironmentsWithServers() throws DotDataException {
		return environmentFactory.getEnvironmentsWithServers();
	}

	@WrapInTransaction
	@Override
	public void deleteEnvironment(String id) throws DotDataException {

		if(!UtilMethods.isSet(id))
			return;

		// remove the endpoints of the environment
		
		List<PublishingEndPoint> endPoints = APILocator.getPublisherEndPointAPI().findSendingEndPointsByEnvironment(id);
        
        for (PublishingEndPoint ep : endPoints) {
            //Delete endpoints associated to this Environment
            APILocator.getPublisherEndPointAPI().deleteEndPointById(ep.getId());
        }

		Environment e = findEnvironmentById(id);

		APILocator.getPermissionAPI().removePermissions(e);

		// delete bundle-environment relationships

		FactoryLocator.getBundleFactory().deleteBundleEnvironmentByEnvironment(id);

		// delete related pushed-assets history

		FactoryLocator.getPushedAssetsFactory().deletePushedAssetsByEnvironment(id);

		environmentFactory.deleteEnvironmentById(id);
	}

	@WrapInTransaction
	@Override
	public void updateEnvironment(Environment environment, List<Permission> perms ) throws DotDataException, DotSecurityException {
		environmentFactory.update(environment);

		APILocator.getPermissionAPI().removePermissions(environment);

		if(perms != null){
			for (Permission p : perms) {
				p.setInode(environment.getId());
				APILocator.getPermissionAPI().save(p, environment, APILocator.getUserAPI().getSystemUser(), false);
			}
		}

	}

	@CloseDBIfOpened
	@Override
	public Environment findEnvironmentByName(String name) throws DotDataException {
		return environmentFactory.getEnvironmentByName(name);
	}

	@CloseDBIfOpened
	@Override
	public Set<Environment> findEnvironmentsByRole(String roleId) throws DotDataException, NoSuchUserException, DotSecurityException {
		return environmentFactory.getEnvironmentsByRole(roleId);
	}

	@CloseDBIfOpened
	@Override
	public List<Environment> findEnvironmentsByBundleId(String bundleId) throws DotDataException {
		return environmentFactory.getEnvironmentsByBundleId(bundleId);
	}

}
