package com.dotmarketing.portlets.cmsmaintenance.ajax;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexHelper;
import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelper;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IndexAjaxAction extends AjaxAction {

	private final ESIndexHelper indexHelper;
	private final ESIndexAPI indexAPI;

	public IndexAjaxAction(){
		this(ESIndexHelper.getInstance(), APILocator.getESIndexAPI(), new WebResource());
	}

	@VisibleForTesting
	public IndexAjaxAction(final WebResource webResource) {
		this(ESIndexHelper.getInstance(), APILocator.getESIndexAPI(), webResource);
	}

	@VisibleForTesting
	protected IndexAjaxAction(ESIndexHelper indexHelper, ESIndexAPI indexAPI,
			final WebResource webResource) {
		super(webResource);
		this.indexHelper = indexHelper;
		this.indexAPI = indexAPI;
	}

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {




		Map<String, String> map = getURIParams();



		String cmd = map.get("cmd");
		java.lang.reflect.Method meth = null;
		Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };
		User user = getUser();





		try {
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)) {
				String userName = map.get("u");
				String password = map.get("p");
				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
                    user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)){
					response.sendError(401);
					return;
				}
			}



			meth = this.getClass().getMethod(cmd, partypes);

		} catch (Exception e) {

			try {
				cmd = "action";
				meth = this.getClass().getMethod(cmd, partypes);
			} catch (Exception ex) {
				Logger.error(this.getClass(), "Trying to run method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				return;
			}
		}
		try {
			meth.invoke(this, arglist);
		} catch (Exception e) {
			Logger.error(IndexAjaxAction.class, "Trying to run method:" + cmd);
			Logger.error(IndexAjaxAction.class, e.getMessage(), e.getCause());
			return;
		}

	}

	


	public void createIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotIndexException {

		Map<String, String> map = getURIParams();
		final int shards = Try.of(()->Integer.parseInt(map.get("shards"))).getOrElse(1);


		final boolean live = map.get("live") != null;
		final String indexName=((live) ? "live_" : "working_" ) + APILocator.getContentletIndexAPI().timestampFormatter.format(new Date());

		APILocator.getContentletIndexAPI().createContentIndexLegacy(indexName, shards);
        ESMappingUtilHelper.getInstance().addCustomMapping(indexName);
	}

	public void clearIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotStateException, DotDataException {
		Map<String, String> map = getURIParams();

		String indexName = indexHelper.getIndexNameOrAlias(map,"indexName","indexAlias",this.indexAPI);

		if(UtilMethods.isSet(indexName))
		    APILocator.getESIndexAPI().clearIndex(indexName);

	}

	public void deleteIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String indexName = indexHelper.getIndexNameOrAlias(map,"indexName","indexAlias",this.indexAPI);
		if(UtilMethods.isSet(indexName))
		    APILocator.getESIndexAPI().delete(indexName);
	}

	public void activateIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		String indexName = indexHelper.getIndexNameOrAlias(map,"indexName","indexAlias",this.indexAPI);
		if(IndexType.SITE_SEARCH.is(indexName)){
			APILocator.getSiteSearchAPI().activateIndex(indexName);
			return;
		}
		APILocator.getContentletIndexAPI().activateIndex(indexName);

	}
	public void deactivateIndex(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException {
		Map<String, String> map = getURIParams();
		String indexName = indexHelper.getIndexNameOrAlias(map,"indexName","indexAlias",this.indexAPI);
		if(IndexType.SITE_SEARCH.is(indexName)){
			APILocator.getSiteSearchAPI().deactivateIndex(indexName);
			return;
		}
		APILocator.getContentletIndexAPI().deactivateIndex(indexName);
	}

	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		return;

	}


	public void updateReplicas(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String indexName = indexHelper.getIndexNameOrAlias(map,"indexName","indexAlias",this.indexAPI);

		int replicas = Integer.parseInt(map.get("replicas"));

		try{
			APILocator.getESIndexAPI().updateReplicas(indexName, replicas);
		}catch (DotDataException e){
			writeError(response, e.getMessage());
		}
	}


	public void writeError(HttpServletResponse response, String error) throws IOException {
		String ret = null;

		try {
			ret = LanguageUtil.get(getUser(), error);
		} catch (Exception e) {

		}
		if (ret == null) {
			try {
				ret = LanguageUtil.get(PublicCompanyFactory.getDefaultCompanyId(), PublicCompanyFactory.getDefaultCompany().getLocale(),
						error);
			} catch (Exception e) {

			}
		}
		if (ret == null) {
			ret = error;
		}

		response.getWriter().println("FAILURE: " + ret);
	}

	public void closeIndex(HttpServletRequest request, HttpServletResponse response) {
	    Map<String, String> map = getURIParams();
	    String indexName = indexHelper.getIndexNameOrAlias(map,"indexName","indexAlias",this.indexAPI);

	    APILocator.getESIndexAPI().closeIndex(indexName);
	}

	public void openIndex(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> map = getURIParams();
		String indexName = map.get("indexName");
		APILocator.getESIndexAPI().openIndex(indexName);
	}

	public void getActiveIndex(HttpServletRequest request, HttpServletResponse response) throws IOException  {
		Map<String, String> map = getURIParams();
		String type =map.get("type");
		String resp = null;

		try {
			resp =  APILocator.getContentletIndexAPI().getActiveIndexName(type);
		} catch (DotDataException e) {
			resp =  e.getMessage();
		}

		response.getWriter().println(resp);
	}

	public void getIndexStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, String> map = getURIParams();
		String indexName = indexHelper.getIndexNameOrAlias(map,"indexName","indexAlias",this.indexAPI);
		String resp = null;

		try {
			resp = APILocator.getESIndexAPI().getIndexStatus(indexName).getStatus();
		} catch (DotDataException e) {
			resp = e.getMessage();
		}

		response.getWriter().println(resp);
    }


	public void getNotActiveIndexNames(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
		List<String> indices = idxApi.listDotCMSIndices();
		List<String> inactives = new ArrayList<>();

		for (String index : indices) {
			try {
				if(APILocator.getESIndexAPI().getIndexStatus(index) == ESIndexAPI.Status.INACTIVE) {
					inactives.add(index);
				}
			} catch (DotDataException e) {
			}
		}

		response.getWriter().println(inactives);
	}




	public void getReindexThreadStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.getWriter().println(ReindexThread.getInstance().isWorking()?"active":"stopped");
	}

	public void indexList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
        response.getWriter().println(idxApi.listDotCMSIndices());        
    }
}
