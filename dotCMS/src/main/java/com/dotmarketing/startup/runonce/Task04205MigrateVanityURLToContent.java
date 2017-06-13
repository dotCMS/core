package com.dotmarketing.startup.runonce;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

import static com.dotcms.util.CollectionsUtils.*;

/**
 * This upgrade task will migrate all the Virtual Links that exits on the DB (virtual_link table), if there is one virtual link with a 
 * site that no longer exists it will be deleted. The old virtual_link fields are migrated like this:
 * Title -> Title
 * Url -> This is splited, one is the Site and the other one the Uri
 * Uri -> Forward To
 * 
 * @author erickgonzalez
 * @version 4.2.0
 * @since Jun 8, 2017
 *
 */
public class Task04205MigrateVanityURLToContent extends AbstractJDBCStartupTask{
	
	private final String SELECT_VIRTUAL_LINK_QUERY = "select inode, title, url, uri from virtual_link";
	private final String UPDATE_INODE_QUERY = "update inode set type = 'contentlet' where type = 'virtual_link' ";
	private final String SELECT_HOSTID_BY_NAME_QUERY = "select identifier from contentlet where text1 = ?";
	private final String SELECT_HOSTLANG_BY_ID_QUERY = "select language_id from contentlet where identifier = ?";
	private final String INSERT_IDENTIFIER_QUERY = "insert into identifier(id,parent_path,asset_name,host_inode,asset_type)values (?,?,?,?,?)";
	private final String INSERT_CONTENTLET_QUERY = "insert into contentlet(inode, show_on_menu, sort_order, title, mod_date, mod_user, friendly_name, structure_inode, identifier, language_id,"
			+ " text1, text2, text3, text4, text5, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14,"
			+ "integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, float1, float2, float3, float4, float5, float6, float7, float8, float9, float10, float11, float12,"
			+ "float13, float14, float15, float16, float17, float18, float19, float20, float21, float22, float23, float24, float25, bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13,"
			+ "bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25) "
			+ "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"; 
	private final String INSERT_CONTENTLET_VERSION_INFO_QUERY = "insert into contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_on, version_ts)"
			+ "values (?,?,?,?,?,?,?)";
	private final String DELETE_INODE_BY_INODE_QUERY = "delete from inode where inode = ?";
	private final String DELETE_VIRTUAL_LINK_QUERY = "delete from virtual_link";
	
	
	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		
		DotConnect dc = new DotConnect();
		
		//Get all the virtual_links
		dc.setSQL(SELECT_VIRTUAL_LINK_QUERY);
		List<Map<String,String>> virtualLinks = dc.loadResults();
		
		//Update Inode Table, since previously the type was 'virtual_link' and now needs to be 'contentlet'
		dc.setSQL(UPDATE_INODE_QUERY);
		dc.loadResult();
		Logger.info(getClass(), "Type in the Inode Table has been changed to contentlet");
		
		// Deletes all rows in the Virtual Link Table
		dc.setSQL(DELETE_VIRTUAL_LINK_QUERY);
		dc.loadResult();
		
		for (Map<String, String> virtualLink : virtualLinks) {

			String oldURL = virtualLink.get("url").toString();
			String inode = virtualLink.get("inode").toString();
			String title = virtualLink.get("title").toString();
			String oldUri = virtualLink.get("uri").toString();

			// Calls the method that splits the URL into the site and the uri
			Map<String, String> siteAndUriMap = splitURL(oldURL);

			String site = siteAndUriMap.get("siteId");
			String uri = siteAndUriMap.get("uri");

			//If the site returns null means that there is no host
			if (site == null) {
				Logger.warn(this,
						"Vanity URL cannot be migrated to Content because the Host does not exist. You will need to create it manually, Title: "
								+ title + ", URL: " + oldURL + ", URI: " + oldUri);
				dc.setSQL(DELETE_INODE_BY_INODE_QUERY);
				dc.addParam(inode);
				dc.loadResult();
			} else {

				String uuid = UUIDGenerator.generateUuid();
				
				int lang = Integer.parseInt(findHostLang(site)); //Gets the lang of the Site, so when saving the Vanity URL it's the same lang

				// Insert on Identifier Table
				dc.setSQL(INSERT_IDENTIFIER_QUERY);
				dc.addParam(uuid); // identifier
				dc.addParam("/"); // parent_path
				dc.addParam("content." + inode); // asset_name
				dc.addParam(Host.SYSTEM_HOST); // host_inode
				dc.addParam("contentlet"); // asset_type
				dc.loadResult();

				// Insert on Contentlet Table
				dc.setSQL(INSERT_CONTENTLET_QUERY);
				dc.addParam(inode); // inode
				dc.addParam(false); // show_on_menu
				dc.addParam(0); // sort_order
				dc.addParam(title); // title
				dc.addParam(new Date()); // mod_date
				dc.addParam("dotcms.org.1"); // mod_user
				dc.addParam(title); // friendly_name
				dc.addParam("8e850645-bb92-4fda-a765-e67063a59be0"); // structure_inode
				dc.addParam(uuid); // identifier
				dc.addParam(lang); // language_id
				dc.addParam(title); // text1(title)
				dc.addParam(site); // text2(site)
				dc.addParam(uri); // text3(uri)
				dc.addParam(oldUri); // text4(forwardTo)
				dc.addParam("redirect"); // text5(action)
				dc.addParam(301); // integer1(responseCode)
				dc.addParam(0); // integer2(sort)
				for (int i = 0; i < 48; i++) {
					dc.addParam(0); // all other integer and float fields
				}
				for (int i = 0; i < 25; i++) {
					dc.addParam(false); // all bool fields
				}

				dc.loadResult();

				// Insert on Contentlet_Version_Info
				dc.setSQL(INSERT_CONTENTLET_VERSION_INFO_QUERY);
				dc.addParam(uuid); // identifier
				dc.addParam(lang); // lang
				dc.addParam(inode); // live_inode
				dc.addParam(inode); // working_inode
				dc.addParam(false); // deleted
				dc.addParam(new Date()); // locked_on
				dc.addParam(new Date()); // version_ts
				dc.loadResult();
			}
		}

	}
	
	/**
	 * This method splits the old Url into the site and uri. 
	 * Also make the call for the {@link #findHostId(String)} that returns the host identifier
	 * 
	 * @param url. The value of the url column on the virtual_link table.
	 * @return a map with the host identifier and uri
	 * @throws DotDataException
	 */
	private Map<String,String> splitURL(String url) throws DotDataException{
		String hostId = Host.SYSTEM_HOST;
		
		int hostSplit = url.indexOf(":");
		if(hostSplit != -1){
			String hostName = url.substring(0, url.lastIndexOf(":"));
			url = url.substring(hostSplit+2);
			hostId = findHostId(hostName);
		}
		
		return map("siteId", hostId, "uri", url);
	}

	/**
	 * This method searches the site identifier, if it doesn't exists it returns null.
	 * 
	 * @param hostName
	 * @return the identifier of the site
	 * @throws DotDataException
	 */
	private String findHostId(String hostName) throws DotDataException {
		String hostId = null;
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_HOSTID_BY_NAME_QUERY);
		dc.addParam(hostName);
		List<Map<String,String>> result = dc.loadResults();
		
		if(!result.isEmpty()){
			hostId = result.get(0).get("identifier").toString();
		}
		
		return hostId;
	}
	
	/**
	 * This method searches the lang of the Site that the Vanity URL is referenced.
	 * 
	 * @param hostId
	 * @return the lang of the site
	 * @throws DotDataException
	 */
	private String findHostLang(String hostId) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_HOSTLANG_BY_ID_QUERY);
		dc.addParam(hostId);
		List<Map<String,String>> hostLang = dc.loadResults();
		
		return hostLang.get(0).get("language_id").toString();
	}

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMySQLScript() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOracleScript() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMSSQLScript() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getH2Script() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

}
