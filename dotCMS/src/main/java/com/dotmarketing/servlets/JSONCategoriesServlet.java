package com.dotmarketing.servlets;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.PaginatedCategories;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class JSONCategoriesServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;
	private static final String JSON_CATEGORIES_SERVLET_AUTHENTICATION_NEEDED = "json.categories.servlet.authentication.needed";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		UtilMethods.removeBrowserCache(response);

		final UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		User user;

		try {

			final boolean isAuthenticationNeeded = Config.getBooleanProperty
								(JSON_CATEGORIES_SERVLET_AUTHENTICATION_NEEDED, true);
			if ((user = userWebAPI.getLoggedInUser(request)) == null && isAuthenticationNeeded) {

				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String inode = request.getParameter("inode");
			String q = request.getParameter("q");
			final String permission = request.getParameter("permission");

			if(UtilMethods.isSet(permission)) {
				loadPermission(inode, request, response);
				return;
			}

            q = StringEscapeUtils.unescapeJava( q );
			inode = (UtilMethods.isSet(inode) && inode.equals("undefined")) ? null : inode;
			q = (UtilMethods.isSet(q) && q.equals("undefined")) ? null : q;

			final String action = request.getParameter("action");

			if(UtilMethods.isSet(action) && action.equals("export")) {
				exportCategories(request, response, inode, q);
				return;
			}

			final ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			final CategoryAPI catAPI = APILocator.getCategoryAPI();
			int start = -1;
			int count = -1;
			final String startStr = request.getParameter("start");
			final String countStr = request.getParameter("count");
			final String sort = request.getParameter("sort");

			if(UtilMethods.isSet(startStr) && UtilMethods.isSet(countStr)) {
				start = Integer.parseInt(request.getParameter("start"));
				count = Integer.parseInt(request.getParameter("count"));
			}

			final Boolean topLevelCats = !UtilMethods.isSet(inode);
			final String reorder = request.getParameter("reorder");

			if(UtilMethods.isSet(reorder) && reorder.equalsIgnoreCase("TRUE")) {
				if(topLevelCats) {
					catAPI.sortTopLevelCategories();
				} else {
					catAPI.sortChildren(inode);
				}
			}

			final PaginatedCategories pagCategories = topLevelCats?catAPI.findTopLevelCategories(user, false, start, count, q, sort):
					catAPI.findChildren(user, inode, false, start, count, q, sort);

			final List<Map<String,Object>> items = new ArrayList<>();
			final List<Category> categories = pagCategories.getCategories();

			if(categories!=null) {
				for (Category category : categories) {
					final Map<String,Object> catMap = new HashMap();
					catMap.put("inode", category.getInode());
					catMap.put("category_name", category.getCategoryName());
					catMap.put("category_key", category.getKey());
					catMap.put("category_velocity_var_name", category.getCategoryVelocityVarName());
					catMap.put("sort_order", category.getSortOrder());
					catMap.put("keywords", category.getKeywords());
					items.add(catMap);
				}
			}

			final Map<String,Object> categoriesMap = new HashMap<>();
			categoriesMap.put("items", items);
			categoriesMap.put("numRows", pagCategories.getTotalCount());
			response.setContentType("text/plain");
			response.getWriter().write(mapper.writeValueAsString(categoriesMap));
			response.getWriter().flush();
			response.getWriter().close();

		}  catch (Exception e) {
			Logger.error(this, "Error retrieving categories", e);
		}
	}

	private void exportCategories(HttpServletRequest request, HttpServletResponse response, String contextInode, String filter) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment; filename=\"categories_" + UtilMethods.dateToHTMLDate(new Date(),"M_d_yyyy") +".csv\"");
		final PrintWriter out = response.getWriter();

		final UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();

		try {
			final User user = uWebAPI.getLoggedInUser(request);
			final CategoryAPI catAPI = APILocator.getCategoryAPI();
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
			Logger.error(this, "Error exporting categories", e);
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
