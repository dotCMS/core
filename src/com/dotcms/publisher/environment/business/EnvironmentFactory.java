package com.dotcms.publisher.environment.business;

import java.util.List;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.exception.DotDataException;

public abstract class EnvironmentFactory {

	protected static String SELECT_ALL_ENVIRONMENTS = "SELECT * FROM publishing_environment order by name";
	protected static String INSERT_ENVIRONMENT = "INSERT INTO publishing_environment VALUES (?,?,?)";
	protected static String UPDATE_ENVIRONMENT = "UPDATE publishing_environment SET name = ?, push_to_all = ? WHERE id = ?";
	protected static String DELETE_ENVIRONMENT = "DELETE FROM publishing_environment WHERE id = ?";
	protected static String SELECT_ENVIRONMENT_BY_ID = "SELECT * FROM publishing_environment WHERE id = ?";
	protected static String SELECT_ENVIRONMENT_BY_NAME = "SELECT * FROM publishing_environment WHERE name = ?";
	protected static String SELECT_ENVIRONMENTS_BY_ROLE_ID = "SELECT * FROM publishing_environment pe JOIN permission p ON pe.id = p.inode_id WHERE p.roleid = ?";
	protected static String SELECT_ENVIRONMENTS_BY_BUNDLE_ID = "SELECT * FROM publishing_environment pe JOIN publishing_bundle_environment pbe ON pe.id = pbe.environment_id where pbe.bundle_id = ?";


	public abstract List<Environment> getEnvironments() throws DotDataException;

	public abstract Environment getEnvironmentById(String id) throws DotDataException;

	public abstract Environment getEnvironmentByName(String name) throws DotDataException;

	public abstract List<Environment> getEnvironmentsByRole(String roleId) throws DotDataException;

	public abstract void save(Environment environment) throws DotDataException;

	public abstract void update(Environment environment) throws DotDataException;

	public abstract void deleteEnvironmentById(String id) throws DotDataException;

	public abstract List<Environment> getEnvironmentsByBundleId(String bundleId) throws DotDataException;

}
