package com.dotmarketing.logConsole.model;

import java.util.Iterator;
import java.util.List;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.logConsole.business.ConsoleLogFactory;
import com.dotmarketing.logConsole.business.ConsoleLogFactoryImpl;
import com.dotmarketing.logConsole.business.ConsoleLoggerSQL;
import com.dotmarketing.logConsole.model.LogMapperRow;
import com.dotmarketing.util.Logger;

public class LogMapper {

	private static LogMapper instance = null;
	private List<LogMapperRow> logList = null;

	private ConsoleLogFactory clf = new ConsoleLogFactoryImpl();

	private LogMapper() {

		try {
			logList = clf.findLogMapper();
		} catch (DotDataException e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
	}

	public static LogMapper getInstance() {
		if(instance == null) {
			instance = new LogMapper();
		}
		return instance;
	}

	public List<LogMapperRow> getLogList() {
		return logList;
	}

	public boolean isLogEnabled(String filename) {
		for (Iterator iterator = this.logList.iterator(); iterator.hasNext();) {
			LogMapperRow lmr = (LogMapperRow) iterator.next();
			if(filename.equals(lmr.getLog_name()) && lmr.getEnabled() == 1) {
				return true;
			}
		}
		return false;
	}

	public void updateLogsList() {

		for (Iterator iterator = logList.iterator(); iterator.hasNext();) {
			LogMapperRow lmr = (LogMapperRow) iterator.next();
			try {
				clf.updateLogMapper(lmr);
			} catch (DotDataException e) {
				Logger.error(this.getClass(), e.getMessage(), e);
			}
		}

	}

}
