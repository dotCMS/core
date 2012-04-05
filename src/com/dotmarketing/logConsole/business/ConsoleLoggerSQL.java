package com.dotmarketing.logConsole.business;

public class ConsoleLoggerSQL {
	
	public static String SELECT_LOGGING_CRITERIA = " select * from log_mapper ";
	public static String UPDATE_LOGGING_CRITERIA = " update log_mapper set enabled = ? where log_name=?";
	
	

}