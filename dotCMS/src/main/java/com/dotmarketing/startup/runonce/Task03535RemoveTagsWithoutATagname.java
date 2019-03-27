package com.dotmarketing.startup.runonce;

import java.util.HashMap;
import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * Remove from db all the Tags and TagInodes where the tag.tagname is null or empty
 * and add a not null constraint to the tagname column
 * @author Oswaldo Gallango
 * @since 2/11/2016
 */
public class Task03535RemoveTagsWithoutATagname extends AbstractJDBCStartupTask {

	private static String removeInvalidIagInodes="DELETE FROM tag_inode WHERE tag_id IN (SELECT tag_id FROM tag WHERE tagname IS NULL OR tagname='');";
	private static String removeInvalidIags="DELETE FROM tag WHERE tagname IS NULL OR tagname='';";

	private static String addTagnameConstraintPOSTGRES="ALTER TABLE tag ALTER COLUMN tagname SET NOT NULL;";
	private static String addTagnameConstraintMYSQL="ALTER TABLE tag MODIFY tagname VARCHAR(255) NOT NULL;";
	private static String addTagnameConstraintH2 ="ALTER TABLE tag ALTER tagname SET NOT NULL;";
	private static String addTagnameConstraintMSSQL="ALTER TABLE tag DROP CONSTRAINT tag_tagname_host;ALTER TABLE tag ALTER COLUMN tagname NVARCHAR(255) NOT NULL;ALTER TABLE tag ADD CONSTRAINT tag_tagname_host UNIQUE(tagname, host_id);";
	private static String addTagnameConstraintORACLE="ALTER TABLE tag MODIFY (tagname NOT NULL);";
	
	@Override
	public boolean forceRun () {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return removeInvalidIagInodes+removeInvalidIags+addTagnameConstraintPOSTGRES;
	}
	
	@Override
	public String getMySQLScript() {
		return removeInvalidIagInodes+removeInvalidIags+addTagnameConstraintMYSQL;
	}
	
	@Override
	public String getOracleScript() {
		return removeInvalidIagInodes+removeInvalidIags+addTagnameConstraintORACLE;
	}
	
	@Override
	public String getMSSQLScript() {
		return removeInvalidIagInodes+removeInvalidIags+addTagnameConstraintMSSQL;
	}
	
	@Override
	public String getH2Script() {
		return removeInvalidIagInodes+removeInvalidIags+addTagnameConstraintH2;
	}
	
	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}
