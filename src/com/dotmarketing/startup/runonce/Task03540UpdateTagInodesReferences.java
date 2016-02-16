package com.dotmarketing.startup.runonce;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.UtilMethods;

/**
 * This class updates the contentlets tag fields references
 * in the TagInode table
 * @author Oswaldo Gallango
 * @since 02/12/2016
 */
public class Task03540UpdateTagInodesReferences extends AbstractJDBCStartupTask {
	private static TagAPI tagAPI = APILocator.getTagAPI();

	private static String GET_STRUCTURES_WITH_TAGS_FIELDS="SELECT STRUCTURE_INODE, VELOCITY_VAR_NAME, FIELD_CONTENTLET FROM FIELD WHERE FIELD_TYPE=?";
	private static String GET_CONTENT_HOST_ID="SELECT HOST_INODE FROM IDENTIFIER WHERE ID=?";
	private static String DELETE_OLD_CONTENT_TAG_INODES="DELETE FROM TAG_INODE WHERE inode=?";

	/**
	 * Update/fix the contentlets tags references in the tag_inode table 
	 */
	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException  {
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_STRUCTURES_WITH_TAGS_FIELDS);
		dc.addParam(Field.FieldType.TAG.toString());
		List<Map<String, Object>> results = (List<Map<String, Object>>) dc.loadResults();
		for(Map<String, Object> result: results){
			String structureInode = (String)result.get("structure_inode");
			String field_varname = (String)result.get("velocity_var_name");
			String field_contentlet = (String)result.get("field_contentlet");

			//get contents with that field set
			dc.setSQL("SELECT INODE, IDENTIFIER, "+field_contentlet+" FROM CONTENTLET WHERE STRUCTURE_INODE=? AND "+field_contentlet+" IS NOT NULL");
			dc.addParam(structureInode);
			List<Map<String, Object>> contentResults = (List<Map<String, Object>>) dc.loadResults();
			for(Map<String, Object> content : contentResults){
				String content_inode=(String) content.get("inode");
				String content_identifier=(String) content.get("identifier");
				String tags=(String) content.get(field_contentlet);

				if(UtilMethods.isSet(tags)){
					//get content HostId
					dc.setSQL(GET_CONTENT_HOST_ID);
					dc.addObject(content_identifier);
					List<Map<String,Object>> identifier = (List<Map<String, Object>>) dc.loadResults();
					String hostId = (String) identifier.get(0).get("host_inode");
					
					try{
						HibernateUtil.startTransaction();

						//delete old contents tag inodes
						dc.setSQL(DELETE_OLD_CONTENT_TAG_INODES);
						dc.addObject(content_inode);
						dc.loadResult();

						//Get/Create the tags
						List<Tag> list = tagAPI.getTagsInText(tags, hostId);
						for ( Tag tag : list ) {
							//Relate the found/created tag with this contentlet
							tagAPI.addContentletTagInode(tag, content_inode, field_varname);
						}

						//clean contentlet tag field
						dc.setSQL("UPDATE CONTENTLET SET "+field_contentlet+"='' WHERE INODE=?");
						dc.addParam(content_inode);
						dc.loadResult();
						HibernateUtil.commitTransaction();
					}catch(DotSecurityException e){
						HibernateUtil.rollbackTransaction();
					}
				}

			}
		}
	}

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return null;
	}

	@Override
	public String getMySQLScript() {
		return null;
	}

	@Override
	public String getOracleScript() {
		return null;
	}

	@Override
	public String getMSSQLScript() {
		return null;
	}

	@Override
	public String getH2Script() {
		return null;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}
