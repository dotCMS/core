package com.dotmarketing.db;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ImportExportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;

public class DotCMSInitDb {

	@CloseDBIfOpened
	private static boolean isConfigured () {

		DotConnect db = new DotConnect();
		db.setSQL("select count(*) as test from inode");

		int test = db.getInt("test");
		return (test > 0);
	}

	@CloseDBIfOpened
	public static void InitializeDb() throws DotDataException {

		final boolean configured = isConfigured();

		if (!configured) {
			if(Config.getBooleanProperty("STARTERSITE_BUILD", true)){
				Logger.info(DotCMSInitDb.class,"There are no inodes - initializing db with starter site");
				try {
					PrintWriter pw = new PrintWriter(new StringWriter());
					loadStarterSite(pw);
					Logger.info(DotCMSInitDb.class, pw.toString());
				} catch (IOException e) {
					Logger.error(DotCMSInitDb.class, "Unable to load starter site", e);
				}
			}else{
				Logger.info(DotCMSInitDb.class,"There are no inodes - initializing db for first time use");
				buildDefaultData();
			}
		}else {
			Logger.info(DotCMSInitDb.class,"inodes exist, skipping initialization of db");
		}
	}

	private static void buildDefaultData() throws DotDataException {
		try {
			HostAPI hostAPI = APILocator.getHostAPI();
			LocalTransaction.wrap(() -> PublicCompanyFactory.createDefaultCompany());
			// Ensures that default groups are set up
	//		GroupFactory.createDefaultGroups();
	//		try {
	//			LayoutFactory.createDefaultLayouts();
	//		} catch (Exception e) {
	//			throw new DotDataException(e.getMessage(), e);
	//		}
			// Creating the default host
			User systemUser = APILocator.getUserAPI().getSystemUser();
			List<Host> hosts = hostAPI.findAll(systemUser, false);
			if (hosts.size() == 0) {
				Logger.debug(DotCMSInitDb.class, "Creating Default Host");
				hostAPI.findDefaultHost(systemUser, false);
			}
	        //Create Default Language
	        APILocator.getLanguageAPI().createDefaultLanguage();
			// Creating the default content structures if it not exists.
			StructureFactory.createDefaultStructure();
		} catch (DotSecurityException e) {
			Logger.fatal(DotCMSInitDb.class, "Unable to initialize default data", e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (Exception e) {
			Logger.fatal(DotCMSInitDb.class, "Unable to initialize default data", e);
			new DotDataException(e.getMessage(), e);
		}
	}

	private static void loadStarterSite(PrintWriter pw) throws IOException{
		
		String starter = Config.getStringProperty("STARTER_DATA_LOAD");
		File starterZip = null;
		
		if(UtilMethods.isSet(starter)){

			// First we try using the real path
            starterZip = new File(FileUtil.getRealPath(starter));

            // Then we try to see if there is an absolute path (or relative in case of integration tests)
            if (!starterZip.exists()) {
				starterZip = new File(starter);
			}
		}
		
		if(starterZip==null || (starterZip!=null && !starterZip.exists())){
			String starterSitePath = "/starter.zip";
			String zipPath = FileUtil.getRealPath(starterSitePath);
			starterZip = new File(zipPath); 
		 }

		ImportExportUtil ieu = new ImportExportUtil();
		if(ieu.validateZipFile(starterZip))
			Try.run((() -> ieu.doImport(pw))).onFailure(e -> {
							throw new DotRuntimeException(e);
			});
	}
}
