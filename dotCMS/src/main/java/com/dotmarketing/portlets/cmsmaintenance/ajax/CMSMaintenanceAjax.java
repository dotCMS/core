package com.dotmarketing.portlets.cmsmaintenance.ajax;


import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ajax.DwrUtil;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.fixtask.FixTasksExecutor;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.portlets.cmsmaintenance.util.CleanAssetsThread;
import com.dotmarketing.portlets.cmsmaintenance.util.CleanAssetsThread.BasicProcessStatus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.io.File;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.quartz.JobExecutionContext;

/**
 * This class provides access to maintenance routines that dotCMS users can run
 * in order to keep their environments as optimized and clean as possible.
 * <p>Every public method can only be run by a CMS administrator for whom the layout API grants
 * the Maintenance portlet, the same requirement as the maintenance REST endpoints. The layout API
 * grants every portlet to a CMS administrator who is a back-end user.</p>
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class CMSMaintenanceAjax {

    /**
     * Returns the progress of the full reindex that is currently running.
     *
     * @return the reindex status values shown on the Maintenance portlet
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public Map getReindexationProgress()
            throws DotDataException, DotSecurityException, PortalException, SystemException {
    	requireAdminWithMaintenancePortlet();
        return ESReindexationProcessStatus.getProcessIndexationMap();
    }



    /**
     * Deletes a search index.
     *
     * @param indexName the name of the index to delete
     * @return {@code true} if the index was deleted
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public boolean deleteIndex(String indexName)
            throws DotDataException, DotSecurityException, PortalException, SystemException {
    	requireAdminWithMaintenancePortlet();
    	return  APILocator.getContentletIndexAPI().delete(indexName);
    }


    /**
     * Checks that the logged-in user can run maintenance routines: a CMS administrator who also
     * has access to the Maintenance portlet.
     *
     * @return {@code true} when the user is allowed; any other user gets an exception instead
     * @throws DotSecurityException if nobody is logged in, or the logged-in user is not a CMS
     *                              administrator with access to the Maintenance portlet
     */
    public boolean validateUser()
            throws DotDataException, DotSecurityException, PortalException, SystemException {
        requireAdminWithMaintenancePortlet();
        return true;
    }


    /**
     * Stops the full reindex that is currently running.
     *
     * @return the reindex status values after stopping it
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public Map stopReindexation()
            throws DotDataException, DotSecurityException, PortalException, SystemException {
    	requireAdminWithMaintenancePortlet();
    	APILocator.getContentletIndexAPI().stopFullReindexation();
        return ESReindexationProcessStatus.getProcessIndexationMap();
    }

	/**
	 * Stops the re-indexation process and switches over to the new index data.
	 * This is useful when there only a few contents that could not be
	 * re-indexed and can be either fixed or deleted in the future.
	 * 
	 * @return A {@link Map} containing status information after switching to
	 *         the new index.
	 * @throws SQLException
	 *             An error occurred when interacting with the database.
	 * @throws DotDataException
	 *             The process to switch to the new failed.
	 * @throws InterruptedException
	 *             The established pauses to switch to the new index failed.
	 * @throws DotSecurityException
	 *             The logged-in user is not a CMS administrator with access to the
	 *             Maintenance portlet.
	 */
	public Map stopReindexationAndSwitchover() throws DotDataException, SQLException, InterruptedException,
			DotSecurityException, PortalException, SystemException {
		requireAdminWithMaintenancePortlet();
		APILocator.getContentletIndexAPI().stopFullReindexationAndSwitchover();
		return ESReindexationProcessStatus.getProcessIndexationMap();
	}

    /**
     * Removes the content of one content type from the search index and indexes it again.
     *
     * @param inode the inode of the content type to reindex
     * @return the confirmation message shown on the Maintenance portlet
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public String cleanReindexStructure(String inode)
            throws DotDataException, DotSecurityException, PortalException, SystemException {
    	requireAdminWithMaintenancePortlet();
    	Structure structure = CacheLocator.getContentTypeCache().getStructureByInode(inode);
    	APILocator.getContentletIndexAPI().removeContentFromIndexByStructureInode(inode);
    	APILocator.getContentletAPI().refresh(structure);

    	Company d = PublicCompanyFactory.getDefaultCompany();
    	try {
			return LanguageUtil.get(d.getCompanyId(),d.getLocale(), "message.cmsmaintenance.cache.indexrebuilt");
		} catch (LanguageException e) {
			return "message.cmsmaintenance.cache.indexrebuilt";
		}
    }

    /**
     * Optimizes every dotCMS search index.
     *
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public void optimizeIndices()
            throws DotDataException, DotSecurityException, PortalException, SystemException {
    	requireAdminWithMaintenancePortlet();
        ContentletIndexAPI api=APILocator.getContentletIndexAPI();
        List<String> indices=api.listDotCMSIndices();
        api.optimize(indices);
    }

    /**
     * Flushes the caches of every dotCMS search index.
     *
     * @return the flush result per index
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public Map<String, Integer>  flushIndiciesCache() throws InterruptedException, ExecutionException,
            DotDataException, DotSecurityException, PortalException, SystemException {
      requireAdminWithMaintenancePortlet();
      ContentletIndexAPI api=APILocator.getContentletIndexAPI();
      List<String> indices=api.listDotCMSIndices();
      return APILocator.getESIndexAPI().flushCaches(indices);
  }
    /**
	 * The path where tmp files are stored. This gets wiped alot
	 */
	private String backupTempFilePath = ConfigUtils.getBackupPath()+File.separator+"temp";

	private static String assetRealPath = null;
	private static String assetPath = File.separator + "assets";

    private  FixTasksExecutor fixtask=FixTasksExecutor.getInstance();

    /**
     * Runs the fix tasks that repair asset inconsistencies.
     *
     * @return the results of the fix tasks, or {@code null} when there are none
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public List <Map> fixAssetsInconsistencies() throws Exception
    {
    	requireAdminWithMaintenancePortlet();
        JobExecutionContext arg0=null;
        fixtask.execute(arg0);
		List result=fixtask.getTasksresults();
		 if(result.size()==0){
			 result=null;
		 }

        return result;

    }

    /**
     * Returns the results gathered so far by the fix tasks that repair asset inconsistencies.
     *
     * @return the results of the fix tasks, or {@code null} when there are none
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public List <Map> getFixAssetsProgress() throws Exception{
    	requireAdminWithMaintenancePortlet();
    	List result=fixtask.getTasksresults();
		 if(result.size()==0){
			 result=null;}
        return result;
    }

	/**
	 * Takes a list of comma-separated Identifiers and deletes them. The content is
	 * always deleted as the logged-in user.
	 *
	 * @param List
	 *            - The list of Identifiers as Strings.
	 * @param userId
	 *            - Not used. The Maintenance portlet still sends it, so it stays in the
	 *            signature; the deletion runs as the logged-in user.
	 * @return A String array of information that provides the user with the
	 *         results of performing this action.
	 * @throws PortalException
	 *             An error occurred when retrieving the user information.
	 * @throws SystemException
	 *             A system error occurred. Please check the system logs.
	 * @throws DotDataException
	 *             An error occurred when accessing the contentlets to delete.
	 * @throws DotSecurityException
	 *             The logged-in user is not a CMS administrator with access to the
	 *             Maintenance portlet, or cannot delete the content.
	 */
	public String deleteContentletsFromIdList(String List, String userId) throws PortalException, SystemException, DotDataException,DotSecurityException {
		final User user = requireAdminWithMaintenancePortlet();
		List<String> conditionletWithErrors = new ArrayList<>();
		ContentletAPI conAPI = APILocator.getContentletAPI();
		String[] inodes = List.split(",");
		Integer contdeleted = 0;

		for (int i = 0; i < inodes.length; i++) {
			inodes[i] = inodes[i].trim();
		}

		List<Contentlet> contentlets = new ArrayList<>();

		for (String inode : inodes) {
			if (!inode.trim().equals("")) {
				contentlets.addAll(conAPI.getSiblings(inode));
			}
		}

		if (!contentlets.isEmpty()) {
			for (Contentlet contentlet : contentlets) {
				boolean delete = conAPI.destroy(contentlet, user, true);

				if (!delete){
					conditionletWithErrors.add(contentlet.getIdentifier());
				}else{
					contdeleted++;
				}
			}

			return getDeleteContentletMessage(user, conditionletWithErrors, contdeleted);
		}else{
			return LanguageUtil.get(user, "message.contentlet.delete.error.dontExists");
		}
	}

	private String getDeleteContentletMessage(User user, List<String> conditionletWithErrors, Integer contdeleted) throws LanguageException {
		String result = null;

		if (conditionletWithErrors.isEmpty()){
			result = LanguageUtil.get(user,"contentlets-were-succesfully-deleted");
		}else{
			String errorMessage = LanguageUtil.get(user,"message.contentlet.delete.error.archived");
			String conditionletIdentifier = conditionletWithErrors.toString().replace("[", "").replace("]", "")
							.replace(", ", ",");
			errorMessage = MessageFormat.format(errorMessage, conditionletIdentifier);

					if (contdeleted > 0){
					result = LanguageUtil.get(user, "message.contentlet.delete.success.withError");
					result = MessageFormat.format(result, errorMessage);
				}else{
					result = errorMessage;
				}
		}

		return result;
	}

	/**
	 * Deletes the whole push publishing history.
	 *
	 * @return {@code "success"}, or a message describing why the history could not be deleted
	 * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
	 *                              to the Maintenance portlet
	 */
	public String deletePushedAssets() throws PortalException, SystemException, DotDataException,DotSecurityException {

		// Runs before the try below so a rejected user gets an exception, not a failure message
		requireAdminWithMaintenancePortlet();

		String result = "success";

		try {

			APILocator.getPushedAssetsAPI().deleteAllPushedAssets();

		} catch(Exception e) {
			Logger.error(getClass(), e.getMessage(), e);
			result = "Could not delete the pushed assets. " + e.getMessage();
		}

		return result;
	}

    /**
     * Deletes the versions of every asset that are older than the given date.
     *
     * @param date the cut-off date, in {@code MM/dd/yyyy} format
     * @return the number of versions deleted
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public int removeOldVersions(String date) throws ParseException, SQLException, DotDataException,
            DotSecurityException, PortalException, SystemException {
        	requireAdminWithMaintenancePortlet();
        	Date assetsOlderThan = new SimpleDateFormat("MM/dd/yyyy").parse(date);
        	return CMSMaintenanceFactory.deleteOldAssetVersions(assetsOlderThan);
    }

    /**
     * Starts the background process that deletes asset files no longer referenced by any content.
     *
     * @return the initial status of the process
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public Map cleanAssets ()
            throws DotDataException, DotSecurityException, PortalException, SystemException {

        requireAdminWithMaintenancePortlet();

        //Create the thread to clean the assets
        CleanAssetsThread cleanAssetsThread = CleanAssetsThread.getInstance( true , true);
        BasicProcessStatus processStatus = cleanAssetsThread.getProcessStatus();
        cleanAssetsThread.start();

        //Return the initial process status
        return processStatus.buildStatusMap();
    }

    /**
     * Method to check the status of the clean assets process
     *
     * @return map with the current status information
     * @throws DotSecurityException if the logged-in user is not a CMS administrator with access
     *                              to the Maintenance portlet
     */
    public Map getCleanAssetsStatus ()
            throws DotDataException, DotSecurityException, PortalException, SystemException {

        requireAdminWithMaintenancePortlet();

        //Getting the running clean assets thread
        CleanAssetsThread cleanAssetsThread = CleanAssetsThread.getInstance(false, false);
        BasicProcessStatus processStatus = cleanAssetsThread.getProcessStatus();

        //Return its current running status
        return processStatus.buildStatusMap();
    }

    /**
     * Returns the logged-in user after checking that they can run maintenance routines: a CMS
     * administrator for whom the layout API grants the Maintenance portlet, the same two checks
     * the maintenance REST endpoints make. The layout API grants every portlet to a CMS
     * administrator who is a back-end user, so the portlet check only rejects an administrator who
     * is not one. Each rejected request is written once to the security log, with the user id
     * only.
     *
     * @return the logged-in {@link User}
     * @throws DotSecurityException if nobody is logged in, the logged-in user is not a CMS
     *                              administrator, or the layout API does not grant them the
     *                              Maintenance portlet
     * @throws DotDataException     if the user's portlets cannot be loaded
     * @throws PortalException      if the logged-in user cannot be resolved
     * @throws SystemException      if the logged-in user cannot be resolved
     */
    private User requireAdminWithMaintenancePortlet()
            throws DotDataException, DotSecurityException, PortalException, SystemException {
        final User user = DwrUtil.getLoggedInUser();
        if (!user.isAdmin()) {
            SecurityLogger.logInfo(CMSMaintenanceAjax.class, String.format(
                    "User [%s] is not a CMS administrator, which maintenance routines require",
                    user.getUserId()));
            throw new DotSecurityException("Maintenance routines require a CMS administrator");
        }
        if (!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.MAINTENANCE.toString(), user)) {
            SecurityLogger.logInfo(CMSMaintenanceAjax.class, String.format(
                    "User [%s] does not have the Maintenance portlet, which maintenance routines require",
                    user.getUserId()));
            throw new DotSecurityException("Maintenance routines require the Maintenance portlet");
        }
        return user;
    }

}
