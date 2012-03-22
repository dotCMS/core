package com.dotmarketing.portlets.cmsmaintenance.factories;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.cmsmaintenance.action.ViewCMSMaintenanceAction;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class is use to fix identifiers inconsistencies in db,
 * solving the problem of identifiers without a working or live asset
 * @author Oswaldo
 *
 */
public class CMSMaintenanceFactory {


/*
	public static Map fixAssetsInconsistencies() throws Exception
	{
		Logger.debug("start");
		Map returnValue = null;
		int counter = 0;
		if(!FixAssetsProcessStatus.getRunning())
		{
			FixAssetsProcessStatus.startProgress();
			DotHibernate.startTransaction();
			try
			{
				//Check the consistency of the identifiers
				List<WebAsset> webAssets = new ArrayList<WebAsset>();
				webAssets.addAll(InodeFactory.getInodesOfClass(Contentlet.class));
				webAssets.addAll(InodeFactory.getInodesOfClass(Container.class));
				webAssets.addAll(InodeFactory.getInodesOfClass(File.class));
				webAssets.addAll(InodeFactory.getInodesOfClass(HTMLPage.class));
				webAssets.addAll(InodeFactory.getInodesOfClass(Link.class));
				webAssets.addAll(InodeFactory.getInodesOfClass(Template.class));
				int total = webAssets.size();
				total += IdentifierFactory.getAllIdentifiers().size();
				FixAssetsProcessStatus.setTotal(total);
				for(WebAsset webAsset : webAssets)
				{
					//Inode Indentifier
					long identifierInode = webAsset.getIdentifier();
					long inodeInode = webAsset.getInode();
					Logger.debug("inode " + inodeInode);
					//Parent Identifier
					Identifier parentIdentifier = (Identifier) InodeFactory.getParentOfClass(webAsset, Identifier.class);
					long parentIdentifierInode = parentIdentifier.getInode();
					if(identifierInode == parentIdentifierInode)
					{
						//good
						if(identifierInode != 0 && parentIdentifierInode != 0)
						{
							FixAssetsProcessStatus.addActual();
							continue;
						}
						//both wrong
						else
						{
							InodeFactory.deleteInode(webAsset);
							FixAssetsProcessStatus.addAError();
							counter++;
						}
					}
					else if(identifierInode != parentIdentifierInode)
					{
						//parent wrong
						if(identifierInode != 0)
						{
							try
							{
								Identifier identifier = APILocator.getIdentifierAPI().find(identifierInode);
								if(identifier.getInode() != 0)
								{
									Tree tree = TreeFactory.getTree(parentIdentifierInode,inodeInode);
									tree.setParent(identifierInode);
									tree.setChild(inodeInode);
									tree.setRelationType("child");
									TreeFactory.saveTree(tree);
								}
								else
								{
									InodeFactory.deleteInode(webAsset);
								}
							}
							catch(Exception ex)
							{
								InodeFactory.deleteInode(webAsset);
							}
							FixAssetsProcessStatus.addAError();
							counter++;
						}
						//identifier wrong
						else
						{
							Identifier identifier = APILocator.getIdentifierAPI().find(parentIdentifierInode);
							if(identifier.getInode() != 0)
							{
								webAsset.setIdentifier(parentIdentifierInode);
								HibernateUtil.saveOrUpdate(webAsset);
							}
							else
							{
								InodeFactory.deleteInode(webAsset);
							}
							FixAssetsProcessStatus.addAError();
							counter++;
						}
					}
					FixAssetsProcessStatus.addActual();
				}

				//Check the working and live versions
				List<Identifier> identifiersList = (List<Identifier>) IdentifierFactory.getAllIdentifiers();
				for(Identifier identifier :identifiersList)
				{
					Logger.debug("identifier inode" + identifier.getInode());
					//Check if exits the identifier have not associated at least one contentlet working or live
					List<WebAsset> webAssetList = (List<WebAsset>) IdentifierFactory.getVersionsandLiveandWorkingChildrenOfClass(identifier,WebAsset.class);
					if(webAssetList.size() > 0)
					{

						boolean dontExistWorkingorLive = true;
						for(WebAsset inode : webAssetList)
						{
							if(inode.isWorking())
							{
								dontExistWorkingorLive = false;
								continue;
							}
						}

						if(dontExistWorkingorLive)
						{
							WebAsset inode = (WebAsset) CMSMaintenanceFactory._getLastVersion(webAssetList);
							inode.setWorking(true);
							HibernateUtil.saveOrUpdate(inode);
							FixAssetsProcessStatus.addAError();
							counter++;
						}
					}
					FixAssetsProcessStatus.addActual();
				}
				DotHibernate.commitTransaction();
				returnValue = FixAssetsProcessStatus.getFixAssetsMap();
				FixAssetsProcessStatus.stopProgress();
				Logger.debug("end");
			}
			catch(Exception e)
			{
				Logger.debug(CMSMaintenanceFactory.class, e.getMessage());
				Logger.warn(CMSMaintenanceFactory.class, e.toString());
				DotHibernate.rollbackTransaction();
				FixAssetsProcessStatus.stopProgress();
				FixAssetsProcessStatus.setActual(-1);
			}
		}
		return returnValue;
	}
*/
	public static Map fixAssetsInconsistencies() throws Exception {
		Logger.info(CMSMaintenanceFactory.class, "Beginning fixAssetsInconsistencies");
		Map returnValue = null;
		int counter = 0;

		final String fix1query = "update inode set identifier = ? where inode = ?";

		final String fix2ContentletQuery = "select c.* from contentlet c, inode i where i.inode = c.inode and c.identifier = ? order by live desc, mod_date desc";
		final String fix3ContentletQuery = "update contentlet set working = ? where inode = ?";

		final String fix2ContainerQuery = "select c.* from containers c, inode i where i.inode = c.inode and c.identifier = ? order by live desc, mod_date desc";
		final String fix3ContainerQuery = "update containers set working = ? where inode = ?";

		final String fix2FileAssetQuery = "select c.* from file_asset c, inode i where i.inode = c.inode and c.identifier = ? order by live desc, mod_date desc";
		final String fix3FileAssetQuery = "update file_asset set working = ? where inode = ?";

		final String fix2HtmlPageQuery = "select c.* from htmlpage c, inode i where i.inode = c.inode and c.identifier = ? order by live desc, mod_date desc";
		final String fix3HtmlPageQuery = "update htmlpage set working = ? where inode = ?";

		final String fix2LinksQuery = "select c.* from links c, inode i where i.inode = c.inode and c.identifier = ? order by live desc, mod_date desc";
		final String fix3LinksQuery = "update links set working = ? where inode = ?";

		final String fix2TemplatesQuery = "select c.* from template c, inode i where i.inode = c.inode and c.identifier = ? order by live desc, mod_date desc";
		final String fix3TemplatesQuery = "update template set working = ? where inode = ?";

		/*final String fix2TreeQuery = "select child,parent,relation_type from tree left join inode on tree.child  = inode.inode where inode.inode is null";
		final String fix3TreeQuery = "select child,parent,relation_type from tree left join inode on tree.parent = inode.inode where inode.inode is null";*/

		final String fix2TreeQuery = "select child,parent,relation_type from tree left join inode on tree.child  = inode.inode left join identifier " +
		 							 "on tree.child = identifier.id where inode.inode is null and identifier.id is null ";
		final String fix3TreeQuery = "select child,parent,relation_type from tree left join inode on tree.parent = inode.inode left join identifier " +
		 							 "on tree.parent = identifier.id where inode.inode is null and identifier.id is null";
		final String fix4TreeQuery = "delete from tree where child = ? and parent = ? and relation_type = ?";

		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			HibernateUtil.startTransaction();
			try {
				DotConnect db = new DotConnect();

				Logger.info(CMSMaintenanceFactory.class, "Deleting all assets with no identifier");
				int total = MaintenanceUtil.deleteAllAssetsWithNoIdentifier();

				MaintenanceUtil.cleanInodeTableData();

				String query = "select distinct d.* " +
						"from identifier d, inode i, contentlet c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and d.inode not in (select d.inode " +
						"from identifier d, inode i, contentlet " +
						"c where c.inode = i.inode and i.identifier = d.inode " +
						"and c.working = " + DbConnectionFactory.getDBTrue() + ") " +
								"and i.type = 'contentlet' ";

				Logger.debug(CMSMaintenanceFactory.class, "Running query for Contentlets: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> contentletIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + contentletIds.size() + " Contentlets");
				total += contentletIds.size();

				query = "select distinct d.* " +
						"from identifier d, inode i, containers c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and d.inode not in (select d.inode " +
						"from identifier d, inode i, containers c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and c.working = " + DbConnectionFactory.getDBTrue() + ") " +
						"and i.type = 'containers' ";


				Logger.debug(CMSMaintenanceFactory.class, "Running query for Containers: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> containerIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + containerIds.size() + " Containers");
				total += containerIds.size();

				query = "select distinct d.* " +
						"from identifier d, inode i, file_asset c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and d.inode not in (select d.inode " +
						"from identifier d, inode i, file_asset c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and c.working = " + DbConnectionFactory.getDBTrue() + ") " +
						"and i.type = 'file_asset' ";

				Logger.debug(CMSMaintenanceFactory.class, "Running query for file assets: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> fileAssetIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + fileAssetIds.size() + " File Assets");
				total += fileAssetIds.size();

				query = "select distinct d.* " +
						"from identifier d, inode i, htmlpage c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and d.inode not in (select d.inode " +
						"from identifier d, inode i, htmlpage c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and c.working = " + DbConnectionFactory.getDBTrue() + ") " +
						"and i.type = 'htmlpage' ";

				Logger.debug(CMSMaintenanceFactory.class, "Running query for html pages: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> htmlpageIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + htmlpageIds.size() + " Html pages");
				total += htmlpageIds.size();

				query = "select distinct d.* " +
						"from identifier d, inode i, links c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and d.inode not in (select d.inode " +
						"from identifier d, inode i, links c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and c.working = " + DbConnectionFactory.getDBTrue() + ") " +
						"and i.type = 'links' ";

				Logger.debug(CMSMaintenanceFactory.class, "Running query for links: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> linkIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + linkIds.size() + " Links");
				total += linkIds.size();

				query = "select distinct d.* " +
						"from identifier d, inode i, template c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and d.inode not in (select d.inode " +
						"from identifier d, inode i, template c " +
						"where c.inode = i.inode and i.identifier = d.inode " +
						"and c.working = " + DbConnectionFactory.getDBTrue() + ") " +
						"and i.type = 'template' ";

				Logger.debug(CMSMaintenanceFactory.class, "Running query for templates: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> templateIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + templateIds.size() + " Templates");
				total += templateIds.size();

				//Tree Query (Child)
				query =  fix2TreeQuery;
				Logger.debug(CMSMaintenanceFactory.class, "Running query for tree: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> treeChildren = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + treeChildren.size() + " Tree");
				total += treeChildren.size();

				//Tree Query (Child)
				query =  fix3TreeQuery;
				Logger.debug(CMSMaintenanceFactory.class,"Running query for tree: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> treeParents = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class,"Found " + treeParents.size() + " Tree");
				total += treeParents.size();

				Logger.info(CMSMaintenanceFactory.class,"Total number of assets: " + total);
				FixAssetsProcessStatus.setTotal(total);

				long inodeInode;
				long parentIdentifierInode;


				//Check the working and live versions of contentlets
				String identifierInode;
				List<HashMap<String, String>> versions;
				HashMap<String, String> version;
				String versionWorking;
				String DbConnFalseBoolean = DbConnectionFactory.getDBFalse().trim().toLowerCase();

				char DbConnFalseBooleanChar;
				if (DbConnFalseBoolean.charAt(0) == '\'')
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(1);
				else
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(0);

				String inode;

				Logger.info(CMSMaintenanceFactory.class, "Verifying working and live versions for "+ contentletIds.size()+" contentlets");
				for (HashMap<String, String> identifier : contentletIds) {
					identifierInode = identifier.get("inode");

				    Logger.debug(CMSMaintenanceFactory.class, "identifier inode " + identifierInode);
				    Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix2ContentletQuery);
					db.setSQL(fix2ContentletQuery);
					db.addParam(identifierInode);
					versions = db.getResults();

					if (0 < versions.size()) {
						version = versions.get(0);
						versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class, "Non Working Contentlet inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix3ContentletQuery);
						db.setSQL(fix3ContentletQuery);
						db.addParam(DbConnectionFactory.getDBTrue().replaceAll("'", ""));
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}


				//Check the working and live versions of containers
				Logger.info(CMSMaintenanceFactory.class, "Verifying working and live versions for "+ containerIds.size()+" containers");
				for (HashMap<String, String> identifier : containerIds) {
					identifierInode = identifier.get("inode");

				    Logger.debug(CMSMaintenanceFactory.class, "identifier inode " + identifierInode);
				    Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix2ContainerQuery);

					db.setSQL(fix2ContainerQuery);
					db.addParam(identifierInode);
					versions = db.getResults();

					if (0 < versions.size()) {
						version = versions.get(0);
						versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class, "Non Working Container inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix3ContainerQuery);
						db.setSQL(fix3ContainerQuery);
						db.addParam(DbConnectionFactory.getDBTrue().replaceAll("'", ""));
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}

				//Check the working and live versions of file assets
				Logger.info(CMSMaintenanceFactory.class, "Verifying working and live versions for "+ fileAssetIds.size()+" file_assets");
				for (HashMap<String, String> identifier : fileAssetIds) {
					identifierInode = identifier.get("inode");

				    Logger.debug(CMSMaintenanceFactory.class, "identifier inode " + identifierInode);
				    Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix2FileAssetQuery);

					db.setSQL(fix2FileAssetQuery);
					db.addParam(identifierInode);
					versions = db.getResults();

					if (0 < versions.size()) {
						version = versions.get(0);
						versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class, "Non Working File inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix3FileAssetQuery);
						db.setSQL(fix3FileAssetQuery);
						db.addParam(DbConnectionFactory.getDBTrue().replaceAll("'", ""));
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;

					}

					FixAssetsProcessStatus.addActual();
				}

				//Check the working and live versions of html pages
				Logger.info(CMSMaintenanceFactory.class, "Verifying working and live versions for "+ htmlpageIds.size()+" htmlpages");
				for (HashMap<String, String> identifier : htmlpageIds) {
					identifierInode = identifier.get("inode");

				    Logger.debug(CMSMaintenanceFactory.class, "identifier inode " + identifierInode);
				    Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix2HtmlPageQuery);

					db.setSQL(fix2HtmlPageQuery);
					db.addParam(identifierInode);
					versions = db.getResults();

					if (0 < versions.size()) {
						version = versions.get(0);
						versionWorking = version.get("working").trim().toLowerCase();

//						Logger.info("Step 5 versionWorking: " + versionWorking);

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class, "Non Working HTML page inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix3HtmlPageQuery);
						db.setSQL(fix3HtmlPageQuery);
						db.addParam(DbConnectionFactory.getDBTrue().replaceAll("'", ""));
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}

				//Check the working and live versions of links
				Logger.info(CMSMaintenanceFactory.class, "Verifying working and live versions for "+ linkIds.size()+" links");
				for (HashMap<String, String> identifier : linkIds) {
					identifierInode = identifier.get("inode");

				    Logger.debug(CMSMaintenanceFactory.class, "identifier inode " + identifierInode);
				    Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix2LinksQuery);

					db.setSQL(fix2LinksQuery);
					db.addParam(identifierInode);
					versions = db.getResults();

					if (0 < versions.size()) {
						version = versions.get(0);
						versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class, "Non Working Link inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix3LinksQuery);
						db.setSQL(fix3LinksQuery);
						db.addParam(DbConnectionFactory.getDBTrue().replaceAll("'", ""));
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}

//				Logger.info("Step 7: " + template.size());

				//Check the working and live versions of templates
				Logger.info(CMSMaintenanceFactory.class, "Verifying working and live versions for "+ templateIds.size()+" templates");
				for (HashMap<String, String> identifier : templateIds) {
					identifierInode = identifier.get("inode");

				    Logger.debug(CMSMaintenanceFactory.class, "identifier inode " + identifierInode);
				    Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix2TemplatesQuery);

					db.setSQL(fix2TemplatesQuery);
					db.addParam(identifierInode);
					versions = db.getResults();

					if (0 < versions.size()) {
						version = versions.get(0);
						versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class, "Non Working Template inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class, "Running query: "+ fix3TemplatesQuery);
						db.setSQL(fix3TemplatesQuery);
						db.addParam(DbConnectionFactory.getDBTrue().replaceAll("'", ""));
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}

				//Check the tree entries that doesn't have a child o parent in the inode table
				treeChildren.addAll(treeParents);
				Logger.info(CMSMaintenanceFactory.class,"Fixing " + treeChildren.size()+ " tree entries");
				for (HashMap<String, String> tree : treeChildren)
				{
				    Logger.debug(CMSMaintenanceFactory.class,"Running query: "+ fix4TreeQuery);
				    try
				    {
				    	db.setSQL(fix4TreeQuery);
				    	db.addParam(tree.get("child"));
				    	db.addParam(tree.get("parent"));
				    	db.addParam(tree.get("relation_type"));
				    	db.getResults();
				    }
				    catch(Exception ex)
				    {
				    	FixAssetsProcessStatus.addAError();
				    	counter++;
				    }
				    FixAssetsProcessStatus.addActual();
				}

				HibernateUtil.commitTransaction();
				returnValue = FixAssetsProcessStatus.getFixAssetsMap();
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(CMSMaintenanceFactory.class, "Ending fixAssetsInconsistencies");
			} catch(Exception e) {
				Logger.debug(CMSMaintenanceFactory.class,"There was a problem fixing asset inconsistencies",e);
				Logger.warn(CMSMaintenanceFactory.class,"There was a problem fixing asset inconsistencies",e);
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.stopProgress();
				FixAssetsProcessStatus.setActual(-1);
			}
		}

		return returnValue;
	}

	public static int deleteOldAssetVersions (Date assetsOlderThan) {
		int counter = 0;
		int auxCount = 0;

		try {
			HibernateUtil.startTransaction();
		} catch (DotHibernateException e) {
			Logger.error(CMSMaintenanceFactory.class, e.getMessage());
		}
		try	{

			String condition = "live <> "+DbConnectionFactory.getDBTrue()+" and working <> "+
			DbConnectionFactory.getDBTrue()+" and mod_date < '" + UtilMethods.dateToHTMLDate(assetsOlderThan,"dd-MMM-yyyy").toUpperCase() + "'";

			Logger.info(CMSMaintenanceFactory.class, "Starting deleteOldAssetVersions for date: "+ UtilMethods.dateToHTMLDate(assetsOlderThan,"yyyy-MM-dd")+" and condition: " + condition);

			ContentletAPI conAPI = APILocator.getContentletAPI();

			Logger.info(CMSMaintenanceFactory.class, "Removing Contentlets");
			auxCount = conAPI.deleteOldContent(assetsOlderThan, 500);
			counter  = auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Contentlets");

			Logger.info(CMSMaintenanceFactory.class, "Removing HTML Pages");
			auxCount = deleteOldAssets(assetsOlderThan, "htmlpage", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " HTML Pages");

			Logger.info(CMSMaintenanceFactory.class, "Removing Containers");
			auxCount = deleteOldAssets(assetsOlderThan, "containers", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Containers");

			Logger.info(CMSMaintenanceFactory.class, "Removing Templates");
			auxCount =  deleteOldAssets(assetsOlderThan, "template", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Templates");

			Logger.info(CMSMaintenanceFactory.class, "Removing Links");
			auxCount = deleteOldAssets(assetsOlderThan, "links", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Links");

			Logger.info(CMSMaintenanceFactory.class, "Removing File Assets");

			auxCount = deleteOldFiles(assetsOlderThan, 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " File Assets");

			auxCount = deleteOldBinary();
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Binaries");

			Logger.info(CMSMaintenanceFactory.class, "Finished removing old asset versions, removed "+counter+" assets");


		}catch(Exception ex){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e) {
				Logger.error(CMSMaintenanceFactory.class, e.getMessage());
			}
			Logger.debug(CMSMaintenanceFactory.class,"There was a problem deleting old asset versions",ex);
			Logger.warn(CMSMaintenanceFactory.class,"There  was a problem deleting old asset versions",ex);
			Logger.error(ViewCMSMaintenanceAction.class,ex.toString(), ex);
			return -1;
		}

		try {
			HibernateUtil.commitTransaction();
		} catch (DotHibernateException e) {
			Logger.error(CMSMaintenanceFactory.class, e.getMessage());
		}
		return counter;

	}

	/**
	 * This method get the File date from a dotmarketing file object
	 * @param file dotmarketing File object
	 * @return Java io.File
	 * @throws IOException
	 */
	private static java.io.File getAssetIOFile (com.dotmarketing.portlets.files.model.File file) throws IOException {

		String fileName = file.getFileName();
		String suffix = UtilMethods.getFileExtension(fileName);

		String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
		String fileInode = file.getInode();

		// creates the path where to save the working file based on the inode
		Logger.debug(CMSMaintenanceFactory.class, "Creating path to save the working file with inode: "+ fileInode);
		String fileFolderPath = String.valueOf(fileInode);
		if (fileFolderPath.length() == 1) {
			fileFolderPath = fileFolderPath + "0";
		}

		fileFolderPath = assetsPath + java.io.File.separator +
		fileFolderPath.substring(0, 1) + java.io.File.separator +
		fileFolderPath.substring(1, 2);

		new java.io.File(fileFolderPath).mkdirs();

		String filePath = fileFolderPath + java.io.File.separator +
		fileInode + "." + suffix;

		// creates the new file as
		// inode{1}/inode{2}/inode.file_extension
		java.io.File assetFile = new java.io.File(filePath);
		if (!assetFile.exists())
			Logger.debug(CMSMaintenanceFactory.class, "Creating new file in:  "+ filePath);
			assetFile.createNewFile();

		return assetFile;
	}


	/**
	 * Remove assets in DB older than the specified date.
	 *
	 * @param assetsOlderThan
	 * @param asset
	 * @param offset
	 * @return int
	 * @exception DotDataException
	 */
	@SuppressWarnings("unchecked")
	private static int deleteOldAssets(Date assetsOlderThan, String asset, int offset) throws DotDataException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(assetsOlderThan);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date date = calendar.getTime();
		DotConnect dc = new DotConnect();

		String countSQL = ("select count(*) as count from "+ asset);
		dc.setSQL(countSQL);
		List<Map<String, String>> result = dc.loadResults();
		int before = Integer.parseInt(result.get(0).get("count"));
		String versionInfoTable = asset.equals("containers") || asset.equals("links") ? asset.substring(0, asset.length()-1) + "_version_info" : asset + "_version_info";
		versionInfoTable = asset.equals("file_asset") ? "fileasset_version_info" : versionInfoTable;

		StringBuffer getInodesSQL = new StringBuffer("select inode from inode where inode in (select inode from ").append(asset).append(" a where  ");
		getInodesSQL.append("a.mod_date < ? and a.inode not in (select working_inode from ").append(versionInfoTable).append(") ");
		getInodesSQL.append(" and a.inode not in (select live_inode from ").append(versionInfoTable).append(" )) ");

		dc.setSQL(getInodesSQL.toString());
		dc.addParam(date);
		List<Map<String, Object>> results = dc.loadResults();
		int lenght = results.size();
		boolean first = true;

		StringBuffer deleteContentletSQL = new StringBuffer("delete from ").append(asset).append(" a where  ");
		deleteContentletSQL.append("a.mod_date < ? and a.inode not in (select working_inode from ").append(versionInfoTable).append(") ");
		deleteContentletSQL.append(" and a.inode not in (select live_inode from ").append(versionInfoTable).append(" ) ");

		dc.setSQL(deleteContentletSQL.toString());
		dc.addParam(date);
		dc.loadResult();

		Logger.info(CMSMaintenanceFactory.class, "Deleting "+lenght+" assets");
		if(lenght>0){
			StringBuffer deleteInodeSQL = new StringBuffer("delete from inode where inode in(");
			first = true;
			List<String> inodesToDelete = new ArrayList<String>();

			for(int i = 0;i < lenght;i++)
			{
				Map<String, Object> hash = (Map<String, Object>) results.get(i);
				String inode = (String) hash.get("inode");
				inodesToDelete.add(inode);
				if(!first){
					deleteInodeSQL.append(",'" + inode + "'");
				}else{
					deleteInodeSQL.append("'" + inode + "'");

				}
				first = false;

				if((i % offset) == 0 && i != 0)
				{
					deleteInodeSQL.append(")");
					dc.setSQL(deleteInodeSQL.toString());
					MaintenanceUtil.cleanInodesFromTree(inodesToDelete, offset);
					dc.loadResult();
					deleteInodeSQL = new StringBuffer("delete from inode where inode in(");
					first = true;

				}

			}
			if(!(lenght % offset == 0) && inodesToDelete.size()>0)
			{
				deleteInodeSQL.append(")");
				dc.setSQL(deleteInodeSQL.toString());
				MaintenanceUtil.cleanInodesFromTree(inodesToDelete, offset);
				dc.loadResult();
			}

		}else{
			Logger.info(CMSMaintenanceFactory.class, "No assets to delete");
		}
		MaintenanceUtil.cleanMultiTreeTable();

		dc.setSQL(countSQL);
		result = dc.loadResults();
		int after = Integer.parseInt(result.get(0).get("count"));
		return before - after;

	}

	/**
	 * Remove assets in the file system older than the speficied date.
	 *
	 * @param assetsOlderThan
	 * @param offset
	 * @return int
	 * @exception DotDataException
	 */
	@SuppressWarnings("unchecked")
	private static int deleteOldFiles(Date assetsOlderThan, int offset) throws DotDataException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(assetsOlderThan);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date date = calendar.getTime();
		DotConnect dc = new DotConnect();

		//List<com.dotmarketing.portlets.files.model.File> fileList = (List<com.dotmarketing.portlets.files.model.File>)InodeFactory.getInodesOfClassByCondition(com.dotmarketing.portlets.files.model.File.class, condition);
		StringBuffer getInodesSQL = new StringBuffer("select inode from inode where inode in (select inode from file_asset where mod_date < ? ");
		getInodesSQL.append(" and inode not in (select working_inode from fileasset_version_info) and inode not in (select live_inode from fileasset_version_info)) ");
		dc.setSQL(getInodesSQL.toString());
		dc.addParam(date);
		List<Map<String, String>> results = dc.loadResults();

		com.dotmarketing.portlets.files.model.File file;
		FileAPI fileAPI = APILocator.getFileAPI();

		User user = null;

		try {
			user = APILocator.getUserAPI().getSystemUser();
		} catch (Exception e) {
			Logger.warn(CMSMaintenanceFactory.class, e.toString());
		}

		for(Map<String, String> inode: results){
			try {
				file = fileAPI.get(inode.get("inode"), user, false);
				File dataFile = getAssetIOFile(file);
				dataFile.delete();

				//search for tumbnails and resized images
				Logger.info(CMSMaintenanceFactory.class, "Searching for thumbnails and resized images");
				if(UtilMethods.isImage(file.getFileName())){
					String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
					String fileInode = file.getInode();

					// creates the path where to save the working file based on the inode
					String fileFolderPath = String.valueOf(fileInode);
					if (fileFolderPath.length() == 1) {
						fileFolderPath = fileFolderPath + "0";
					}

					fileFolderPath = assetsPath + java.io.File.separator +
					fileFolderPath.substring(0, 1) + java.io.File.separator +
					fileFolderPath.substring(1, 2);
					java.io.File[] fileArrays = new java.io.File(fileFolderPath).listFiles();
					int fileCount = 0;
					Logger.debug(CMSMaintenanceFactory.class, "Found "+ fileArrays.length + " thumbnails and resized images");
					for(java.io.File file2 : fileArrays){
						if(file2.getName().indexOf(file.getInode()+"_resized") != -1 ||
								file2.getName().indexOf(file.getInode()+"_thumb") != -1){
							Logger.debug(CMSMaintenanceFactory.class, "Deleting file: "+ file2.getName());
							file2.delete();
							fileCount+=1;
						}
					}
					Logger.info(CMSMaintenanceFactory.class, "Deleted "+fileCount+" thumbnails and resized images");
				}
			} catch (Exception e) {
				Logger.warn(CMSMaintenanceFactory.class, e.toString());
			}
		}

		return deleteOldAssets(assetsOlderThan, "file_asset", 500);
	}

	private static int deleteOldBinary() {
        int result = 0;

		String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
		File assetsRootFolder = new File(assetsPath);
		if (!assetsRootFolder.exists() || !assetsRootFolder.isDirectory())
			return result;

		String reportsPath = "";
		if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
			reportsPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + Config.getStringProperty("REPORT_PATH");
		} else {
			reportsPath = Config.CONTEXT.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + File.separator + Config.getStringProperty("REPORT_PATH"));
		}
		File reportsFolder = new File(reportsPath);

		String messagesPath = "";
		if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
			messagesPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + "messages";
		} else {
			messagesPath = Config.CONTEXT.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + "messages");
		}
		File messagesFolder = new File(messagesPath);

		ContentletAPI contentletAPI = APILocator.getContentletAPI();
		Contentlet contentlet;

		User user = null;
		try {
			user = APILocator.getUserAPI().getSystemUser();
		} catch (Exception e) {
			Logger.warn(CMSMaintenanceFactory.class, e.toString());
		}

		File[] filesLevel1 = assetsRootFolder.listFiles();
		for (File fileLevel1: filesLevel1) {
			if(!fileLevel1.getPath().equals(assetsRootFolder.getPath()+java.io.File.separator+"license")
					&& !fileLevel1.getPath().equals(reportsFolder.getPath())
					&& !fileLevel1.getPath().equals(messagesFolder.getPath())){
				if (fileLevel1.isDirectory()) {
					File[] filesLevel2 = fileLevel1.listFiles();
					for (File fileLevel2: filesLevel2) {
						if (fileLevel2.isDirectory()) {
							File[] filesLevel3 = fileLevel2.listFiles();
							for (File fileLevel3: filesLevel3) {
								if (fileLevel3.isDirectory()) {
									try {
										contentlet = contentletAPI.find(fileLevel3.getName(), user, false);
										if ((contentlet == null) || !InodeUtils.isSet(contentlet.getInode())) {
											deleteFolder(fileLevel3);

											++result;
										}
									} catch (Exception e) {
										Logger.warn(CMSMaintenanceFactory.class, e.toString());
									}
								}
							}
						}
					}
				}
			}
		}

		return result;
	}

	private static boolean deleteFolder(File folder) {
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			boolean result;
			for (File file: files) {
				result = deleteFolder(file);
				if (result == false)
					return result;
			}
		}

		return folder.delete();
	}
}
