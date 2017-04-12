package com.dotcms.contenttype.business;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.DotStatefulJob;

public class DeleteFieldJob extends DotStatefulJob {

	final String findSQL = "select count(*) as test from contentlet where %s is not null and structure_type=? order by inode limit 200 ";

	final String wipeSQL = "update contentlet set %s = null where inode in (select inode from contentlet where %s is not null and structure_type=? order by inode limit 200 )";

	public void run(JobExecutionContext jobContext) throws JobExecutionException {

		Field fieldToDelete = (Field) jobContext.get("field");

		try {
			int count = countContents(fieldToDelete);
			while (count > 0) {
				count = LocalTransaction.wrapReturn(() -> {
					wipeField(fieldToDelete);
					return countContents(fieldToDelete);
				});
			}
			APILocator.getDistributedJournalAPI().addStructureReindexEntries(fieldToDelete.contentTypeId());
		} catch (DotDataException e) {
			throw new JobExecutionException(e);
		} finally {
			DbConnectionFactory.closeConnection();
		}

	}

	int countContents(Field fieldToDelete) throws DotDataException {

		DotConnect dc = new DotConnect();
		dc.setSQL(String.format(findSQL, fieldToDelete.dbColumn()));
		dc.addParam(fieldToDelete.contentTypeId());
		return dc.getInt("test");
	}

	private void wipeField(Field fieldToDelete) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(String.format(findSQL, fieldToDelete.dbColumn(), fieldToDelete.dbColumn()));
		dc.addParam(fieldToDelete.contentTypeId());
		dc.loadResult();
	}

}
