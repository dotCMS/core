package com.dotmarketing.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class JSONTagsServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		UtilMethods.removeBrowserCache(response);

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		User user = null;

		try {
			user = uWebAPI.getLoggedInUser(request);

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			TagAPI tagAPI = APILocator.getTagAPI();

			String termFilter = "";
			if(UtilMethods.isSet(request.getParameter("tagname"))) {
				termFilter = request.getParameter("tagname").toLowerCase();
			}

			boolean globalTagsFilter = false;

			if(UtilMethods.isSet(request.getParameter("global"))) {
				if(request.getParameter("global").equalsIgnoreCase("1"))
					globalTagsFilter = true;
			}

			String hostFilter = request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID).toString();

			String sort = request.getParameter("sort");

			String action = request.getParameter("action");

			if(UtilMethods.isSet(action) && action.equals("export")) {
				List<Tag> tagsList = tagAPI.getFilteredTags(termFilter, hostFilter, globalTagsFilter, sort, 0, 0);
				exportTags(request,  response,  tagsList);
				return;
			}

			if(UtilMethods.isSet(action) && action.equals("download")) {
				downloadCSVSampleFile(response);
				return;
			}

			int start = 0;
			int count = 20;
			String startStr = request.getParameter("start");
			String countStr = request.getParameter("count");

			if(UtilMethods.isSet(startStr) && UtilMethods.isSet(countStr)) {
				start = Integer.parseInt(request.getParameter("start"));
				count = Integer.parseInt(request.getParameter("count"));
			}


			List<Map<String,Object>> items = new ArrayList<Map<String,Object>>();

			List<Tag> tags = null;

			if(UtilMethods.isSet(termFilter)){
				tags = tagAPI.getFilteredTags(termFilter, hostFilter, globalTagsFilter, sort, start, count);
			}else{
				tags = tagAPI.getFilteredTags("", hostFilter, globalTagsFilter, sort, start, count);
			}

			if(tags!=null) {
				for (Tag tag : tags) {
					String hostId = tag.getHostId();
					Host host = null;
					try{
						host = APILocator.getHostAPI().find(hostId, user, true);
					}
					catch (Exception e){
						Logger.error(this, "Unable to get host from tag Id:"+ tag.getHostId());
					}
					if (host!=null){
						Map<String,Object> tagMap = new HashMap<String,Object>();
						String hostName = "";
						if(host.isSystemHost())
							hostName = LanguageUtil.get(APILocator.getUserAPI().getSystemUser(), "tag-all-hosts");
						else
							hostName = host.getHostname();
						tagMap.put("tagId", tag.getTagId());
						tagMap.put("tagname", tag.getTagName());
						tagMap.put("hostId", tag.getHostId());
						tagMap.put("hostName", hostName);
						items.add(tagMap);
					}
				}
			}

			Map<String,Object> m = new HashMap<String, Object>();
			m.put("items", items);
			m.put("numRows", tagAPI.getFilteredTags(termFilter, hostFilter, globalTagsFilter, sort, 0, 0).size());
			String s = mapper.writeValueAsString(m);
			response.setContentType("text/plain");
			response.getWriter().write(s);
			response.getWriter().flush();
			response.getWriter().close();




		} catch (DotRuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void downloadCSVSampleFile(HttpServletResponse response) throws IOException {
		ServletOutputStream out = response.getOutputStream();
		try {
			response.setContentType("application/octet-stream; charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"csv_sample_file.csv\"");
			out.print("Tag Name,Host ID");
			out.print("\r\n");

			out.print("Sample Tag,Host Identifier");
			out.print("\r\n");

			HibernateUtil.closeSession();

		}catch(Exception p){
			Logger.error(this,p.getMessage(),p);
		}
		finally{
			out.flush();
			out.close();
		}

	}

	/**
	 * Export all tags into a single CSV file, including tag name, user id and the host id
	 * @throws DotDataException
	 * @throws IOException
	 * @throws DotSecurityException
	 */
	private void exportTags(HttpServletRequest request, HttpServletResponse response, List<Tag> tagsList) throws DotSecurityException, IOException{
		ServletOutputStream out = response.getOutputStream();
		try {
			response.setContentType("application/octet-stream; charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"tags_" + UtilMethods.dateToHTMLDate(new Date(),"M_d_yyyy") +".csv\"");


			if(tagsList.size() > 0) {

				out.print("Tag Name,Host ID");
				out.print("\r\n");

				for(Tag tag : tagsList ){
					out.print(tag.getTagName()+",");
					out.print(tag.getHostId());
					out.print("\r\n");
				}

				HibernateUtil.closeSession();

			}
			else {
				out.print("There are no Tags to show");
				out.print("\r\n");
			}
		}catch(Exception p){
			Logger.error(this,p.getMessage(),p);
		}
		finally{
			out.flush();
			out.close();
		}
	}


}

