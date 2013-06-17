package com.dotcms.publisher.environment.business;

import java.util.List;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public class EnvironmentAPIImpl implements EnvironmentAPI {

	private EnvironmentFactory environmentFactory;

	public EnvironmentAPIImpl() {
		environmentFactory = FactoryLocator.getEnvironmentFactory();
	}

	@Override
	public Environment findEnvironmentById(String id) throws DotDataException {
		return environmentFactory.getEnvironmentById(id);
	}

	@Override
	public void saveEnvironment(Environment environment, List<Permission> perms) throws DotDataException, DotSecurityException {
		environmentFactory.save(environment);
		APILocator.getPermissionAPI().removePermissions(environment);

		if(perms != null){
			for (Permission p : perms) {
				p.setInode(environment.getId());
				APILocator.getPermissionAPI().save(p, environment, APILocator.getUserAPI().getSystemUser(), false);
			}
		}


	}

	@Override
	public List<Environment> findAllEnvironments() throws DotDataException {
		return environmentFactory.getEnvironments();
	}

	@Override
	public void deleteEnvironment(String id) throws DotDataException {
		List<PublishingEndPoint> endPoints = APILocator.getPublisherEndPointAPI().findSendingEndPointByEnvironment(id);

		for (PublishingEndPoint ep : endPoints) {
			APILocator.getPublisherEndPointAPI().deleteEndPointById(ep.getId());
		}

		environmentFactory.deleteEnvironmentById(id);

	}

	@Override
	public void updateEnvironment(Environment environment) throws DotDataException {
		environmentFactory.update(environment);

	}

	@Override
	public Environment findEnvironmentByName(String name) throws DotDataException {
		return environmentFactory.getEnvironmentByName(name);
	}

	@Override
	public List<Environment> findEnvironmentsByRole(String roleId) throws DotDataException {
		return environmentFactory.getEnvironmentsByRole(roleId);
	}

}
