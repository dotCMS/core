package com.dotcms.publisher.environment.business;

import java.util.List;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public interface EnvironmentAPI {

	public Environment findEnvironmentById(String id) throws DotDataException;

	public Environment findEnvironmentByName(String name) throws DotDataException;

	public List<Environment> findEnvironmentsByRole(String roleId) throws DotDataException;

	public void saveEnvironment(Environment e, List<Permission> perms) throws DotDataException, DotSecurityException;

	public List<Environment> findAllEnvironments() throws DotDataException;

	public void deleteEnvironment(String id) throws DotDataException;

	public void updateEnvironment(Environment e) throws DotDataException;

}
