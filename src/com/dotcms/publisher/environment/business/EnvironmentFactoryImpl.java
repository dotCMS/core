package com.dotcms.publisher.environment.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

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
	public Environment getEnvironmentById(String id)
			throws DotDataException {
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
		try{
			environment.setId(UUID.randomUUID().toString());
			DotConnect dc = new DotConnect();
			dc.setSQL(INSERT_ENVIRONMENT);
			dc.addParam(environment.getId());
			dc.addParam(environment.getName());
			dc.addParam(environment.getPushToAll().booleanValue());
			dc.loadResult();
		}
		catch(DotDataException e) {
			Logger.debug(getClass(), "Unexpected DotDataException in save method", e);
			throw e;
		}

	}

	@Override
	public void update(Environment environment) throws DotDataException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(UPDATE_ENVIRONMENT);
			dc.addParam(environment.getName());
			dc.addParam(environment.getPushToAll().booleanValue());
			dc.addParam(environment.getId());
			dc.loadResult();
		}
		catch(DotDataException e) {
			Logger.debug(getClass(), "Unexpected DotDataException in save method", e);
			throw e;
		}

	}

	@Override
	public void deleteEnvironmentById(String id) throws DotDataException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(DELETE_ENVIRONMENT);
			dc.addParam(id);
			dc.loadResult();
		}
		catch(DotDataException e) {
			Logger.debug(getClass(), "Unexpected DotDataException in delete method", e);
			throw e;
		}

	}

	@Override
	public Environment getEnvironmentByName(String name)
			throws DotDataException {
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


}
