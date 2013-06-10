package com.dotcms.publisher.environment.business;

import java.util.List;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;

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
	public void saveEnvironment(Environment environment) throws DotDataException {
		environmentFactory.save(environment);

	}

	@Override
	public List<Environment> findAllEnvironments() throws DotDataException {
		return environmentFactory.getEnvironments();
	}

	@Override
	public void deleteEnvironment(String id) throws DotDataException {
		environmentFactory.deleteEnvironmentById(id);

	}

	@Override
	public void updateEnvironment(Environment environment) throws DotDataException {
		environmentFactory.update(environment);

	}

}
