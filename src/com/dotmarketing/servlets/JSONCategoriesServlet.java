package com.dotmarketing.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.PaginatedCategories;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class JSONCategoriesServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		UtilMethods.removeBrowserCache(response);

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		User user = null;

		try {

			user = uWebAPI.getLoggedInUser(request);
			String inode = request.getParameter("inode");
			String action = request.getParameter("action");
			String q = request.getParameter("q");
			String permission = request.getParameter("permission");
			String reorder = request.getParameter("reorder");

			if(UtilMethods.isSet(permission)) {
				loadPermission(inode, request, response);
				return;
			}

			inode = (UtilMethods.isSet(inode) && inode.equals("undefined")) ? null : inode;
			q = (UtilMethods.isSet(q) && q.equals("undefined")) ? null : q;

			if(UtilMethods.isSet(action) && action.equals("export")) {
				exportCategories(request, response, inode, q);
				return;
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			CategoryAPI catAPI = APILocator.getCategoryAPI();
			int start = -1;
			int count = -1;
			String startStr = request.getParameter("start");
			String countStr = request.getParameter("count");
			String sort = request.getParameter("sort");

			if(UtilMethods.isSet(startStr) && UtilMethods.isSet(countStr)) {
				start = Integer.parseInt(request.getParameter("start"));
				count = Integer.parseInt(request.getParameter("count"));
			}

			Boolean topLevelCats = !UtilMethods.isSet(inode);

			if(UtilMethods.isSet(reorder) && reorder.equalsIgnoreCase("TRUE")) {
				if(topLevelCats) {
					catAPI.sortTopLevelCategories();
				} else {
					catAPI.sortChildren(inode);
				}
			}

			PaginatedCategories pagCategories = topLevelCats?catAPI.findTopLevelCategories(user, false, start, count, q, sort):
					catAPI.findChildren(user, inode, false, start, count, q, sort);

			List<Map<String,Object>> items = new ArrayList<Map<String,Object>>();
			List<Category> categories = pagCategories.getCategories();

			if(categories!=null) {
				for (Category category : categories) {
					Map<String,Object> catMap = new HashMap<String,Object>();
					catMap.put("inode", category.getInode());
					catMap.put("category_name", category.getCategoryName());
					catMap.put("category_key", category.getKey());
					catMap.put("category_velocity_var_name", category.getCategoryVelocityVarName());
					catMap.put("sort_order", category.getSortOrder());
					catMap.put("keywords", category.getKeywords());
					items.add(catMap);
				}
			}

			Map<String,Object> m = new HashMap<String, Object>();
			m.put("items", items);
			m.put("numRows", pagCategories.getTotalCount());
			String s = mapper.writeValueAsString(m);
			response.setContentType("text/plain");
			response.getWriter().write(s);
			response.getWriter().flush();
			response.getWriter().close();

		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	private void exportCategories(HttpServletRequest request, HttpServletResponse response, String contextInode, String filter) throws ServletException, IOException {
		ServletOutputStream out = response.getOutputStream();
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment; filename=\"categories_" + UtilMethods.dateToHTMLDate(new Date(),"M_d_yyyy") +".csv\"");

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		User user = null;

		try {
			user = uWebAPI.getLoggedInUser(request);
			CategoryAPI catAPI = APILocator.getCategoryAPI();
			List<Category> categories = UtilMethods.isSet(contextInode)?catAPI.findChildren(user, contextInode, false, filter):
				catAPI.findTopLevelCategories(user, false, filter);

			if(!categories.isEmpty()) {
				out.print("\"name\",\"key\",\"variable\",\"sort\"");
				out.print("\r\n");

				for (Category category : categories) {
					String catName = category.getCategoryName();
					String catKey = category.getKey();
					String catVar = category.getCategoryVelocityVarName();
					String catSort = Integer.toString(category.getSortOrder());
					catName = catName==null?"":catName;
					catKey = catKey==null?"":catKey;
					catVar = catVar==null?"":catVar;
					catSort = catSort==null?"":catSort;

//					if(catName.indexOf(",")>-1) {
//						catName = "'" + catName + "'";
//					}

					catName = "\"" + catName + "\"";
					catKey = "\"" + catKey + "\"";
					catVar = "\"" + catVar + "\"";

					out.print(catName+","+catKey+","+catVar+","+catSort);
					out.print("\r\n");
				}

			} else {
				out.print("There are no Categories to show");
				out.print("\r\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			out.flush();
			out.close();
		}
	}


	private void loadPermission(String inode, HttpServletRequest request, HttpServletResponse response) throws Exception {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		User user = uWebAPI.getLoggedInUser(request);
		CategoryAPI categoryAPI = APILocator.getCategoryAPI();
		Category cat = categoryAPI.find(inode, user, false);
		request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, cat);
		RequestDispatcher rd = request.getRequestDispatcher("/html/portlet/ext/common/edit_permissions_tab_ajax.jsp");
		rd.include(request, response);
	}

}
