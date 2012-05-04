package org.apache.log4j.appender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

public class RollingNewFileAppender extends RollingFileAppender {

	private Map<String, RollingFileAppender> map;

	public RollingNewFileAppender() {
		super();
		map = new HashMap<String, RollingFileAppender>();
	}

	protected void subAppend(LoggingEvent event) {
		String message = "" + event.getMessage();
		String[] list = message.split(":");
		if (list.length > 2) {
			String hostName = list[1];

			RollingFileAppender rolling = map.get(hostName);
			if (rolling == null) {
				try {

					String[] folderList = fileName.split("logs" + File.separator);
					String newFileName = ((String) folderList[0]).concat("logs" + File.separator + hostName + File.separator).concat(folderList[1]);
					System.out.println("New filename: " + newFileName);
					rolling = new RollingFileAppender(this.layout, newFileName, this.fileAppend);
				} catch (IOException e) {
					e.printStackTrace();
					super.subAppend(event);
					return;
				}
				map.put(hostName, rolling);
			}

			rolling.append(event);
		} else {
			super.subAppend(event);
		}

	}

	private String cleanMessage(String message) {

		return null;

	}

}
