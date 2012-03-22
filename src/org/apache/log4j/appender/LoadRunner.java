package org.apache.log4j.appender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

public class LoadRunner implements Runnable {

  /**
   * @param args
   */
  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    List threadList = new ArrayList();
    for (int i = 0; i < 16; i++) {
      Thread t = new Thread(new LoadRunner(i));
      t.setDaemon(true);
      t.start();
      threadList.add(t);
    }
    for (Iterator iter = threadList.iterator(); iter.hasNext();) {
      try {
        ((Thread) iter.next()).join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    long end = System.currentTimeMillis();
    try {
      Thread.sleep(1000L);
    } catch (InterruptedException e) {
      // no-op
    }
    System.out.println("Time taken: " + (end - start) + "ms");
    System.exit(0);
  }

  private int index = 0;

  private Logger logger;

  public LoadRunner(int index) {
    super();
    this.index = index;
    this.logger = Logger.getLogger(this.getClass());
  }

  public void run() {
    for (int i = 5000; i-- > 0;) {
      StringBuffer buffer = new StringBuffer(256);
      buffer.append("Index ");
      buffer.append(this.index);
      buffer.append(this.hugeString());
      this.logger.debug(buffer);
//      try {
//        Thread.sleep(5L);
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//        break;
//      }
    }
  }
  
  private String hugeString() {
	  StringBuffer buffer = new StringBuffer(640);
	  for (int i = 0; i < 24; i++) {
		  buffer.append("abcdefghijklmnopqrstuvwxyz");
	  }
	  return buffer.toString();
  }
}
