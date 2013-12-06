package com.dotmarketing.servlets;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

public class AdminControlServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

			ServletOutputStream out = response.getOutputStream();
			response.setContentType("image/gif");
			FileInputStream fis = new FileInputStream(FileUtil.getRealPath("/images/shim.gif"));

			byte[] buf = new byte[1024];
			int i = 0;

			while ((i = fis.read(buf)) != -1) {
				out.write(buf, 0, i);
			}

			fis.close();
			out.close();
		}
		catch (FileNotFoundException e) {
			Logger.error(this, e.toString(), e);
		}

		String window = request.getParameter("window");
		String top = request.getParameter("top");
		String left = request.getParameter("left");
		String closed = request.getParameter("closed");
		
		HttpSession session = request.getSession();
		if(window!=null && window.equals("admin")){
			if (top!=null) {
				//session.setAttribute(WebKeys.ADMIN_CONTROL_TOP,top);
				session.setAttribute("ADMIN_CONTROL_TOP",top);
			}
			else {
				session.removeAttribute("ADMIN_CONTROL_TOP");
			}
			if (left!=null) {
				//session.setAttribute(WebKeys.ADMIN_CONTROL_LEFT,left);
				session.setAttribute("ADMIN_CONTROL_LEFT",left);
			}
			else {
				session.removeAttribute("ADMIN_CONTROL_LEFT");
			}
			if (closed!=null && closed.equals("true")) {
				//session.setAttribute(WebKeys.ADMIN_CONTROL_CLOSED,closed);
				session.setAttribute("ADMIN_CONTROL_CLOSED",new Boolean(true));
			}
			else {
				session.setAttribute("ADMIN_CONTROL_CLOSED",new Boolean(false));
			}
		}else{
			if (top!=null) {
				//session.setAttribute(WebKeys.ADMIN_CONTROL_TOP,top);
				session.setAttribute("TASK_CONTROL_TOP",top);
			}
			else {
				session.removeAttribute("TASK_CONTROL_TOP");
			}
			if (left!=null) {
				//session.setAttribute(WebKeys.ADMIN_CONTROL_LEFT,left);
				session.setAttribute("TASK_CONTROL_LEFT",left);
			}
			else {
				session.removeAttribute("TASK_CONTROL_LEFT");
			}
			if (closed!=null && closed.equals("true")) {
				//session.setAttribute(WebKeys.ADMIN_CONTROL_CLOSED,closed);
				session.setAttribute("TASK_CONTROL_CLOSED",new Boolean(true));
			}
			else {
				session.setAttribute("TASK_CONTROL_CLOSED",new Boolean(false));
			}
		
		}
	}


}
