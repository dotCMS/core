package com.dotcms.publisher.environment.business;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public class EnvironmentFactoryImpl extends EnvironmentFactory {

	@Override
	public List<Environment> getEnvironments() throws DotDataException {
		List<Environment> environments = new ArrayList<Environment>();
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ALL_ENVIRONMENTS);
		List<Map<String, Object>> res = dc.loadObjectResults();

		for(Map<String, Object> row : res){
			Environment environment = PublisherUtil.getEnvironmentByMap(row);
			environments.add(environment);
		}

		return environments;
	}

	@Override
	public List<Environment> getEnvironmentsWithServers() throws DotDataException {
		List<Environment> environments = new ArrayList<Environment>();
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ENVIRONMENTS_WITH_SERVERS);
		List<Map<String, Object>> res = dc.loadObjectResults();

		for(Map<String, Object> row : res){
			Environment environment = PublisherUtil.getEnvironmentByMap(row);
			environments.add(environment);
		}

		return environments;
	}

	@Override
	public Environment getEnvironmentById(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ENVIRONMENT_BY_ID);
		dc.addParam(id);
		List<Map<String, Object>> res = dc.loadObjectResults();
		Environment e = null;

		if(res!=null && !res.isEmpty()) {
			Map<String, Object> row = res.get(0);
			e = PublisherUtil.getEnvironmentByMap(row);
		}

		return e;
	}

	@Override
	public void save(Environment environment) throws DotDataException {
		environment.setId(UUID.randomUUID().toString());
		DotConnect dc = new DotConnect();
		dc.setSQL(INSERT_ENVIRONMENT);
		dc.addParam(environment.getId());
		dc.addParam(environment.getName());
		dc.addParam(environment.getPushToAll().booleanValue());
		dc.loadResult();

	}

	@Override
	public void update(Environment environment) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(UPDATE_ENVIRONMENT);
		dc.addParam(environment.getName());
		dc.addParam(environment.getPushToAll().booleanValue());
		dc.addParam(environment.getId());
		dc.loadResult();
	}

	@Override
	public void deleteEnvironmentById(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_ENVIRONMENT);
		dc.addParam(id);
		dc.loadResult();
	}

	@Override
	public Environment getEnvironmentByName(String name) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ENVIRONMENT_BY_NAME);
		dc.addParam(name);
		List<Map<String, Object>> res = dc.loadObjectResults();
		Environment e = null;

		if(res!=null && !res.isEmpty()) {
			Map<String, Object> row = res.get(0);
			e = PublisherUtil.getEnvironmentByMap(row);
		}

		return e;
	}

	@Override
	public Set<Environment> getEnvironmentsByRole(String roleId) throws DotDataException, NoSuchUserException, DotSecurityException {
		Set<Environment> environments = new HashSet<Environment>();
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ENVIRONMENTS_BY_ROLE_ID);
		dc.addParam(roleId);
		List<Map<String, Object>> res = dc.loadObjectResults();
		for(Map<String, Object> row : res){
			Environment environment = PublisherUtil.getEnvironmentByMap(row);
			environments.add(environment);
		}
		return environments;
	}

	@Override
	public List<Environment> getEnvironmentsByBundleId(String bundleId)
			throws DotDataException {
		List<Environment> environments = new ArrayList<Environment>();
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ENVIRONMENTS_BY_BUNDLE_ID);
		dc.addParam(bundleId);
		List<Map<String, Object>> res = dc.loadObjectResults();

		for(Map<String, Object> row : res){
			Environment environment = PublisherUtil.getEnvironmentByMap(row);
			environments.add(environment);
		}

		return environments;
	}


}
