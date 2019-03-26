package com.dotmarketing.portlets.cmsmaintenance.ajax;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;

/**
 * Each of the Fix Tasks will return this object in form of a map to the UI. In order to be iterated
 * in the JSP file and display the stats numbers of each Task ran.
 */
public class FixAssetsProcessStatus implements Serializable {

  private static boolean running = false;
  private static int total = 0;
  private static int actual = 0;
  private static int errorsFixed = 0;
  private static Date initialTime = new Date();
  private static Date finalTime = new Date();
  private static String description = "";

  public static synchronized int getActual() {
    return actual;
  }

  public static synchronized void setActual(int actual) {
    FixAssetsProcessStatus.actual = actual;
  }

  public static synchronized void addActual() {
    actual++;
  }

  public static synchronized int getErrorsFixed() {
    return errorsFixed;
  }

  public static synchronized void setErrorsFixed(int errorsFixed) {
    FixAssetsProcessStatus.errorsFixed = errorsFixed;
  }

  public static synchronized void addAErrorFixed() {
    errorsFixed++;
  }

  public static synchronized Date getFinalTime() {
    return finalTime;
  }

  public static synchronized void setFinalTime(Date finalTime) {
    FixAssetsProcessStatus.finalTime = finalTime;
  }

  public static synchronized Date getInitialTime() {
    return initialTime;
  }

  public static synchronized void setInitialTime(Date initialTime) {
    FixAssetsProcessStatus.initialTime = initialTime;
  }

  public static synchronized int getTotal() {
    return total;
  }

  public static synchronized void setTotal(int total) {
    FixAssetsProcessStatus.total = total;
  }

  public static synchronized void addTotal(int total) {
    FixAssetsProcessStatus.total += total;
  }

  public static synchronized boolean getRunning() {
    return running;
  }

  public static synchronized void setRunning(boolean running) {
    FixAssetsProcessStatus.running = running;
  }

  public static void setDescription(String description) {
    FixAssetsProcessStatus.description = description;
  }

  public static String getDescription() {
    return description;
  }

  /**
   * This method will fill the map used in the UI with each of the properties of the class and other
   * calculations like the elapsed and remaining times.
   *
   * @return Map with the stats numbers of each Fix Task.
   * @throws Exception
   */
  public static synchronized Map getFixAssetsMap() throws Exception {
    try {
      Map<String, Object> theMap = new Hashtable<>();
      theMap.put("description", description);
      theMap.put("total", getTotal());
      theMap.put("actual", getActual());
      theMap.put("errorsFixed", getErrorsFixed());
      Date initialTime = getInitialTime();
      theMap.put("finalTime", getFinalTime());
      theMap.put("running", running);

      float percentage = (getTotal() != 0 ? ((getActual() * 100) / getTotal()) : 100);
      theMap.put("percentage", percentage);

      // Initial Time
      SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MM/dd/yyyy hh:mm:ss");
      theMap.put("initialTime", dateFormat.format(initialTime));

      // Final Time
      theMap.put("finalTime", dateFormat.format(initialTime));

      // Elapsed Time
      long startTime = initialTime.getTime();
      long currentTime = new Date().getTime();
      dateFormat = new SimpleDateFormat("HH:mm:ss");
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      long elapsed = currentTime - startTime;
      theMap.put("elapsed", dateFormat.format(new Date(elapsed)));

      // Remaining Time
      long elapsepItems = getActual();
      long remainingItems = getTotal() - elapsepItems;
      double remainingTime = (elapsepItems != 0 ? ((remainingItems * elapsed) / elapsepItems) : 0);
      theMap.put("remaining", dateFormat.format(new Date((long) remainingTime)));

      return theMap;
    } catch (Exception ex) {
      throw ex;
    }
  }

  public static synchronized void startProgress() {
    setRunning(true);
    reset();
    setInitialTime(new Date());
  }

  public static synchronized void stopProgress() {
    setRunning(false);
    reset();
    setFinalTime(new Date());
  }

  private static synchronized void reset() {
    setTotal(0);
    setActual(0);
    setErrorsFixed(0);
  }
}
