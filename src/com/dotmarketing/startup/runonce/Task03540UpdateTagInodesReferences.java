package com.dotmarketing.startup.runonce;

import com.dotcms.repackage.org.apache.commons.collections.map.MultiKeyMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class updates the contentlets tag fields references
 * in the TagInode table.
 *
 * @author Oswaldo Gallango & Oscar Arrieta
 * @since 02/12/2016
 */
public class Task03540UpdateTagInodesReferences extends AbstractJDBCStartupTask {

	private static final String GET_STRUCTURES_WITH_TAGS_FIELDS_POSTGRES="SELECT DISTINCT ON (structure_inode) structure_inode, velocity_var_name, field_contentlet FROM field WHERE field_type=?";
	private static final String GET_STRUCTURES_WITH_TAGS_FIELDS="SELECT structure_inode, velocity_var_name, field_contentlet FROM field WHERE field_type=?";
	private static final String GET_CONTENT_HOST_ID="SELECT host_inode FROM identifier WHERE id=?";
	private static final String DELETE_OLD_CONTENT_TAG_INODES="DELETE FROM tag_inode WHERE inode=?";

	Map<String, String> allHostIds = new HashMap<>();
	MultiKeyMap tagsAndHostMap = new MultiKeyMap();

	/**
	 * Update/fix the contentlets tags references in the tag_inode table 
	 */
	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException  {
		if (DbConnectionFactory.isPostgres()){
			executeUpgradeForPostgres();
		} else {
			executeUpgradeOthers();
		}
	}

	private void executeUpgradeForPostgres() throws DotDataException, DotRuntimeException{
		Logger.info(this, "Starting Task03540UpdateTagInodesReferences depending on your dataset it could take several minutes.");

		DotConnect dc = new DotConnect();

		dc.setSQL("TRUNCATE TABLE tag_inode");
		dc.loadResult();

		dc.setSQL(GET_STRUCTURES_WITH_TAGS_FIELDS_POSTGRES);
		dc.addParam(Field.FieldType.TAG.toString());
		List<Map<String, Object>> results = (List<Map<String, Object>>) dc.loadResults();

		for(Map<String, Object> result: results){

			HibernateUtil.startTransaction();

			final String structureInode = (String)result.get("structure_inode");
			final String field_varname = (String)result.get("velocity_var_name");
			final String field_contentlet = (String)result.get("field_contentlet");

			Logger.info(Task03540UpdateTagInodesReferences.class,
				"START the cleaning the structure_inode: " + structureInode + " field_contentlet: " + field_contentlet);

			String queryCleanFileds = "UPDATE contentlet "
				+ "SET "+field_contentlet+" = regexp_replace("+field_contentlet+", E'[,;\\n\\t\\r]+', ',', 'g') "
				+ "WHERE structure_inode = ?";

			dc.setSQL(queryCleanFileds);
			dc.addParam(structureInode);
			dc.loadResult();
			Logger.info(Task03540UpdateTagInodesReferences.class,
				"END the cleaning the structure_inode: " + structureInode + " field_contentlet: " + field_contentlet);

			Logger.info(Task03540UpdateTagInodesReferences.class, "Starting the INSERT into structure_inode: " + structureInode);

			String bigQuery = "INSERT into tag_inode (tag_id, inode, field_var_name, mod_date) (\n"
				+ "  SELECT DISTINCT ON (tag_id, inode, field_var_name)\n"
				+ "    tag.tag_id    AS tag_id,\n"
				+ "    content_info.inode AS inode,\n"
				+ "    ?                  AS field_var_name,\n"
				+ "    now()              AS mod_date\n"
				+ "  FROM (\n"
				+ "         SELECT\n"
				+ "           content_row.inode,\n"
				+ "           content_row.host_inode                                                   AS host_inode,\n"
				+ "           trim(unnest(string_to_array(CAST(content_row."+ field_contentlet +" AS TEXT), ','))) AS tag_name\n"
				+ "         FROM (\n"
				+ "                SELECT\n"
				+ "                  c.inode,\n"
				+ "                  c."+ field_contentlet +",\n"
				+ "                  i.host_inode\n"
				+ "                FROM contentlet c\n"
				+ "                  JOIN identifier i\n"
				+ "                    ON c.identifier = i.id\n"
				+ "                       AND c.structure_inode = ?\n"
				+ "                       AND (c."+ field_contentlet +" <> '') IS TRUE\n"
				+ "              ) AS content_row\n"
				+ "       ) AS content_info\n"
				+ "    JOIN tag\n"
				+ "      ON (lower(content_info.tag_name) = lower(tag.tagname)\n"
				+ "          AND tag.host_id = (SELECT tag.host_id\n"
				+ "                                  FROM tag\n"
				+ "                                  WHERE lower(tag.tagname) = lower(content_info.tag_name)\n"
				+ "                                  ORDER BY\n"
				+ "                                    CASE\n"
				+ "                                    WHEN tag.host_id = content_info.host_inode\n"
				+ "                                      THEN 1\n"
				+ "                                    WHEN tag.host_id = 'SYSTEM_HOST'\n"
				+ "                                      THEN 2\n"
				+ "                                    END\n"
				+ "                                  LIMIT 1)))";

			dc.setSQL(bigQuery);
			dc.addParam(field_varname);
			dc.addParam(structureInode);
			dc.loadResult();

			Logger.info(Task03540UpdateTagInodesReferences.class, "END of the INSERT into structure_inode: " + structureInode);

			HibernateUtil.commitTransaction();
		}

		Logger.info(this, "Finishing Task03540UpdateTagInodesReferences");
	}

	private void executeUpgradeOthers() throws DotDataException, DotRuntimeException{
		Logger.info(this, "Starting Task03540UpdateTagInodesReferences depending on your dataset it could take several minutes.");

		DotConnect dc = new DotConnect();
		dc.setSQL(GET_STRUCTURES_WITH_TAGS_FIELDS);
		dc.addParam(Field.FieldType.TAG.toString());
		List<Map<String, Object>> results = (List<Map<String, Object>>) dc.loadResults();

		for(Map<String, Object> result: results){

			final String structureInode = (String)result.get("structure_inode");
			final String field_varname = (String)result.get("velocity_var_name");
			final String field_contentlet = (String)result.get("field_contentlet");

			//We are going to retrieve only set of 25 rows. Avoiding hundreds or thousands of results at once.
			final int selectMaxRows = 25;
			int selectStartRow = 0;

			//Get contents with that field set.
			dc = new DotConnect();
			dc.setSQL("SELECT inode, identifier, " + field_contentlet +
				" FROM contentlet" +
				" WHERE structure_inode=?" +
				" AND " + field_contentlet + " IS NOT NULL");
			dc.addParam(structureInode);
			dc.setStartRow(selectStartRow);
			dc.setMaxRows(selectMaxRows);
			List<Map<String, Object>> contentResults = (List<Map<String, Object>>) dc.loadResults();

			while (contentResults != null && !contentResults.isEmpty()) {

				for(Map<String, Object> content : contentResults){
					String content_inode = (String)content.get("inode");
					String content_identifier = (String)content.get("identifier");
					String tags = (String)content.get(field_contentlet);

					if(UtilMethods.isSet(tags)){
						//Get content HostId.
						dc = new DotConnect();
						dc.setSQL(GET_CONTENT_HOST_ID);
						dc.addObject(content_identifier);
						List<Map<String,Object>> identifier = (List<Map<String, Object>>) dc.loadResults();
						String hostId = (String)identifier.get(0).get("host_inode");

						try{
							HibernateUtil.startTransaction();

							//Delete old contents tag inodes.
							dc = new DotConnect();
							dc.setSQL(DELETE_OLD_CONTENT_TAG_INODES);
							dc.addObject(content_inode);
							dc.loadResult();

							//Get/Create the tags
							List<String> tagNameList = getTagsInColumn(tags);
							Map<String, String> tagIdList = new HashMap<>();
							for ( String tagName : tagNameList ) {
								String tagId;

								if(tagsAndHostMap.containsKey(tagName, hostId)){
									tagId = tagsAndHostMap.get(tagName, hostId).toString();
								} else {
									tagId = findTagIdByName(tagName, hostId);
									tagsAndHostMap.put(tagName, hostId, tagId);
								}

								if (UtilMethods.isSet(tagId)) {
									tagIdList.put(tagId, tagId);
								} else {
									Logger.error(Task03540UpdateTagInodesReferences.class,
										"Tag Name not found in Tag Table, ignore it: " + tagName +
											" Contentlet inode: " + content_inode);
								}
							}

							for ( String tagId : tagIdList.keySet() ) {
								Logger.info(this, "Adding Contentlet Tag ID: " + tagId + " to Content Inode: " + content_inode);

								//Relate the found/created tag with this contentlet
								dc = new DotConnect();
								dc.setSQL("INSERT INTO tag_inode (tag_id, inode, field_var_name, mod_date) VALUES (?,?,?,?)");
								dc.addParam(tagId);
								dc.addParam(content_inode);
								dc.addParam(field_varname);
								dc.addParam(new Date());

								dc.loadResult();
							}

						} catch(DotSecurityException e){
							HibernateUtil.rollbackTransaction();
						}
					}
				}

				Logger.info(this, "Fetching more tags from the DB, please wait...");

				//Increase start row now that we completed the work with the latest resultset.
				selectStartRow += selectMaxRows;

				dc = new DotConnect();
				dc.setSQL("SELECT inode, identifier, " + field_contentlet +
					" FROM contentlet" +
					" WHERE structure_inode=?" +
					" AND " + field_contentlet + " IS NOT NULL");
				dc.addParam(structureInode);
				dc.setStartRow(selectStartRow);
				dc.setMaxRows(selectMaxRows);
				contentResults = (List<Map<String, Object>>) dc.loadResults();
			}
		}

		Logger.info(this, "Finishing Task03540UpdateTagInodesReferences");
	}

	private List<String> getTagsInColumn ( String text ) throws DotSecurityException, DotDataException {

		List<String> tagIds = new ArrayList<>();

		//Split the given list of tasks.
		String[] tagNames = text.split("[,\\n\\t\\r]");
		for (String tagname : tagNames) {
			tagname = tagname.trim();
			if (tagname.length() > 0) {
				tagIds.add(tagname);
			}
		}

		return tagIds;
	}

	/**
	 * When we find multiple possible tags with the same tag name we need to choose what tag to use.
	 * First to choose are Persona tags, then the tag with the given host id and finally the tag living in the
	 * system host
	 *
	 * @param tagName
	 * @param tagHostId
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private String findTagIdByName(String tagName, String tagHostId) throws DotDataException, DotSecurityException {

		String existingTagId;

		DotConnect dc = new DotConnect();
		dc.setSQL("SELECT tag_id, host_id, persona FROM tag WHERE tagname = ?");
		dc.addParam(tagName.toLowerCase());
		List<Map<String, Object>> sqlResults = dc.loadObjectResults();

		String personaTagId = "";
		String sameHostTagId = "";
		String systemHostTagId = "";

		for (Map<String, Object> sqlResult : sqlResults) {
			String tagIdRow = UtilMethods.isSet(sqlResult.get("tag_id")) ? sqlResult.get("tag_id").toString() : "";
			String hostIdRow = UtilMethods.isSet(sqlResult.get("host_id")) ? sqlResult.get("host_id").toString() : "";
			boolean isPersonaRow = UtilMethods.isSet(sqlResult.get("persona")) && DbConnectionFactory.isDBTrue(sqlResult.get("persona").toString());

			if ( isPersonaRow ) {
				personaTagId = tagIdRow;
			} else {
				if ( hostIdRow.equals(tagHostId) ){
					sameHostTagId = tagIdRow;
				} else {
					if ( hostIdRow.equals(Host.SYSTEM_HOST) ){
						systemHostTagId = tagIdRow;
					}
				}
			}

		}

		if (UtilMethods.isSet(personaTagId)) {
			existingTagId = personaTagId;
		} else {
			if (UtilMethods.isSet(sameHostTagId)) {
				existingTagId = sameHostTagId;
			} else {
				existingTagId = systemHostTagId;
			}
		}

		return existingTagId;
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
