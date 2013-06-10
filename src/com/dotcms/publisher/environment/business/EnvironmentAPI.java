package com.dotcms.publisher.environment.business;

import java.util.List;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.exception.DotDataException;

public interface EnvironmentAPI {

	public Environment findEnvironmentById(String id) throws DotDataException;

	public void saveEnvironment(Environment e) throws DotDataException;

	public List<Environment> findAllEnvironments() throws DotDataException;

	public void deleteEnvironment(String id) throws DotDataException;

	public void updateEnvironment(Environment e) throws DotDataException;

}
