package com.dotmarketing.portlets.contentlet.ajax;

import com.dotmarketing.business.ajax.DwrUtil;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * Lets the content import pages follow and stop a running import. Only the user who started an
 * import, or a CMS administrator, can see whether it is still running or cancel it.
 */
public class ImportContentletAjax {

	/**
	 * Tells the import pages whether an import has finished.
	 * <p>A caller who did not start the import and is not a CMS administrator always gets
	 * {@code true}, the same answer an unknown import id gets, and the import is not looked at.
	 * That answer is not written to the security log because the page asks repeatedly.</p>
	 *
	 * @param id the import id
	 * @return {@code false} while the import is still running, {@code true} otherwise
	 * @throws DotSecurityException if nobody is logged in
	 */
	public boolean checkImportStatus(Long id) throws DotSecurityException, PortalException, SystemException {
		final User user = DwrUtil.getLoggedInUser();
		if (!canManageImport(user, id)) {
			return true;
		}
		Boolean result = ImportAuditUtil.isImportfinished(id);
		return result;
	}

	/**
	 * Cancels a running import. Only the user who started it, or a CMS administrator, can cancel
	 * it; the check runs before the import is flagged as cancelled.
	 *
	 * @param importId the import id
	 * @return the id of the cancelled import
	 * @throws DotSecurityException if nobody is logged in, or the logged-in user did not start the
	 *                              import and is not a CMS administrator (also for an unknown or
	 *                              missing id)
	 */
	public long cancelImport(Long importId) throws DotSecurityException, PortalException, SystemException {
		final User user = DwrUtil.getLoggedInUser();
		if (!canManageImport(user, importId)) {
			SecurityLogger.logInfo(ImportContentletAjax.class, String.format(
					"User [%s] did not start the import and is not a CMS administrator, which cancelling it requires",
					user.getUserId()));
			throw new DotSecurityException(
					"Only the user who started the import or a CMS administrator can cancel it");
		}
		ImportAuditUtil.cancelImport(importId);
		return importId;
	}

	/**
	 * Tells whether a user can follow or cancel an import: a CMS administrator can manage any
	 * import, any other user only the imports they started. A {@code null} id is treated like an
	 * import the user did not start.
	 *
	 * @param user     the logged-in {@link User}
	 * @param importId the import id
	 * @return {@code true} if the user can manage the import
	 */
	private boolean canManageImport(final User user, final Long importId) {
		if (importId == null) {
			return false;
		}
		if (user.isAdmin()) {
			return true;
		}
		final String ownerId = ImportAuditUtil.findImportOwner(importId);
		final String callerId = user.getUserId();
		return ownerId != null && callerId != null && ownerId.equals(callerId.trim());
	}

}
