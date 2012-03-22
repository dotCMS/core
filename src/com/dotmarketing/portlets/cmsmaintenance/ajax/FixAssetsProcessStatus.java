package com.dotmarketing.portlets.cmsmaintenance.ajax;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;

public class FixAssetsProcessStatus implements Serializable {
	private static boolean running = false;
	private static int total = 0;
	private static int actual = 0;
	private static int error = 0;
	private static Date initialTime = new Date();
	private static Date finalTime = new Date();
	private static String description= "";
	public synchronized static int getActual() {
		return actual;
	}
	public synchronized static void setActual(int actual) {
		FixAssetsProcessStatus.actual = actual;
	}
	public synchronized static int getError() {
		return error;
	}
	public synchronized static void setError(int error) {
		FixAssetsProcessStatus.error = error;
	}
	public synchronized static Date getFinalTime() {
		return finalTime;
	}
	public synchronized static void setFinalTime(Date finalTime) {
		FixAssetsProcessStatus.finalTime = finalTime;
	}
	public synchronized static Date getInitialTime() {
		return initialTime;
	}
	public synchronized static void setInitialTime(Date initialTime) {
		FixAssetsProcessStatus.initialTime = initialTime;
	}
	public synchronized static int getTotal() {
		return total;
	}
	public synchronized static void setTotal(int total) {
		FixAssetsProcessStatus.total = total;
	}

	public synchronized static void addActual()
	{
		actual++;
	}

	public synchronized static void addAError()
	{
		error++;
	}

	public synchronized static boolean getRunning() {
		return running;
	}
	public synchronized static void setRunning(boolean running) {
		FixAssetsProcessStatus.running = running;
	}

	public synchronized static Map getFixAssetsMap() throws Exception 
	{
		try
		{
			Map<String, Object> theMap = new Hashtable<String, Object> ();
			theMap.put("description",description);
			theMap.put("total",getTotal());
			long actual = (long) getActual();
			theMap.put("actual",getActual());
			theMap.put("error",getError());
			Date initialTime = getInitialTime();
			theMap.put("finalTime",getFinalTime());     
			theMap.put("running",running);

			float percentage = (getTotal() != 0 ? ((getActual() * 100) / getTotal()) : 100);
			theMap.put("percentage",percentage);
			
			//Initial Time
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MM/dd/yyyy hh:mm:ss");
			theMap.put("initialTime",dateFormat.format(initialTime));
			
			//Final Time
			theMap.put("finalTime",dateFormat.format(initialTime));

			//Elapsed Time
			long startTime = initialTime.getTime();
			long currentTime = new Date().getTime();
			dateFormat = new SimpleDateFormat("HH:mm:ss");
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			long elapsed = currentTime - startTime;
			theMap.put("elapsed",dateFormat.format(new Date(elapsed)));

			//Remaining Time
			long elapsepItems = getActual();
			long remainingItems = getTotal() - elapsepItems;
			double remainingTime = (elapsepItems != 0 ? ((remainingItems * elapsed) / elapsepItems) : 0);        
			theMap.put("remaining",dateFormat.format(new Date((long) remainingTime)));

			return theMap;
		}
		catch(Exception ex)
		{
			String message = ex.toString();
			throw ex;
		}
	}
	
	public static synchronized void startProgress()
	{
		setRunning(true);
		reset();
		setInitialTime(new Date());
	}
	
	public static synchronized void stopProgress()
	{
		setRunning(false);
		reset();
		setFinalTime(new Date());
	}
	
	private static synchronized void reset()
	{
		setTotal(0);
		setActual(0);
		setError(0);
	}
	public static void setDescription(String description) {
		FixAssetsProcessStatus.description = description;
	}
	public static String getDescription() {
		return description;
	}
	
}