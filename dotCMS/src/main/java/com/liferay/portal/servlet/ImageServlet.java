/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.servlet;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.model.Company;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.ImageLocalUtil;
import com.liferay.portal.model.Image;
import com.liferay.util.ParamUtil;

/**
 * <a href="ImageServlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Brett Randall
 * @version $Revision: 1.9 $
 *
 */
public class ImageServlet extends HttpServlet {

	public void init(ServletConfig sc) throws ServletException {
		synchronized (ImageServlet.class) {
			super.init(sc);
		}
		final Company company = APILocator.getCompanyAPI().getDefaultCompany();
		//If the City column does not startsWith /dA means that we need to migrate the old logo
		//to a new dotAsset
		if (company.getCity() == null || !company.getCity().startsWith("/dA")) {
			migrateCurrentLogoToDotAsset(company);
		}
	}

	public void service(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {
		final Company company = APILocator.getCompanyAPI().getDefaultCompany();
		//Forward to the Servlet
		if (company.getCity() != null && company.getCity().startsWith("/dA")) {
			req.getRequestDispatcher(company.getCity()).forward(req, res);
		}
	}

	@WrapInTransaction
	private void migrateCurrentLogoToDotAsset(final Company company) {
		try {
			final Image image = ImageLocalUtil.get("dotcms.org");

			final File outputFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()+ File.separator + "company_logo.png");
			FileUtils.writeByteArrayToFile(outputFile, image.getTextObj());

			Contentlet contentlet = new Contentlet();
			contentlet.setContentTypeId(
					Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
							.find("dotAsset").id()).getOrNull());
			contentlet.setHost(APILocator.systemHost().getHost());
			contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
			contentlet.setBinary(DotAssetContentType.ASSET_FIELD_VAR, outputFile);
			final Optional<WorkflowAction> workflowActionOpt =
					APILocator.getWorkflowAPI().findActionMappedBySystemActionContentlet
							(contentlet, WorkflowAPI.SystemAction.PUBLISH, APILocator.systemUser());
			if (workflowActionOpt.isPresent()) {
				contentlet = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet, new ContentletDependencies.Builder()
						.workflowActionId(workflowActionOpt.get().getId()).modUser(APILocator.systemUser())
						.build());
			} else {
				contentlet = APILocator.getContentletAPI().checkin(contentlet, APILocator.systemUser(), false);
				APILocator.getContentletAPI().publish(contentlet, APILocator.systemUser(), false);
			}
			final String assetUrl = String
					.format("/dA/%s/asset/company_logo.png", contentlet.getIdentifier());
			company.setCity(assetUrl);
			//Update the company
			CompanyManagerUtil.updateCompany(company);
		} catch (Exception e) {
			Logger.error(this, e.getMessage());
		}
	}

}