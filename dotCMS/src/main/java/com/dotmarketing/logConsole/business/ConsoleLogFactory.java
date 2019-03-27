package com.dotmarketing.logConsole.business;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.logConsole.model.LogMapperRow;


public interface ConsoleLogFactory {

	public List convertListToObjects(List<Map<String, Object>> rs, Class clazz) throws DotDataException;

	public Object convertMaptoObject(Map<String, Object> map, Class clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException;

	public List<LogMapperRow> findLogMapper() throws DotDataException;
	
	public void updateLogMapper(LogMapperRow r) throws DotDataException;

}
