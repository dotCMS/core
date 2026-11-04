package com.dotmarketing.portlets.contentlet.ajax;

import com.dotmarketing.business.ajax.DwrUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.util.Logger;
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
	 * <p>For a CMS administrator the stored status of the import is read directly. For any other
	 * caller, the user who started the import is first read from its audit record with
	 * {@link ImportAuditUtil#findImportOwner(Long)}; a caller who did not start it gets
	 * {@code true}, the stored status is not read, and nothing is written to the security log.
	 * A {@code null} id also gets {@code true}.</p>
	 *
	 * @param id the import id
	 * @return {@code false} while the import is still running, {@code true} otherwise
	 * @throws DotSecurityException if nobody is logged in
	 * @throws DotDataException     if the user who started the import cannot be read
	 */
	public boolean checkImportStatus(Long id)
			throws DotSecurityException, DotDataException, PortalException, SystemException {
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
	 * @throws DotSecurityException if nobody is logged in, the id is missing, or the logged-in user
	 *                              did not start the import and is not a CMS administrator (an
	 *                              unknown id counts as an import the user did not start)
	 * @throws DotDataException     if the user who started the import cannot be read; the import
	 *                              is then not cancelled
	 */
	public long cancelImport(Long importId)
			throws DotSecurityException, DotDataException, PortalException, SystemException {
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
	 * @throws DotDataException if the user who started the import cannot be read; the cause is
	 *                          logged and the exception carries a fixed message, as DWR sends the
	 *                          message to the page
	 */
	private boolean canManageImport(final User user, final Long importId) throws DotDataException {
		if (importId == null) {
			return false;
		}
		if (user.isAdmin()) {
			return true;
		}
		final String ownerId;
		try {
			ownerId = ImportAuditUtil.findImportOwner(importId);
		} catch (final DotDataException e) {
			Logger.error(ImportContentletAjax.class, "The user who started the import could not be read", e);
			throw new DotDataException("The import could not be checked");
		}
		return ownerId != null && ownerId.equals(user.getUserId());
	}

}
