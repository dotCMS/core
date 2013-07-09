package com.dotcms.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import bsh.util.Util;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


@Path("/bundle")
public class BundleResource extends WebResource {


	@GET
	@Path("/getunsendbundles/{params:.*}")
	@Produces("application/json")
	public String getUnsendBundles(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException {
		InitDataObject initData = init(params, true, request, true);

		String userId = initData.getParamsMap().get("userid");


		StringBuilder json = new StringBuilder();

		json.append("[");
		int environmentCounter = 0;

		List<Bundle> bundles = APILocator.getBundleAPI().getUnsendBundles(userId);

		for(Bundle b : bundles) {

			json.append("{id: '").append(b.getId()).append("', ");
			json.append("name: '").append(b.getName()).append("' } ");

			if(environmentCounter+1 < bundles.size()) {
				json.append(", ");
			}

			environmentCounter++;
		}

		json.append("]");


		return json.toString();

	}

	@GET
	@Path("/doesbundleexist/{params:.*}")
	@Produces("application/json")
	public String doesBundleExist(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException {
		InitDataObject initData = init(params, true, request, true);

		String bundleName = initData.getParamsMap().get("name");

		Bundle bundle = APILocator.getBundleAPI().getBundleByName(bundleName);

		return bundle!=null?"true":"false";

	}

	@GET
	@Path("/updatebundle/{params:.*}")
	@Produces("application/json")
	public String updateBundle(@Context HttpServletRequest request, @PathParam("params") String params) {
		InitDataObject initData = init(params, true, request, true);
		String bundleId = initData.getParamsMap().get("bundleid");
		String bundleName = initData.getParamsMap().get("bundlename");

		try {

			if(!UtilMethods.isSet(bundleId)) {
				return "false";
			}

			Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
			bundle.setName(bundleName);
			APILocator.getBundleAPI().updateBundle(bundle);

		} catch (DotDataException e) {
			Logger.error(getClass(), "Error trying to update Bundle. Bundle ID: " + bundleId);
			return "false";
		}

		return "true";

	}

}