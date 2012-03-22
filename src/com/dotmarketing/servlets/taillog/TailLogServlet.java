package com.dotmarketing.servlets.taillog;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.input.TailerListenerAdapter;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class TailLogServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1700686919872123657L;

	static String logFolder = null;

	public void init() {
		String x = Config.getStringProperty("TAIL_LOG_LOG_FOLDER");
		if (UtilMethods.isSet(x)) {
			logFolder = x;
		}
	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		
		
		
		User user = null;
		try {
			user = com.liferay.portal.util.PortalUtil.getUser(request);
		} catch (Exception e) {
			response.sendError(403);
			return;
		}
		try {
			if (!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
				response.sendError(403);
				return;
			}
		} catch (Exception e2) {
			Logger.error(this.getClass(), e2.getMessage(), e2);
			response.sendError(403);
			return;
		}
		if(logFolder ==null){
			return;
		}
		
		
		
		String fileName = request.getParameter("fileName");
		
		try{
			fileName = UtilMethods.validateFileName(fileName);
		}
		catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage());
			return;
		}
		

		// clean and check passed in filename against allowed files

		
		
		
		String regex = Config.getStringProperty("TAIL_LOG_FILE_REGEX");
		if(!UtilMethods.isSet(regex)){
			regex="!.*";
		}
		
		//regex=".*\\.log$|.*\\.out$";
		//regex="!.*";
		
		if(!Pattern.compile(regex).matcher(fileName).matches()){
			//response.sendError(403);
			return;
		}
		


		ServletOutputStream out = response.getOutputStream();

		File file = null;
		try {
			file = new File(getServletContext().getRealPath(logFolder + "/" + fileName));
		} catch (Exception e) {
			Logger.error(this.getClass(), "unable to open log file '" + logFolder
					+ "' please set the config variable TAIL_LOG_SERVLET_FILEPATH correctly");
		}
		if (file == null || !file.exists()) {

			out.write(new String("unable to open log file '" + logFolder
					+ "' please set the config variable TAIL_LOG_SERVLET_FILEPATH correctly").getBytes());
			return;
		}

		out.print("<html>"
				+ "<head>"
				+ "<title>dotCMS Log</title>"
				+ "<style type='text/css'>@import '/html/css/dot_admin.css';</style>"
				+ "<script>var working =false;"
				+ "function doS(){"
				+ "if(!working){working=true;if(parent.document.getElementById('scrollMe').checked){dh=document.body.scrollHeight;ch=document.body.clientHeight;if(dh>ch){moveme=dh-ch;window.scrollTo(0,moveme);}}working=false;}"
				+ "}</script>" + "</head><body class='tailerBody'>");

		out.flush();

		Tailer tailer = null;
		long startPosition = ((file.length() - 5000) < 0) ? 0 : file.length() - 5000;

		MyTailerListener listener = new MyTailerListener();
		listener.handle("Tailing " + logFolder + "/" + fileName);
		listener.handle("----------------------------- ");
		tailer = new Tailer(file, listener, 1000);
		tailer.setStartPosition(startPosition);
		MyTailerThread thread = new MyTailerThread(tailer);

		String name = null;
		for (int i = 0; i < 1000; i++) {
			name = "LogTailer" + i + ":" + fileName;
			Thread t = ThreadUtils.getThread(name);
			if (t == null) {
				break;
			}
			if (i > 100) {
				throw new ServletException("Too many Logger threads");
			}
		}

		thread.setName(name);

		thread.start();

		try {
			while (thread.isAlive()) {
				String write = listener.getOut(true).toString();
				if (write == null || write.length() == 0) {
					response.getOutputStream().print(" ");
				} else {
					response.getOutputStream().print(write);
					response.getOutputStream().print("<script>doS();</script>");
				}
				response.getOutputStream().flush();
				Thread.sleep(1000);

			}
		} catch (Exception e) {
			if (thread != null) {
				thread.stopTailer();
			}
		}

	}

	private class MyTailerListener extends TailerListenerAdapter {

		StringWriter out = new StringWriter();

		public void handle(String line) {
			getOut().append(UtilMethods.xmlEscape(line) + "<br />");
		}

		StringWriter getOut() {
			return getOut(false);
		}

		StringWriter getOut(boolean refresh) {
			synchronized (this) {
				if (refresh) {
					StringWriter s = new StringWriter().append(out.toString());
					this.out = new StringWriter();
					return s;
				}
				return out;
			}
		}

	}

	class MyTailerThread extends Thread {

		Tailer tailer = null;

		public MyTailerThread(Tailer target) {

			super(target);
			tailer = (Tailer) target;
		}

		public void stopTailer() {
			// ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			// int threadCount = bean.getThreadCount();

			// System.out.println("Thread Count = " + threadCount);
			if (tailer != null) {
				tailer.stop();
			}
		}

		public void setTailer(Tailer tailer) {
			this.tailer = tailer;
		}

	}

}
