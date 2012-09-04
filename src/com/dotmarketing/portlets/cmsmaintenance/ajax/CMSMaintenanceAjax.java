package com.dotmarketing.portlets.cmsmaintenance.ajax;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import net.sf.hibernate.HibernateException;

import org.directwebremoting.WebContextFactory;
import org.quartz.JobExecutionContext;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.fixtask.FixTasksExecutor;
import com.dotmarketing.logConsole.model.LogMapper;
import com.dotmarketing.logConsole.model.LogMapperRow;
import com.dotmarketing.plugin.model.Plugin;
import com.dotmarketing.plugin.model.PluginProperty;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.portlets.containers.model.ContainerVersionInfo;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardUserPreferences;
import com.dotmarketing.portlets.files.model.FileAssetVersionInfo;
import com.dotmarketing.portlets.htmlpages.model.HTMLPageVersionInfo;
import com.dotmarketing.portlets.links.model.LinkVersionInfo;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.TemplateVersionInfo;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.HibernateCollectionConverter;
import com.dotmarketing.util.HibernateMapConverter;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.ImageLocalManagerUtil;
import com.liferay.portal.ejb.PortletPreferencesLocalManagerUtil;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.Mapper;

public class CMSMaintenanceAjax {

    public Map getReindexationProgress() throws DotDataException {
    	validateUser();
        return ESReindexationProcessStatus.getProcessIndexationMap();
    }
    
    
    
    public boolean deleteIndex(String indexName){
    	validateUser();
    	ESIndexAPI esapi= new ESIndexAPI();
    	
    	return  APILocator.getContentletIndexAPI().delete(indexName);
    }
    
    
    public boolean validateUser() {    	
    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = null;
        try {
        	user = com.liferay.portal.util.PortalUtil.getUser(req);
        	if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)){
        		throw new DotSecurityException("User does not have access to the CMS Maintance Portlet");
        	}
        	return true;
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            throw new DotRuntimeException (e.getMessage());
        }    	
    }
    
    
    public Map stopReindexation() throws DotDataException {
    	validateUser();
    	ReindexThread.getInstance().stopFullReindexation();
        return ESReindexationProcessStatus.getProcessIndexationMap();
    }
    
    public String cleanReindexStructure(String inode) throws DotDataException {
    	validateUser();
    	Structure structure = StructureCache.getStructureByInode(inode);
    	APILocator.getContentletIndexAPI().removeContentFromIndexByStructureInode(inode);
    	APILocator.getContentletAPI().refresh(structure);

    	Company d = PublicCompanyFactory.getDefaultCompany();
    	try {
			return LanguageUtil.get(d.getCompanyId(),d.getLocale(), "message.cmsmaintenance.cache.indexrebuilt");
		} catch (LanguageException e) {
			return "message.cmsmaintenance.cache.indexrebuilt";
		}
    }
    
    public void optimizeIndices() {
    	validateUser();
        ContentletIndexAPI api=APILocator.getContentletIndexAPI();
        List<String> indices=api.listDotCMSIndices();
        api.optimize(indices);
    }

    /**
	 * The path where tmp files are stored. This gets wiped alot
	 */
	private String backupTempFilePath = ConfigUtils.getBackupPath()+File.separator+"temp";

	private static String assetRealPath = null;
	private static String assetPath = "/assets";

    private  FixTasksExecutor fixtask=FixTasksExecutor.getInstance();

    public List <Map> fixAssetsInconsistencies() throws Exception
    {
    	validateUser();
        JobExecutionContext arg0=null;
        fixtask.execute(arg0);
		List result=fixtask.getTasksresults();
		 if(result.size()==0){
			 result=null;
		 }

        return result;

    }

    public List <Map> getFixAssetsProgress() throws Exception{
    	List result=fixtask.getTasksresults();
		 if(result.size()==0){
			 result=null;}
        return result;
    }



	public String[] deleteContentletsFromIdList(String List, String userId) throws PortalException, SystemException, DotDataException,DotSecurityException {

		ContentletAPI conAPI = APILocator.getContentletAPI();
		String[] inodes = List.split(",");
		Integer contdeleted = 0;
		String contnotfound = "";
		String conthasreqrel = "";
		String conthasnoperm = "";

		User user = UserLocalManagerUtil.getUserById(userId);
		for (int i = 0; i < inodes.length; i++) {
			inodes[i] = inodes[i].trim();
		}

		List<Contentlet> contentlets = new ArrayList<Contentlet>();

		for (String inode : inodes) {
			if (!inode.trim().equals("")) {
				contentlets.addAll(conAPI.getSiblings(inode));
			}
		}

		for (Contentlet contentlet : contentlets) {
			conAPI.delete(contentlet, APILocator.getUserAPI().getSystemUser(), true, true);
			contdeleted++;
		}

		String[] results = { contdeleted.toString(), contnotfound, conthasreqrel,conthasnoperm };

		return results;
	}

    public int removeOldVersions(String date) throws ParseException, SQLException, DotDataException {
        	Date assetsOlderThan = new SimpleDateFormat("MM/dd/yyyy").parse(date);
        	return CMSMaintenanceFactory.deleteOldAssetVersions(assetsOlderThan);
    }

    public String doBackupExport(String action, boolean dataOnly) throws IOException, ServletException, DotDataException {
    	validateUser();
			try {
				MaintenanceUtil.fixImagesTable();
			} catch (SQLException e) {
				Logger.error(this, e.getMessage());
			}

			String backupFilePath = ConfigUtils.getBackupPath();
			String backupTempFilePath = ConfigUtils.getBackupPath()+File.separator+"temp";

			File f = new File(backupFilePath);
			f.mkdirs();
			f = new File(backupTempFilePath);
			f.mkdirs();
			deleteTempFiles();
			String message = "";
			if(action.equals("createZip")) {
				if(!dataOnly){
					moveAssetsToBackupDir();
				}
				message = "Creating XML Files. ";
				createXMLFiles();
				String x = UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
				File zipFile = new File(backupFilePath + "/backup_" + x + "_.zip");
				message +="Zipping up to file:" + zipFile.getAbsolutePath();
				BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(zipFile));
				Logger.info(this, "Creating zipped backup file in "+ backupFilePath + " folder. Please wait");
				zipTempDirectoryToStream(bout);
				message +=". Done.";
				Logger.info(this, "Backup file was created in " +zipFile.getAbsolutePath());



			}
			/*else if(action.equals("downloadZip")) {

				message ="File Downloaded";
				String x = UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
				File zipFile = new File(backupFilePath + "/backup_" + x + "_.zip");

				ActionResponseImpl responseImpl = (ActionResponseImpl) response;
				HttpServletResponse httpResponse = responseImpl.getHttpServletResponse();
				httpResponse.setHeader("Content-type", "");
				httpResponse.setHeader("Content-Disposition", "attachment; filename=" + zipFile.getName());

				if(!dataOnly){
					moveAssetsToBackupDir();
				}

				createXMLFiles();

				zipTempDirectoryToStream(httpResponse.getOutputStream());

			}*/

			return message;

		}

		public void deleteTempFiles() {
			validateUser();
			Logger.info(this, "Deleting Temporary Files");
			File f = new File(backupTempFilePath);
			String[] _tempFiles = f.list();
			Logger.info(this, "Found "+_tempFiles.length+" Files");
			int count = 0;
			for (int i = 0; i < _tempFiles.length; i++) {
				f = new File(backupTempFilePath + "/" + _tempFiles[i]);
					if(f.isDirectory()){
						FileUtil.deltree(f);
					}
				count+=1;
				f.delete();
			}
			Logger.info(this, "Deleted " + count + " Files");
		}

		public void moveAssetsToBackupDir() throws FileNotFoundException, IOException{
			validateUser();
			String assetDir;
			File backupDir = new File(backupTempFilePath);
			backupDir.mkdirs();
			Logger.info(this, "Moving assets to back up directory: " + backupTempFilePath);
			if(!UtilMethods.isSet(assetRealPath)){
				assetDir = Config.CONTEXT.getRealPath(assetPath);
			}else{
				assetDir = assetRealPath;
			}
			FileUtil.copyDirectory(assetDir, backupTempFilePath + File.separator + "asset");

			//do not ship the license.
			String f = backupTempFilePath + File.separator + "asset" + File.separator + "license";
			FileUtil.deltree(f);

			String d = backupTempFilePath + File.separator + "asset" + File.separator + "dotGenerated";
			FileUtil.deltree(d);

		}

		/**
		 * This method will pull a list of all tables /classed being managed by
		 * hibernate and export them, one class per file to the backupTempFilePath
		 * as valid XML. It uses XStream to write the xml out to the files.
		 *
		 * @throws ServletException
		 * @throws IOException
		 * @author Will
		 * @throws DotDataException
		 * @throws DotCacheException 
		 */
		@SuppressWarnings("unchecked")
		public void createXMLFiles() throws ServletException, IOException, DotDataException {
			validateUser();
	//		deleteTempFiles();

			Logger.info(this, "Starting createXMLFiles()");

			Set<Class> _tablesToDump = new HashSet<Class>();
			try {

				/* get a list of all our tables */
				Map map = HibernateUtil.getSession().getSessionFactory().getAllClassMetadata();
				Iterator it = map.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry pairs = (Map.Entry) it.next();
					Class x = (Class) pairs.getKey();
					if (!x.equals(Inode.class) && !x.equals(Clickstream.class) && !x.equals(ClickstreamRequest.class) 
					        && !x.equals(Plugin.class) && !x.equals(PluginProperty.class))
						_tablesToDump.add(x);

				}
				XStream _xstream = null;
				HibernateUtil _dh = null;
				List _list = null;
				File _writing = null;
				BufferedOutputStream _bout = null;

				for (Class clazz : _tablesToDump) {
					//http://jira.dotmarketing.net/browse/DOTCMS-5031
	                if(PermissionReference.class.equals(clazz)){
	                	continue;
	                }

	                if(Contentlet.class.equals(clazz)){
	                	Logger.debug(this, "Processing contentlets. This will take a little bit longer...");
	                }

					_xstream = new XStream(new DomDriver());

					//http://jira.dotmarketing.net/browse/DOTCMS-6059
					if(clazz.equals(DashboardSummary404.class) || clazz.equals(DashboardUserPreferences.class)){
						_xstream.addDefaultImplementation(net.sf.hibernate.collection.Set.class, java.util.Set.class);
						_xstream.addDefaultImplementation(net.sf.hibernate.collection.List.class, java.util.List.class);
						_xstream.addDefaultImplementation(net.sf.hibernate.collection.Map.class, java.util.Map.class);
						Mapper mapper = _xstream.getMapper();
						_xstream.registerConverter(new HibernateCollectionConverter(mapper));
						_xstream.registerConverter(new HibernateMapConverter(mapper));
					}

					/*
					 * String _shortClassName =
					 * clazz.getName().substring(clazz.getName().lastIndexOf("."),clazz.getName().length());
					 * xstream.alias(_shortClassName, clazz);
					 */
					int i= 0;
					int step = 1000;
					int total =0;
					java.text.NumberFormat formatter = new java.text.DecimalFormat("0000000000");
					/* we will only export 10,000,000 items of any given type */
					for(i=0;i < 10000000;i=i+step){

	                    _dh = new HibernateUtil(clazz);
	                    _dh.setFirstResult(i);
	                    _dh.setMaxResults(step);

	                    //This line was previously like;
	                    //_dh.setQuery("from " + clazz.getName() + " order by 1,2");
	                    //This caused a problem when the database is Oracle because Oracle causes problems when the results are ordered
	                    //by an NCLOB field. In the case of containers table, the second field, CODE, is an NCLOB field. Because of this,
	                    //ordering is done only on the first field for the tables, which is INODE
	                    if(com.dotmarketing.beans.Tree.class.equals(clazz)){
	                    	_dh.setQuery("from " + clazz.getName() + " order by parent, child, relation_type");
	                    }
	                    else if(MultiTree.class.equals(clazz)){
	                    	_dh.setQuery("from " + clazz.getName() + " order by parent1, parent2, child, relation_type");
	                    }
	                    else if(TagInode.class.equals(clazz)){
	                    	_dh.setQuery("from " + clazz.getName() + " order by inode, tag_id");
	                    }
	                    else if(FileAssetVersionInfo.class.equals(clazz)){
	                    	_dh.setSQLQuery("SELECT {fileasset_version_info.*} from fileasset_version_info fileasset_version_info, identifier where identifier.id = fileasset_version_info.identifier order by fileasset_version_info.identifier ");
	                    }
	                    else if(TemplateVersionInfo.class.equals(clazz)){
	                    	_dh.setSQLQuery("SELECT {template_version_info.*} from template_version_info template_version_info, identifier where identifier.id = template_version_info.identifier order by template_version_info.identifier ");
	                    }
	                    else if(ContainerVersionInfo.class.equals(clazz)){
	                    	_dh.setSQLQuery("SELECT {container_version_info.*} from container_version_info container_version_info, identifier where identifier.id = container_version_info.identifier order by container_version_info.identifier ");
	                    }
	                    else if(HTMLPageVersionInfo.class.equals(clazz)){
	                    	_dh.setSQLQuery("SELECT {htmlpage_version_info.*} from htmlpage_version_info htmlpage_version_info, identifier where identifier.id = htmlpage_version_info.identifier order by htmlpage_version_info.identifier ");
	                    }
	                    else if(LinkVersionInfo.class.equals(clazz)){
	                    	_dh.setSQLQuery("SELECT {link_version_info.*} from link_version_info link_version_info, identifier where identifier.id = link_version_info.identifier order by link_version_info.identifier ");
	                    }
	                    else if(CalendarReminder.class.equals(clazz)){
	                    	_dh.setQuery("from " + clazz.getName() + " order by user_id, event_id, send_date");
	                    } else if(Identifier.class.equals(clazz)){
	                    	_dh.setQuery("from " + clazz.getName() + " order by parent_path");
	                    } else {
	                        _dh.setQuery("from " + clazz.getName() + " order by 1");

	                    }

	                    _list = _dh.list();
	                    if(_list.size() ==0){
	                        try {
	                        _bout.close();
	                        }
	                        catch( java.lang.NullPointerException npe){}
	                        _list = null;
	                        _dh = null;
	                        _bout = null;

	                        break;
	                    }

	                    if(_list != null && _list.size() > 0 && _list.get(0) instanceof Comparable){
	                    	java.util.Collections.sort(_list);
	                    }

	    				_writing = new File(backupTempFilePath + "/" + clazz.getName() + "_" + formatter.format(i) + ".xml");
	    				_bout = new BufferedOutputStream(new FileOutputStream(_writing));

	    				total = total + _list.size();

	    				try {
	                        Thread.sleep(10);
	                    } catch (InterruptedException e) {
	                    	Logger.warn(this, "An error ocurred trying to create XML files");
	                        Logger.error(this,e.getMessage(),e);
	                    }

	    				_xstream.toXML(_list, _bout);

	    				_bout.close();
	    				_list = null;
	    				_dh = null;
	    				_bout = null;

					}
					Logger.info(this, "writing : " + total + " records for " + clazz.getName());
				}

				/* Run Liferay's Tables */
				/* Companies */
				_list = PublicCompanyFactory.getCompanies();
				List<Company> companies = new ArrayList<Company>(_list);
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/" + Company.class.getName() + ".xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* Users */
				_list = APILocator.getUserAPI().findAllUsers();
				_list.add(APILocator.getUserAPI().getDefaultUser());
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/" + User.class.getName() + ".xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* Roles */
	//			_list = RoleManagerUtil.findAll();
	//			_xstream = new XStream(new DomDriver());
	//			_writing = new File(backupTempFilePath + "/" + Role.class.getName() + ".xml");
	//			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
	//			_xstream.toXML(_list, _bout);
	//			_bout.close();
	//			_list = null;
	//			_bout = null;

				/* Groups */
	//			_list = new ArrayList<Group>();
	//			for (Company company : companies) {
	//				_list.addAll(CompanyLocalManagerUtil.getGroups(CompanyUtils.getDefaultCompany().getCompanyId()));
	//			}
	//			List<Group> groups = new ArrayList<Group>(_list);
	//			_xstream = new XStream(new DomDriver());
	//			_writing = new File(backupTempFilePath + "/" + Group.class.getName() + ".xml");
	//			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
	//			_xstream.toXML(_list, _bout);
	//			_bout.close();
	//			_list = null;
	//			_bout = null;

				/* Layouts */
	//			_list = LayoutManagerUtil.findAll();
	//			_xstream = new XStream(new DomDriver());
	//			_writing = new File(backupTempFilePath + "/" + Layout.class.getName() + ".xml");
	//			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
	//			_xstream.toXML(_list, _bout);
	//			_bout.close();
	//			_list = null;
	//			_bout = null;

				/* users_roles */
				DotConnect dc = new DotConnect();
	//			dc.setSQL("select * from users_roles");
	//			_list = dc.getResults();
	//			_xstream = new XStream(new DomDriver());
	//			_writing = new File(backupTempFilePath + "/Users_Roles.xml");
	//			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
	//			_xstream.toXML(_list, _bout);
	//			_bout.close();
	//			_list = null;
	//			_bout = null;
	//
	//			/* users_groups */
	//			dc.setSQL("select * from users_groups");
	//			_list = dc.getResults();
	//			_xstream = new XStream(new DomDriver());
	//			_writing = new File(backupTempFilePath + "/Users_Groups.xml");
	//			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
	//			_xstream.toXML(_list, _bout);
	//			_bout.close();
	//			_list = null;
	//			_bout = null;
	//
	//			/* users_groups */
	//			dc.setSQL("select * from groups_roles");
	//			_list = dc.getResults();
	//			_xstream = new XStream(new DomDriver());
	//			_writing = new File(backupTempFilePath + "/Groups_Roles.xml");
	//			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
	//			_xstream.toXML(_list, _bout);
	//			_bout.close();
	//			_list = null;
	//			_bout = null;

				/* counter */
				dc.setSQL("select * from counter");
				_list = dc.getResults();
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Counter.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* counter */
				dc.setSQL("select * from address");
				_list = dc.getResults();
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Address.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* pollschoice */
				dc.setSQL("select * from pollschoice");
				_list = dc.getResults();
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Pollschoice.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* pollsdisplay */
				dc.setSQL("select * from pollsdisplay");
				_list = dc.getResults();
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Pollsdisplay.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* pollsquestion */
				dc.setSQL("select * from pollsquestion");
				_list = dc.getResults();
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Pollsquestion.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* pollsvote */
				dc.setSQL("select * from pollsvote");
				_list = dc.getResults();
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Pollsvote.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* image */
				_list = ImageLocalManagerUtil.getImages();

				/*
				 * The changes in this part were made for Oracle databases. Oracle has problems when
				 * getString() method is called on a LONG field on an Oracle database. Because of this,
				 * the object is loaded from liferay and DotConnect is not used
				 * http://jira.dotmarketing.net/browse/DOTCMS-1911
				 */

				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Image.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* portlet */

				/*
				 * The changes in this part were made for Oracle databases. Oracle has problems when
				 * getString() method is called on a LONG field on an Oracle database. Because of this,
				 * the object is loaded from liferay and DotConnect is not used
				 * http://jira.dotmarketing.net/browse/DOTCMS-1911
				 */
				dc.setSQL("select * from portlet");
				_list = dc.getResults();
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Portlet.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;

				/* portlet_preferences */

				try{
					_list = PortletPreferencesLocalManagerUtil.getPreferences();
				}catch(Exception e){
					Logger.error(this,"Error in retrieveing all portlet preferences");
				}
				_xstream = new XStream(new DomDriver());
				_writing = new File(backupTempFilePath + "/Portletpreferences.xml");
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_xstream.toXML(_list, _bout);
				_bout.close();
				_list = null;
				_bout = null;
				
				
				
				//backup workflow
				File file = new File(backupTempFilePath + "/WorkflowSchemeImportExportObject.json");
				WorkflowImportExportUtil.getInstance().exportWorkflows(file);
				
				
				/* log_mapper */
				List<LogMapperRow> logs;
                try {
                    logs = LogMapper.getInstance().getLogList();
                    _xstream = new XStream(new DomDriver());
                    _writing = new File(backupTempFilePath + "/LogsMappers.xml");
                    _bout = new BufferedOutputStream(new FileOutputStream(_writing));
                    _xstream.toXML(logs, _bout);
                    _bout.close();
                } catch (DotCacheException e) {
                    Logger.error(this, e.getMessage(), e);
                }
				
			} catch (HibernateException e) {
				Logger.error(this,e.getMessage(),e);
			} catch (SystemException e) {
				Logger.error(this,e.getMessage(),e);
			}

		}

		/**
		 * Will zip up all files in the tmp directory and send the result to the
		 * given OutputStream
		 *
		 * @param out
		 *            OutputStream to write the zip files to
		 * @throws IOException
		 * @author Will
		 */
		public void zipTempDirectoryToStream(OutputStream out) throws IOException {
			byte b[] = new byte[512];
			ZipOutputStream zout = new ZipOutputStream(out);
			ZipUtil.zipDirectory(backupTempFilePath, zout);
	//		File f = new File(backupTempFilePath);
	//		String[] s = f.list();
	//		for (int i = 0; i < s.length; i++) {
	//			if(s[i].equals(".svn")){
	//				continue;
	//			}
	//			f = new File(backupTempFilePath + "/" + s[i]);
	//			InputStream in;
	//			if(f.isDirectory()){
	//				in = new BufferedInputStream(new ByteArrayInputStream(f.));
	//			}else{
	//				in = new BufferedInputStream(new FileInputStream(f));
	//			}
	//			ZipEntry e = new ZipEntry(s[i].replace(File.separatorChar, '/'));
	//			zout.putNextEntry(e);
	//			int len = 0;
	//			while ((len = in.read(b)) != -1) {
	//				zout.write(b, 0, len);
	//			}
	//			zout.closeEntry();
	//			in.close();
	//		}
			zout.close();
			out.close();
	}
	    
	    private User getUser(HttpServletRequest req) {

	        // get the user
	        User user = null;
	        try {
	            user = com.liferay.portal.util.PortalUtil.getUser(req);
	        } catch (Exception e) {
	            Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
	            throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
	        }
	        return user;

	    }
}
