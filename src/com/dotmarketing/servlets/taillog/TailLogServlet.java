package com.dotmarketing.servlets.taillog;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.org.apache.commons.io.input.TailerListenerAdapter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class TailLogServlet extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = -1700686919872123657L;

	public void init() {}

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

		String fileName = request.getParameter("fileName");

		if(fileName.trim().isEmpty()) {
		    return;
		}


        String tailLogLofFolder = com.dotmarketing.util.Config.getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
        if (!tailLogLofFolder.endsWith(java.io.File.separator)) {
            tailLogLofFolder = tailLogLofFolder + java.io.File.separator;
        }
		File logFolder 	= new File(com.dotmarketing.util.FileUtil.getAbsolutlePath(tailLogLofFolder));
		File logFile 	= new File(com.dotmarketing.util.FileUtil.getAbsolutlePath(tailLogLofFolder + fileName));
        

		// if the logFile is outside of of the logFolder, die
		if ( !logFolder.exists() 
				||   !logFile.getCanonicalPath().startsWith(logFolder.getCanonicalPath())) {

			response.sendError(403);

			SecurityLogger.logInfo(TailLogServlet.class,  "Invalid File request:" + logFile.getCanonicalPath() + " from:" +request.getRemoteHost() + " " );
			return;
		}
		

		Logger.info(this.getClass(), "Requested logFile:" + logFile.getCanonicalPath());
		


		String regex = Config.getStringProperty("TAIL_LOG_FILE_REGEX", ".*\\.log$|.*\\.out$");


		if(!Pattern.compile(regex).matcher(fileName).matches()){
			//response.sendError(403);
			return;
		}

		response.setContentType("text/html;charset=UTF-8");

		ServletOutputStream out = response.getOutputStream();


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
		long startPosition = ((logFile.length() - 5000) < 0) ? 0 : logFile.length() - 5000;

		MyTailerListener listener = new MyTailerListener();
		listener.handle("Tailing " + fileName);
		listener.handle("----------------------------- ");
		tailer = new Tailer(logFile, listener, 1000);
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
